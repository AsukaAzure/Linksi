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
import java.net.URLEncoder

data class LinkMetadata(
    val title: String = "",
    val description: String = "",
    val faviconUrl: String = "",
    val previewImageUrl: String = "",
    val domain: String = ""
)

object MetadataFetcher {

    private const val SCRAPER_API_BASE = "https://link-metadata-scraper.vercel.app/api/scrape"

    // Sites that reliably block/gate on-device scraping (login walls, bot
    // detection) go straight to the hosted API instead of wasting a Jsoup
    // round trip that's going to fail anyway. Add to this list as you find
    // more sites that behave this way.
    private val SOCIAL_MEDIA_DOMAINS = setOf(
        "instagram.com",
        "twitter.com",
        "x.com",
        "facebook.com",
        "fb.com",
        "tiktok.com",
        "threads.net",
        "linkedin.com",
        "pinterest.com",
        "snapchat.com",
        "reddit.com"
    )

    // Matches the title of common bot-challenge / login-wall interstitials
    // (Cloudflare "Just a moment...", login walls, etc.) so a blocked local
    // fetch doesn't get saved as if it were real page content.
    private val BLOCKED_TITLE_PATTERN = Regex(
        "^(just a moment|attention required|please wait|access denied|are you a human|login|log in|sign in)",
        RegexOption.IGNORE_CASE
    )

