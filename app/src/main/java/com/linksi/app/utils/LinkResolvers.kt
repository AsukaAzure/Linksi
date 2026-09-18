package com.linksi.app.utils

import android.util.Log
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

/**
 * Per-domain metadata resolvers.
 *
 * The idea: stop treating "social media" as one category that needs one clever
 * scraper. Each platform has its own public embed surface, and those surfaces
 * are *designed* to be called by third parties. Ask the right endpoint and you
 * get clean JSON; scrape the HTML page and you get a login wall.
 *
 * Order of attempts in MetadataFetcher should be:
 *   1. matching resolver here
 *   2. generic OG scrape (fetchLocally)
 *   3. WebView
 *   4. domain-only placeholder card
 */

private const val MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

/** Identify yourself honestly. Several of these APIs rate-limit anonymous UAs harder. */
private const val APP_UA = "Linksi/1.0 (Android link saver; +https://github.com/AsukaAzure)"

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

private fun httpBody(url: String, ua: String = APP_UA, timeoutMs: Int = 8000): String? = try {
    val res = Jsoup.connect(url)
        .ignoreContentType(true)
        .ignoreHttpErrors(true)
        .followRedirects(true)
        .userAgent(ua)
        .header("Accept-Language", "en-US,en;q=0.9")
        .timeout(timeoutMs)
        .execute()
    if (res.statusCode() in 200..299) res.body() else {
        Log.w("LinkResolvers", "HTTP ${res.statusCode()} for $url")
        null
    }
} catch (e: Exception) {
    Log.w("LinkResolvers", "request failed: $url", e)
    null
}

private fun httpJson(url: String, ua: String = APP_UA, timeoutMs: Int = 8000): JSONObject? =
    httpBody(url, ua, timeoutMs)?.let {
        try { JSONObject(it) } catch (e: Exception) { null }
    }

private fun favicon(domain: String) =
    "https://www.google.com/s2/favicons?domain=$domain&sz=64"

/**
 * Follows redirects and returns the URL actually landed on, without
 * downloading the body. Needed before calling an oEmbed endpoint with a
 * share-shortener link (e.g. reddit.com/r/sub/s/AbCdEf) — oEmbed providers
 * pattern-match the URL shape and don't recognize shortener paths as "this
 * is a specific post", so passing the short link through unresolved gets you
 * a generic fallback instead of the actual content.
 */
private fun resolveRedirect(url: String, ua: String = MOBILE_UA, timeoutMs: Int = 8000): String = try {
    val res = Jsoup.connect(url)
        .method(org.jsoup.Connection.Method.GET)
        .ignoreContentType(true)
        .followRedirects(true)
        .userAgent(ua)
        .timeout(timeoutMs)
        .execute()
    res.url().toString()
} catch (e: Exception) {
    Log.w("LinkResolvers", "redirect resolution failed for $url, using as-is", e)
    url
}

interface LinkResolver {
    fun matches(domain: String): Boolean
    /** Returns null when this resolver can't help; caller falls through. */
    fun resolve(url: String): LinkMetadata?
}

/**
 * Runs a resolver's resolve() and turns any uncaught throw into null instead
 * of letting it escape. Each resolver already catches its own network/parse
 * exceptions, but this is the backstop: one platform's edge case should never
 * be able to take down fetchAll's sibling coroutines.
 */
fun LinkResolver.safeResolve(url: String): LinkMetadata? = try {
    resolve(url)
} catch (e: Exception) {
    Log.w("LinkResolvers", "${this::class.simpleName} threw on $url", e)
    null
}

/* ------------------------------------------------------------------ YouTube */

object YouTubeResolver : LinkResolver {
    override fun matches(domain: String) =
        domain == "youtube.com" || domain.endsWith(".youtube.com") || domain == "youtu.be"

    private fun videoId(url: String): String? {
        val uri = URI(url)
        return when {
            uri.host.orEmpty().contains("youtu.be") -> uri.path.trim('/').substringBefore('/')
            uri.path.startsWith("/shorts/") -> uri.path.removePrefix("/shorts/").substringBefore('/')
            uri.path.startsWith("/live/") -> uri.path.removePrefix("/live/").substringBefore('/')
            else -> uri.query
                ?.split("&")
                ?.firstOrNull { it.startsWith("v=") }
                ?.removePrefix("v=")
        }?.takeIf { it.isNotBlank() }
    }

    override fun resolve(url: String): LinkMetadata? {
        val json = httpJson("https://www.youtube.com/oembed?url=${enc(url)}&format=json")
            ?: return null
        val id = videoId(url)
        // oEmbed gives hqdefault; maxresdefault is far nicer on a card and exists
        // for almost every upload. Coil will fall back if the request 404s.
        val image = if (id != null) "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
        else json.optString("thumbnail_url")

        return LinkMetadata(
            title = json.optString("title"),
            description = json.optString("author_name"),
            faviconUrl = favicon("youtube.com"),
            previewImageUrl = image,
            domain = "youtube.com"
        )
    }
}

