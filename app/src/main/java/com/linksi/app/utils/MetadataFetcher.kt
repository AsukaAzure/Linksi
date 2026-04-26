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
                .timeout(8000)
                .userAgent("Mozilla/5.0 (compatible; Linksibot/1.0)")
                .get()

            val title = doc.select("meta[property=og:title]").attr("content")
                .ifBlank { doc.select("meta[name=twitter:title]").attr("content") }
                .ifBlank { doc.title() }

            val description = doc.select("meta[property=og:description]").attr("content")
                .ifBlank { doc.select("meta[name=description]").attr("content") }

            val previewImage = doc.select("meta[property=og:image]").attr("content")
                .ifBlank { doc.select("meta[name=twitter:image]").attr("content") }

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
