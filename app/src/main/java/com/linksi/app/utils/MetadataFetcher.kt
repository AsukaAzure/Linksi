package com.linksi.app.utils

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI

data class LinkMetadata(
    val title: String = "",
    val description: String = "",
    val faviconUrl: String = "",
    val previewImageUrl: String = "",
    val domain: String = ""
)

object MetadataFetcher {

    // Matches the title of common bot-challenge / login-wall interstitials
    // (Cloudflare "Just a moment...", login walls, etc.) so a blocked local
    // fetch doesn't get saved as if it were real page content.
    private val BLOCKED_TITLE_PATTERN = Regex(
        "^(just a moment|attention required|please wait|access denied|are you a human|login|log in|sign in)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Resolution order:
     *   1. A per-domain resolver from LinkResolvers.kt, if one matches. These
     *      call the platform's own public embed/oEmbed endpoint instead of
     *      scraping HTML that's behind a login wall.
     *   2. Generic Open Graph scrape for everything else.
     *   3. WebView, for JS-rendered pages that return an empty shell to Jsoup.
     *   4. Domain-only placeholder card.
     *
     * Domains in NO_PREVIEW_DOMAINS skip straight past step 2 — there's no
     * public surface to read, so a Jsoup round trip only buys a timeout.
     */
    suspend fun fetch(url: String, context: Context? = null): LinkMetadata = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url.trim())
        val domain = extractDomain(normalizedUrl)

        var result = resolverFor(domain)?.safeResolve(normalizedUrl)
            ?: if (isNoPreviewDomain(domain)) null else fetchLocally(normalizedUrl)

        // Fallback to WebView if the above failed or returned minimal data.
        if (context != null && (result == null || result.title.isBlank())) {
            val webViewResult = fetchWithWebView(normalizedUrl, context)
            if (webViewResult != null && webViewResult.title.isNotBlank()) {
                result = webViewResult
            }
        }