/* ------------------------------------------------------------------- TikTok */

object TikTokResolver : LinkResolver {
    override fun matches(domain: String) =
        domain == "tiktok.com" || domain.endsWith(".tiktok.com")

    override fun resolve(url: String): LinkMetadata? {
        // TikTok's oEmbed is public, unauthenticated and still returns a thumbnail.
        // Note: thumbnail URLs have no file extension — don't infer type from the path.
        val json = httpJson("https://www.tiktok.com/oembed?url=${enc(url)}") ?: return null
        val author = json.optString("author_name")
        return LinkMetadata(
            title = json.optString("title").ifBlank { "TikTok video" },
            description = if (author.isNotBlank()) "@$author" else "",
            faviconUrl = favicon("tiktok.com"),
            previewImageUrl = json.optString("thumbnail_url"),
            domain = "tiktok.com"
        )
    }
}

/* -------------------------------------------------------------------- X / Twitter */

object TwitterResolver : LinkResolver {
    override fun matches(domain: String) =
        domain == "x.com" || domain == "twitter.com" ||
                domain.endsWith(".x.com") || domain.endsWith(".twitter.com")

    override fun resolve(url: String): LinkMetadata? {
        // X serves no usable OG data to non-whitelisted clients. FxEmbed
        // (api.fxtwitter.com) is the community JSON proxy that Discord-fix bots use.
        // Third-party infrastructure — treat an outage as "no preview", not a crash.
        val path = try { URI(url).path } catch (e: Exception) { return null }
        val json = httpJson("https://api.fxtwitter.com$path") ?: return null
        if (json.optInt("code", 0) != 200) return null

        val tweet = json.optJSONObject("tweet") ?: return null
        val author = tweet.optJSONObject("author")
        val handle = author?.optString("screen_name").orEmpty()
        val name = author?.optString("name").orEmpty()

        val media = tweet.optJSONObject("media")
        val image = media?.optJSONArray("photos")?.optJSONObject(0)?.optString("url")
            ?: media?.optJSONArray("videos")?.optJSONObject(0)?.optString("thumbnail_url")
            ?: author?.optString("avatar_url")

        return LinkMetadata(
            title = if (name.isNotBlank()) "$name (@$handle)" else "Post on X",
            description = tweet.optString("text").take(500),
            faviconUrl = favicon("x.com"),
            previewImageUrl = image.orEmpty(),
            domain = "x.com"
        )
    }
}

/* ---------------------------------------------------------------- Instagram */

object InstagramResolver : LinkResolver {
    override fun matches(domain: String) =
        domain == "instagram.com" || domain.endsWith(".instagram.com")

    private fun shortcode(url: String): String? {
        val path = try { URI(url).path } catch (e: Exception) { return null }
        val parts = path.trim('/').split('/')
        val i = parts.indexOfFirst { it == "p" || it == "reel" || it == "reels" || it == "tv" }
        return if (i >= 0 && i + 1 < parts.size) parts[i + 1] else null
    }

    private const val TAG = "LinkResolvers.IG"

    // The oEmbed HTML always contains this exact anchor text, whether or not
    // Meta actually had anything to say about the post — it's boilerplate,
    // not content. Treating it as a real title suppresses every fallback
    // that could have gotten the real thing (fetchLocally, WebView), because
    // the caller sees a non-blank title and assumes success.
    private val GENERIC_TITLES = setOf(
        "view this post on instagram",
        "instagram",
        "instagram post"
    )
    private fun isGeneric(s: String) = s.trim().lowercase() in GENERIC_TITLES

    // Meta serves two different responses to the same embed URL depending on
    // User-Agent: a lightweight, OG-tag-bearing page to allowlisted crawler
    // UAs (this is why WhatsApp/Telegram/Discord show clean IG previews with
    // no login), and a JS-only app shell with no metadata at all to anything
    // that looks like an ordinary mobile browser. Ask for the crawler version.
    private const val CRAWLER_UA = "facebookexternalhit/1.1"

