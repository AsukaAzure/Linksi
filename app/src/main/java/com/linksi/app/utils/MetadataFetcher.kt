package com.linksi.app.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun fetch(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        try {
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

            // Title — try multiple sources
            val title = listOfNotNull(
                doc.title().takeIf { it.isNotBlank() },
                doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() },
                doc.select("h1").first()?.text()?.takeIf { it.isNotBlank() }
            ).firstOrNull()?.trim() ?: ""

            // Description
            val description = listOfNotNull(
                doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
            ).firstOrNull()?.trim() ?: ""

            // Preview image — make relative URLs absolute
            val rawImage = listOfNotNull(
                doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[property=og:image:url]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() },
                doc.select("meta[name=twitter:image:src]").attr("content").takeIf { it.isNotBlank() },
                // Fallback: first large image
                doc.select("img[src]").firstOrNull { img ->
                    val w = img.attr("width").toIntOrNull() ?: 0
                    val h = img.attr("height").toIntOrNull() ?: 0
                    val src = img.attr("src")
                    src.isNotBlank() && (w > 200 || h > 200 || (w == 0 && h == 0))
                            && !src.contains("logo", ignoreCase = true)
                            && !src.contains("icon", ignoreCase = true)
                            && !src.contains("avatar", ignoreCase = true)
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

            val domain = URI(url).host?.removePrefix("www.") ?: ""
            val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"

            LinkMetadata(
                title = title.take(200),
                description = description.take(500),
                faviconUrl = faviconUrl,
                previewImageUrl = previewImage,
                domain = domain
            )
        } catch (e: Exception) {
            Log.e("MetadataFetcher", "Failed to fetch metadata for $url", e)
            val domain = try { URI(url).host?.removePrefix("www.") ?: "" } catch (e: Exception) { "" }
            LinkMetadata(
                domain = domain,
                faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
            )
        }
    }
}

fun extractDomain(url: String): String {
    return try {
        URI(url).host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}

fun isValidUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        uri.scheme in listOf("http", "https") && uri.host != null
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
