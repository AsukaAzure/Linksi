package com.linksi.app.utils

import com.linksi.app.BuildConfig
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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

    suspend fun fetch(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url.trim())
        val domain = extractDomain(normalizedUrl)

        val result = if (isSocialMediaDomain(domain)) {
            fetchFromScraperApi(normalizedUrl)
        } else {
            fetchLocally(normalizedUrl)
        }

        result ?: LinkMetadata(
            domain = domain,
            faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
        )
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
        concurrency: Int = 6,
        onItemComplete: ((url: String, metadata: LinkMetadata) -> Unit)? = null
    ): List<LinkMetadata> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(concurrency)

        urls.map { url ->
            async {
                semaphore.withPermit {
                    val metadata = fetch(url)
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
                .userAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Safari/537.36"
                )
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
                doc.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:image:src]").attr("content").takeIf { it.isNotBlank() },
                doc.select("img[src]").firstOrNull { img ->
                    val w = img.attr("width").toIntOrNull() ?: 0
                    val h = img.attr("height").toIntOrNull() ?: 0
                    val src = img.attr("src")
                    src.isNotBlank() && (w > 200 || h > 200 || (w == 0 && h == 0)) &&
                            !src.contains("logo", ignoreCase = true) &&
                            !src.contains("icon", ignoreCase = true) &&
                            !src.contains("avatar", ignoreCase = true)
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
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("www.") -> "https://$url"
        else -> "https://$url"
    }
}