    override fun resolve(url: String): LinkMetadata? {
        // Instagram share links (instagram.com/share/p/AbCdEf or a /reel/
        // link with tracking params from the app's Share sheet) can 302
        // before landing on the real /p/{shortcode}/ page. Resolve first so
        // shortcode() sees the canonical path.
        val canonicalUrl = if (url.contains("/share/")) resolveRedirect(url) else url
        val code = shortcode(canonicalUrl)
        if (code == null) {
            Log.d(TAG, "no shortcode extracted from $canonicalUrl (original: $url)")
            return null
        }

        // Meta dropped the access-token requirement on its oEmbed endpoints in
        // June 2026, so this needs no app, no token, no App Review. It gives you
        // the embed HTML (caption + author) but deliberately NO thumbnail_url —
        // Meta's own guidance is to read the image off the embed page instead.
        var title = ""
        var caption = ""
        val oembedUrl = "https://graph.facebook.com/v23.0/instagram_oembed?url=${enc(canonicalUrl)}&omitscript=true"
        val oembed = httpJson(oembedUrl, ua = CRAWLER_UA)
        if (oembed == null) {
            Log.d(TAG, "oembed request returned no/invalid JSON for $canonicalUrl")
        } else if (oembed.has("error")) {
            Log.d(TAG, "oembed error for $canonicalUrl: ${oembed.optJSONObject("error")}")
        }
        val html = oembed?.optString("html").orEmpty()
        if (html.isNotBlank()) {
            try {
                val doc = Jsoup.parse(html)
                caption = doc.select("blockquote p").text().trim()
                val anchorText = doc.select("blockquote a").firstOrNull()?.text()?.trim().orEmpty()
                if (!isGeneric(anchorText)) title = anchorText
                Log.d(TAG, "oembed parsed: anchor=\"$anchorText\" usableTitle=\"$title\" captionLen=${caption.length}")
            } catch (e: Exception) {
                Log.w(TAG, "oembed html parse failed", e)
            }
        } else {
            Log.d(TAG, "oembed html field was empty for $canonicalUrl")
        }

        // The /embed/captioned/ surface is the public, server-rendered embed
        // widget. It carries og:image and the display_url without a login wall
        // — but only when asked for with a crawler UA; a browser UA gets the
        // full JS app shell instead, with no metadata in it at all.
        var image = ""
        val embedUrl = "https://www.instagram.com/p/$code/embed/captioned/"
        val embedHtml = httpBody(embedUrl, ua = CRAWLER_UA)
        if (embedHtml == null) {
            Log.d(TAG, "embed page request failed entirely: $embedUrl")
        } else {
            Log.d(TAG, "embed page returned ${embedHtml.length} chars, title tag: " +
                    (Regex("<title>(.*?)</title>").find(embedHtml)?.groupValues?.get(1) ?: "none"))
            try {
                val doc = Jsoup.parse(embedHtml)
                image = doc.select("meta[property=og:image]").attr("content")
                if (image.isBlank()) image = doc.select("img.EmbeddedMediaImage").attr("src")
                if (image.isBlank()) {
                    image = Regex("\"display_url\":\"(.*?)\"")
                        .find(embedHtml)?.groupValues?.get(1)
                        ?.replace("\\u0026", "&")
                        ?.replace("\\/", "/")
                        .orEmpty()
                }
                if (title.isBlank()) {
                    val ogTitle = doc.select("meta[property=og:title]").attr("content")
                    if (!isGeneric(ogTitle)) title = ogTitle
                }
                if (caption.isBlank()) {
                    caption = doc.select(".Caption").text().trim()
                }
                Log.d(TAG, "embed page parsed: imageFound=${image.isNotBlank()} titleNow=\"$title\"")
            } catch (e: Exception) {
                Log.w(TAG, "embed page parse failed", e)
            }
        }

        if (title.isBlank() && caption.isBlank() && image.isBlank()) {
            // Last attempt: the plain post page with the same crawler UA that
            // makes fetchLocally work on ordinary sites. Some post types serve
            // OG tags here even when /embed/captioned/ comes back empty.
            val plainHtml = httpBody(canonicalUrl, ua = CRAWLER_UA)
            if (plainHtml != null) {
                try {
                    val doc = Jsoup.parse(plainHtml)
                    image = doc.select("meta[property=og:image]").attr("content")
                    val ogTitle = doc.select("meta[property=og:title]").attr("content")
                    if (!isGeneric(ogTitle)) title = ogTitle
                    caption = doc.select("meta[property=og:description]").attr("content")
                    Log.d(TAG, "plain-page crawler fallback: imageFound=${image.isNotBlank()} titleNow=\"$title\"")
                } catch (e: Exception) {
                    Log.w(TAG, "plain-page fallback parse failed", e)
                }
            }
        }

        if (title.isBlank() && caption.isBlank() && image.isBlank()) {
            Log.d(TAG, "all sources empty for $canonicalUrl, falling through to fetchLocally")
            return null
        }

        return LinkMetadata(
            title = title.ifBlank { "Instagram post" }.take(200),
            description = caption.take(500),
            faviconUrl = favicon("instagram.com"),
            previewImageUrl = image,
            domain = "instagram.com"
        )
    }
}