    suspend fun fetch(url: String, context: Context? = null): LinkMetadata = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url.trim())
        val domain = extractDomain(normalizedUrl)

        // Special handling for Reddit to get high-quality previews
        if (domain.contains("reddit.com") || domain.contains("redd.it")) {
            val redditMeta = fetchRedditMetadata(normalizedUrl)
            // ONLY return if we actually got an image. If no image, let it fall back
            // to the Scraper API or local JS scraping which might have better luck.
            if (redditMeta != null && redditMeta.previewImageUrl.isNotBlank()) {
                return@withContext redditMeta
            }
        }

        var result = if (isSocialMediaDomain(domain)) {
            fetchFromScraperApi(normalizedUrl)
        } else {
            fetchLocally(normalizedUrl)
        }

        // Fallback to WebView if local/API fetch failed or returned minimal data
        if (context != null && (result == null || result.title.isBlank())) {
            val webViewResult = fetchWithWebView(normalizedUrl, context)
            if (webViewResult != null) {
                result = webViewResult
            }
        }

        result ?: LinkMetadata(
            domain = domain,
            faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
        )
    }

    /**
     * Reddit-specific fetcher that hits the public .json endpoint.
     * This is much more reliable for Reddit than scraping HTML.
     */
    private fun fetchRedditMetadata(url: String): LinkMetadata? {
        return try {
            // 1. Try Official oEmbed API first (Most reliable for titles/thumbnails)
            val oEmbedUrl = "https://www.reddit.com/oembed?url=${URLEncoder.encode(url, "UTF-8")}"
            val oEmbedResponse = Jsoup.connect(oEmbedUrl)
                .timeout(5000)
                .ignoreContentType(true)
                .userAgent("Twitterbot/1.0")
                .execute()
            
            val oEmbedJson = JSONObject(oEmbedResponse.body())
            val oEmbedTitle = oEmbedJson.optString("title")
            val oEmbedThumb = oEmbedJson.optString("thumbnail_url")

            // 2. Try JSON API for high-resolution images and extra stats
            val jsonUrl = if (url.contains("?")) {
                url.substringBefore("?") + ".json?raw_json=1"
            } else {
                url.removeSuffix("/") + ".json?raw_json=1"
            }

            val response = Jsoup.connect(jsonUrl)
                .timeout(10000)
                .ignoreContentType(true)
                .followRedirects(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .execute()
            
            val body = response.body()
            val postData = try {
                val jsonArray = org.json.JSONArray(body)
                jsonArray.getJSONObject(0)
                    .getJSONObject("data")
                    .getJSONArray("children")
                    .getJSONObject(0)
                    .getJSONObject("data")
            } catch (e: Exception) {
                val jsonObj = JSONObject(body)
                if (jsonObj.has("data")) {
                    jsonObj.getJSONObject("data")
                        .getJSONArray("children")
                        .getJSONObject(0)
                        .getJSONObject("data")
                } else null
            }

            if (postData != null) {
                val title = postData.optString("title").ifBlank { oEmbedTitle }
                val subreddit = postData.optString("subreddit_name_prefixed")
                val selfText = postData.optString("selftext").take(250)
                val ups = postData.optInt("ups", 0)
                
                var previewImage = ""
                // Try high-res preview first
                val preview = postData.optJSONObject("preview")
                val images = preview?.optJSONArray("images")
                if (images != null && images.length() > 0) {
                    previewImage = images.getJSONObject(0).getJSONObject("source").optString("url")
                }
                
                // Fallback to direct URL if it's an image
                if (previewImage.isBlank()) {
                    val destUrl = postData.optString("url_overridden_by_dest")
                    if (destUrl.contains("i.redd.it") || destUrl.contains(".jpg") || destUrl.contains(".png")) {
                        previewImage = destUrl
                    }
                }
                
                // Final fallbacks
                if (previewImage.isBlank()) previewImage = oEmbedThumb
                if (previewImage.isBlank()) {
                    val thumb = postData.optString("thumbnail")
                    if (thumb.startsWith("http")) previewImage = thumb
                }

                return LinkMetadata(
                    title = title,
                    description = if (subreddit.isNotBlank()) "$subreddit • $selfText" else selfText,
                    faviconUrl = "https://www.redditstatic.com/desktop2x/img/favicon/android-icon-192x192.png",
                    previewImageUrl = previewImage,
                    domain = "reddit.com"
                )
            }

            // If JSON failed but oEmbed worked
            if (oEmbedTitle.isNotBlank()) {
                return LinkMetadata(
                    title = oEmbedTitle,
                    faviconUrl = "https://www.redditstatic.com/desktop2x/img/favicon/android-icon-192x192.png",
                    previewImageUrl = oEmbedThumb,
                    domain = "reddit.com"
                )
            }
            null
        } catch (e: Exception) {
            Log.w("MetadataFetcher", "Reddit fetch failed", e)
            null
        }
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

                webView.evaluateJavascript(js) { json ->
                    try {
                        val cleanedJson = json.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
                        val obj = JSONObject(cleanedJson)
                        val domain = extractDomain(url ?: "")
                        
                        deferred.complete(LinkMetadata(
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            previewImageUrl = obj.optString("image"),
                            domain = domain,
                            faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
                        ))
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

        // Timeout after 10 seconds
        withTimeoutOrNull(10000) {
            deferred.await()
        } ?: run {
            webView.stopLoading()
            webView.destroy()
            null
        }
    }

    private fun isSocialMediaDomain(domain: String): Boolean {
        return SOCIAL_MEDIA_DOMAINS.any { known ->
            domain == known || domain.endsWith(".$known")
        }
    }

    /**
     * Fetch metadata for many URLs (e.g. a bulk bookmark import) without
     * overwhelming the network stack or the scraper's cold-start capacity.
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

    /**
     * Used for regular (non-social-media) sites. Returns null if the fetch
     * fails, or if what came back looks like a bot-challenge/login-wall page
     * rather than real content — the caller then shows a domain-only card.
     */
    private fun fetchLocally(url: String): LinkMetadata? {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("facebookexternalhit/1.1")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.facebook.com/")
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
     * Used for social media domains. Calls the hosted Vercel scraper.
     * Returns null if the request fails, or if the site is behind bot
     * protection / a login wall the API couldn't get past.
     */
    private fun fetchFromScraperApi(url: String): LinkMetadata? {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val endpoint = "$SCRAPER_API_BASE?url=$encoded"

            val response = Jsoup.connect(endpoint)
                .timeout(15000) // generous timeout to absorb Vercel cold starts
                .ignoreContentType(true) // response is JSON, not HTML
                .ignoreHttpErrors(true)
                .userAgent("Linksi-Android/1.0")
                .execute()

            if (response.statusCode() !in 200..299) {
                Log.w("MetadataFetcher", "Scraper API returned ${response.statusCode()} for $url")
                return null
            }

            val json = JSONObject(response.body())

            if (json.optBoolean("blocked", false)) {
                Log.w("MetadataFetcher", "Scraper API reports $url as blocked")
                return null
            }

            val remoteTitle = json.optString("title")
            if (BLOCKED_TITLE_PATTERN.containsMatchIn(remoteTitle)) {
                Log.w("MetadataFetcher", "Scraper API result for $url looks like a login/challenge page")
                return null
            }

            val domain = extractDomain(json.optString("canonicalUrl", url).ifBlank { url })

            LinkMetadata(
                title = remoteTitle.take(200),
                description = json.optString("description").take(500),
                faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64",
                previewImageUrl = json.optString("image"),
                domain = domain
            )
        } catch (e: Exception) {
            Log.w("MetadataFetcher", "Scraper API call failed for $url", e)
            null
        }
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

fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return ""

    var normalized = trimmed
    
    // Convert to https if it's http
    if (normalized.startsWith("http://")) {
        normalized = "https://" + normalized.substring(7)
    } else if (!normalized.startsWith("https://")) {
        normalized = "https://$normalized"
    }

    return try {
        val uri = java.net.URI(normalized).normalize()
        var result = uri.toString()
        
        // Remove trailing slash for root domains AND paths to be robust
        if (result.endsWith("/")) {
            result = result.substring(0, result.length - 1)
        }
        
        // Lowercase the entire URL for comparison consistency
        result.lowercase()
    } catch (e: Exception) {
        normalized.lowercase()
    }
}