        result ?: LinkMetadata(
            domain = domain,
            faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
        )
    }.also {
        Log.d("MetadataFetcher", "final result for $url -> title=\"${it.title}\" " +
                "descLen=${it.description.length} image=\"${it.previewImageUrl}\" domain=${it.domain}")
    }

    private suspend fun fetchWithWebView(url: String, context: Context): LinkMetadata? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<LinkMetadata?>()

        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val js = """
                    (function() {
                        var meta = {};
                        var ogTitle = document.querySelector('meta[property="og:title"]');
                        var ogDesc = document.querySelector('meta[property="og:description"]');
                        var ogImg = document.querySelector('meta[property="og:image"]');
                        var title = document.title;

                        meta.title = (ogTitle ? ogTitle.content : '') || title || '';
                        meta.description = (ogDesc ? ogDesc.content : '') || document.querySelector('meta[name="description"]')?.content || '';
                        meta.image = (ogImg ? ogImg.content : '') || '';

                        return JSON.stringify(meta);
                    })()
                """.trimIndent()

                view?.evaluateJavascript(js) { json ->
                    try {
                        val cleanedJson = json.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
                        val obj = JSONObject(cleanedJson)
                        val pageDomain = extractDomain(url ?: "")
                        val rawTitle = obj.optString("title")

                        // Don't save a login wall or bot challenge as real content.
                        if (BLOCKED_TITLE_PATTERN.containsMatchIn(rawTitle)) {
                            Log.w("MetadataFetcher", "$url looks blocked/gated in WebView (title: \"$rawTitle\")")
                            deferred.complete(null)
                            return@evaluateJavascript
                        }

                        deferred.complete(
                            LinkMetadata(
                                title = rawTitle,
                                description = obj.optString("description"),
                                previewImageUrl = obj.optString("image"),
                                domain = pageDomain,
                                faviconUrl = "https://www.google.com/s2/favicons?domain=$pageDomain&sz=64"
                            )
                        )
                    } catch (e: Exception) {
                        deferred.complete(null)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                deferred.complete(null)
            }
        }

        webView.loadUrl(url)

        // Always tear the WebView down — the old code only destroyed it on the
        // timeout path, so every successful (and every errored) fetch leaked one.
        try {
            withTimeoutOrNull(10_000) { deferred.await() }
        } finally {
            webView.stopLoading()
            webView.destroy()
        }
    }

    /**
     * Generic Open Graph scrape. Returns null if the fetch fails, or if what
     * came back looks like a bot-challenge/login-wall page rather than real
     * content — the caller then tries WebView, then a domain-only card.
     */
    private fun fetchLocally(url: String): LinkMetadata? {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("facebookexternalhit/1.1")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get()

            val ogTitle = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
            val rawTitle = doc.title().trim()

            if (BLOCKED_TITLE_PATTERN.containsMatchIn(rawTitle) && ogTitle == null) {
                Log.w("MetadataFetcher", "$url looks blocked/gated locally (title: \"$rawTitle\")")
                return null
            }

            val title = listOfNotNull(
                ogTitle,
                doc.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() },
                rawTitle.takeIf { it.isNotBlank() },
                doc.select("h1").first()?.text()?.takeIf { it.isNotBlank() }
            ).firstOrNull()?.trim() ?: ""

            val description = listOfNotNull(
                doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
            ).firstOrNull()?.trim() ?: ""

            if (title.isBlank() && description.isBlank()) {
                Log.w("MetadataFetcher", "$url returned no usable metadata locally")
                return null
            }

            val rawImage = listOfNotNull(
                doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[property=og:image:url]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[property=og:image:secure_url]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:image:src]").attr("content").takeIf { it.isNotBlank() },
                doc.select("link[rel=image_src]").attr("href").takeIf { it.isNotBlank() },
                doc.select("img[src]").firstOrNull { img ->
                    val src = img.attr("abs:src")
                    val w = img.attr("width").toIntOrNull() ?: 0
                    val h = img.attr("height").toIntOrNull() ?: 0

                    // Prioritize images that look like content, not icons
                    src.isNotBlank() &&
                            !src.contains("favicon") &&
                            !src.contains("logo") &&
                            !src.contains("icon") &&
                            !src.contains("avatar") &&
                            (w == 0 || w > 100) && (h == 0 || h > 100)
                }?.attr("abs:src")?.takeIf { it.isNotBlank() }
            ).firstOrNull() ?: ""

            val previewImage = when {
                rawImage.startsWith("http://") || rawImage.startsWith("https://") -> rawImage
                rawImage.startsWith("//") -> "https:$rawImage"
                rawImage.startsWith("/") -> {
                    val uri = URI(url)
                    "${uri.scheme}://${uri.host}$rawImage"
                }
                else -> rawImage
            }

            val domain = extractDomain(url)

            LinkMetadata(
                title = title.take(200),
                description = description.take(500),
                faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64",
                previewImageUrl = previewImage,
                domain = domain
            )
        } catch (e: Exception) {
            Log.w("MetadataFetcher", "Local fetch failed for $url", e)
            null
        }
    }

    /**
     * Fetch metadata for many URLs (e.g. a bulk bookmark import) without
     * overwhelming the network stack or the resolvers' rate limits.
     *
     * - Caps concurrency instead of firing every request at once.
     * - One failing link never affects the others.
     * - `onItemComplete` lets you update the UI incrementally instead of
     *   blocking on the whole batch before showing anything.
     *
     * @param concurrency how many requests to run in parallel. 4-8 is a good
     *   starting point.
     */
    suspend fun fetchAll(
        urls: List<String>,
        context: Context? = null,
        concurrency: Int = 6,
        onItemComplete: ((url: String, metadata: LinkMetadata) -> Unit)? = null
    ): List<LinkMetadata> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(concurrency)

        urls.map { url ->
            async {
                semaphore.withPermit {
                    val metadata = fetch(url, context)
                    onItemComplete?.invoke(url, metadata)
                    metadata
                }
            }
        }.awaitAll()
    }
}

fun extractDomain(url: String): String {
    return try {
        URI(normalizeUrl(url.trim())).host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}

fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val uri = URI(normalizeUrl(url.trim()))
        val host = uri.host
        uri.scheme in listOf("http", "https") &&
                !host.isNullOrBlank() &&
                (host.contains(".") || host == "localhost")
    } catch (e: Exception) {
        false
    }
}

/**
 * Normalizes scheme and host only.
 *
 * The previous version lowercased the entire URL, which silently corrupted
 * every case-sensitive path segment — Instagram shortcodes (/p/DAbC_xYz/) and
 * YouTube video IDs both are — so those links 404'd before any fetch happened.
 * Host and scheme are case-insensitive by spec; paths and query strings are not.
 */
fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return ""

    var normalized = trimmed

    // Convert to https if it's http
    if (normalized.startsWith("http://", ignoreCase = true)) {
        normalized = "https://" + normalized.substring(7)
    } else if (!normalized.startsWith("https://", ignoreCase = true)) {
        normalized = "https://$normalized"
    }

    return try {
        val uri = URI(normalized).normalize()
        val host = uri.host?.lowercase() ?: return normalized

        val path = uri.rawPath.orEmpty().let {
            // Drop a trailing slash, but never reduce the path to nothing.
            if (it.length > 1 && it.endsWith("/")) it.dropLast(1) else it
        }

        buildString {
            append("https://")
            append(host)
            if (uri.port != -1) append(":${uri.port}")
            append(path)
            uri.rawQuery?.let { append("?$it") }
        }
    } catch (e: Exception) {
        normalized
    }
}