/* ------------------------------------------------------------------- Reddit */

object RedditResolver : LinkResolver {
    override fun matches(domain: String) =
        domain == "reddit.com" || domain.endsWith(".reddit.com") || domain.endsWith("redd.it")

    private const val TAG = "LinkResolvers.Reddit"

    override fun resolve(url: String): LinkMetadata? {
        // The `.json` trick is dead: Reddit shut off unauthenticated .json in
        // May 2026 and now returns 403 (and 403s datacenter IPs outright, which
        // is why the Vercel proxy fails). oEmbed is the only no-auth surface
        // left and it is not guaranteed — expect nulls and fall through.
        // The durable fix is OAuth against oauth.reddit.com; see notes.
        //
        // Share links from the Reddit app (reddit.com/r/sub/s/AbCdEf, or bare
        // redd.it/AbCdEf) don't match oEmbed's URL pattern for "this is a
        // specific post" — it can't tell them apart from a subreddit link, so
        // it falls back to subreddit-level title/description. Resolving the
        // redirect first gets the real /comments/.../ URL oEmbed recognizes.
        val canonicalUrl = if (url.contains("/s/") || url.contains("redd.it")) {
            resolveRedirect(url).also {
                Log.d(TAG, "resolved share link $url -> $it")
            }
        } else {
            Log.d(TAG, "using url as-is (no share-link pattern matched): $url")
            url
        }

        val oembedUrl = "https://www.reddit.com/oembed?url=${enc(canonicalUrl)}"
        val json = httpJson(oembedUrl, ua = MOBILE_UA)
        if (json == null) {
            Log.d(TAG, "oembed request returned no/invalid JSON for $canonicalUrl")
            return null
        }
        if (json.has("error") || json.has("message")) {
            Log.d(TAG, "oembed error payload for $canonicalUrl: $json")
        }
        val title = json.optString("title")
        Log.d(TAG, "oembed title=\"$title\" for $canonicalUrl")
        if (title.isBlank()) return null

        // If oEmbed still gave us something generic, this doesn't catch it
        // reliably (it's a title, not a flag) — but it's a cheap guard against
        // the exact "r/subredditname" bare-community title shape.
        if (Regex("^r/\\w+$", RegexOption.IGNORE_CASE).matches(title.trim())) {
            Log.d(TAG, "oembed returned subreddit-level title for $canonicalUrl — resolver may need a second redirect pass")
        }

        // oEmbed is a text-only widget (title + author + subreddit link) —
        // Reddit's schema never included an image field here, so this isn't
        // a fallback for a failure, it's the normal second half of the job.
        // The post page carries a server-rendered og:image, but — same as
        // Instagram — Reddit appears to serve that only to allowlisted
        // crawler UAs (Discordbot, Slackbot) and a client-rendered shell
        // with no metadata to anything that looks like an ordinary browser.
        var image = ""
        val postHtml = httpBody(canonicalUrl, ua = "Discordbot/2.0 (+https://discordapp.com)")
        if (postHtml == null) {
            Log.d(TAG, "post page fetch failed for $canonicalUrl, no image available")
        } else {
            Log.d(TAG, "post page returned ${postHtml.length} chars")
            try {
                val doc = Jsoup.parse(postHtml)
                image = doc.select("meta[property=og:image]").attr("content")
                if (image.isBlank()) {
                    image = doc.select("meta[name=twitter:image]").attr("content")
                }
                Log.d(TAG, "post page og:image=\"$image\"")
            } catch (e: Exception) {
                Log.w(TAG, "post page parse failed", e)
            }
        }

        // A text/self post genuinely has no image — Reddit doesn't generate
        // one, so a blank result here isn't necessarily a bug to chase.
        return LinkMetadata(
            title = title.take(200),
            description = json.optString("author_name"),
            faviconUrl = favicon("reddit.com"),
            previewImageUrl = image,
            domain = "reddit.com"
        )
    }
}

/* ------------------------------------------------------------------ Registry */

val LINK_RESOLVERS: List<LinkResolver> = listOf(
    YouTubeResolver,
    TikTokResolver,
    TwitterResolver,
    InstagramResolver,
    RedditResolver
)

fun resolverFor(domain: String): LinkResolver? =
    LINK_RESOLVERS.firstOrNull { it.matches(domain) }

/**
 * Domains with no public embed surface at all. Don't burn 15 seconds of
 * timeouts on them — go straight to a placeholder card.
 */
val NO_PREVIEW_DOMAINS = setOf(
    "facebook.com", "fb.com", "linkedin.com", "snapchat.com", "pinterest.com"
)

fun isNoPreviewDomain(domain: String) =
    NO_PREVIEW_DOMAINS.any { domain == it || domain.endsWith(".$it") }