package com.linksi.app.utils

import android.util.Log
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
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

/**
 * Single greppable tag for the whole metadata-fetch pipeline. Everything the
 * resolver/fetcher does is logged here at INFO level, so one command shows
 * the full story of a save:
 *
 *     adb logcat -s LinksiMeta
 */
private const val LOG_TAG = "LinksiMeta"

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
        Log.i(LOG_TAG, "[Http] $url -> HTTP ${res.statusCode()} (${res.statusMessage()})")
        null
    }
} catch (e: Exception) {
    Log.i(LOG_TAG, "[Http] $url -> request failed: ${e.javaClass.simpleName}: ${e.message}")
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

    private const val TAG = LOG_TAG

    override fun resolve(url: String): LinkMetadata? {
        Log.i(TAG, "[Reddit] resolve start: $url")
        // Share links from the Reddit app (reddit.com/r/sub/s/AbCdEf, or bare
        // redd.it/AbCdEf) don't match oEmbed's URL pattern, and redd.it even
        // redirects to the bare /comments/{id} form that oEmbed rejects with
        // "invalid URL value". Resolve the redirect first; the feed below
        // handles every remaining URL form.
        val canonicalUrl = if (url.contains("/s/") || url.contains("redd.it")) {
            resolveRedirect(url).also {
                Log.i(TAG, "[Reddit] resolved share link $url -> $it")
            }
        } else {
            Log.i(TAG, "[Reddit] using url as-is (no share-link pattern matched)")
            url
        }

        // Primary surface when configured: the official API (Settings → Reddit).
        // The only source that carries the real full-resolution image and the
        // vote/comment counts — no unauthenticated surface has either, and the
        // API endpoint isn't subject to the bot-walling that killed the rest.
        RedditApiClient.resolvePost(canonicalUrl)?.let { post ->
            Log.i(TAG, "[Reddit] API hit: score=${post.score} comments=${post.numComments} image=${post.imageUrl.ifBlank { "(none)" }}")
            val description = buildString {
                append(post.score).append(" votes, ").append(post.numComments).append(" comments")
                if (post.selftext.isNotBlank()) append(". ").append(post.selftext.take(300))
            }
            return LinkMetadata(
                title = post.title.ifBlank { "Reddit post" }.take(200),
                description = description.take(500),
                faviconUrl = favicon("reddit.com"),
                previewImageUrl = post.imageUrl,
                domain = "reddit.com",
                upvotes = post.score,
                numComments = post.numComments
            )
        }
        Log.i(TAG, "[Reddit] API not used (no client ID or request failed) — unauthenticated surfaces next")

        // Primary surface: the post's Atom feed at {url}/.rss. Still fully
        // unauthenticated (unlike the .json API, which has returned 403 since
        // May 2026), works with the app's own user-agent — no crawler
        // impersonation — and one request carries everything: the post entry
        // (id "t3_...") has the title, the author, and the image. Share-link
        // redirects come back with ?share_id=... attached (Reddit's own
        // tracking param) — strip it so the feed request is canonical.
        var title = ""
        var author = ""
        var image = ""
        val feedUrl = canonicalUrl.substringBefore('?').trimEnd('/') + "/.rss"
        var feedXml = httpBody(feedUrl, ua = APP_UA)
        if (feedXml == null) {
            // www.reddit.com rate-limits bursts with 429 and httpBody maps any
            // non-2xx to null. fetchAll's per-domain lock serializes callers,
            // so one short backoff-and-retry is safe here and recovers the
            // common single-save 429.
            Thread.sleep(1200)
            feedXml = httpBody(feedUrl, ua = APP_UA)
        }
        if (feedXml == null) {
            Log.i(TAG, "[Reddit] feed failed even after retry: $feedUrl")
        } else {
            parseRedditFeed(feedXml)?.let { feed ->
                title = feed.title
                author = feed.author
                image = feed.imageUrl
                Log.i(TAG, "[Reddit] feed OK: title=\"$title\" author=\"$author\" image=$image")
            } ?: run {
                // A 200 response that isn't a feed usually means Reddit served
                // its bot-challenge page to this network. The snippet says which.
                val snippet = feedXml.take(160).replace('\n', ' ')
                Log.i(TAG, "[Reddit] feed returned ${feedXml.length} chars but no entries. Starts with: $snippet")
            }
        }

        // Fallback 1: oEmbed (title + author, never an image) when the feed
        // is missing or malformed.
        if (title.isBlank()) {
            val json = httpJson("https://www.reddit.com/oembed?url=${enc(canonicalUrl)}", ua = MOBILE_UA)
            if (json != null && !json.has("error") && !json.has("message")) {
                title = json.optString("title")
                author = json.optString("author_name")
                Log.i(TAG, "[Reddit] oEmbed fallback: title=\"$title\" author=\"$author\"")
            }
        }

        // Second surface, always fetched: old.reddit.com serves fully
        // server-rendered HTML to any browser UA — no challenge, no crawler
        // games. It's the only place with the post's upvotes and comment
        // count (the pill on the card), and its thumbnail fills the image
        // when the feed had none. Text posts genuinely have neither — a
        // redditstatic sprite in the thumbnail slot is filtered out.
        var upvotes = 0
        var numComments = 0
        run {
            val path = try { URI(canonicalUrl).path } catch (e: Exception) { "" }
            if (path.isNotBlank()) {
                val oldHtml = httpBody("https://old.reddit.com$path", ua = MOBILE_UA)
                if (oldHtml == null) {
                    Log.i(TAG, "[Reddit] old.reddit fetch failed — no stats or image fallback")
                } else {
                    val stats = parseOldRedditPage(oldHtml)
                    upvotes = stats.score
                    numComments = stats.numComments
                    if (image.isBlank()) image = stats.imageUrl
                    Log.i(TAG, "[Reddit] old.reddit: score=$upvotes comments=$numComments image=${image.ifBlank { "(none)" }}")
                }
            }
        }


        if (title.isBlank() && image.isBlank()) {
            Log.i(TAG, "[Reddit] all surfaces empty, falling through to generic scrape")
            return null
        }

        // share.redd.it/preview/post/{id} is Reddit's generated share card —
        // the post title rendered on a branded background with the snoo logo,
        // not the post's own image. The og:image fallback above returns it
        // for every post, so it must never reach a card.
        if (image.contains("share.redd.it")) {
            Log.i(TAG, "[Reddit] DROPPING branded share-card image (this is what showed as the big reddit logo)")
            image = ""
        }

        Log.i(TAG, "[Reddit] resolve done: title=\"$title\" author=\"$author\" image=$image")

        // A text/self post genuinely has no image — Reddit doesn't generate
        // one, so a blank result here isn't necessarily a bug to chase.
        return LinkMetadata(
            title = title.ifBlank { "Reddit post" }.take(200),
            description = author,
            faviconUrl = favicon("reddit.com"),
            previewImageUrl = image,
            domain = "reddit.com",
            upvotes = upvotes,
            numComments = numComments
        )
    }
}

internal data class OldRedditStats(
    val score: Int,
    val numComments: Int,
    val imageUrl: String
)

/**
 * Extracts score, comment count and thumbnail from an old.reddit post page.
 *
 * The score div's text rounds big numbers ("12.5k") — its title attribute
 * holds the exact count, so prefer that and fall back to the text.
 */
internal fun parseOldRedditPage(html: String): OldRedditStats {
    return try {
        val doc = Jsoup.parse(html)
        val scoreEl = doc.selectFirst("div.score")
        val score = scoreEl?.attr("title")?.filter { it.isDigit() }?.toIntOrNull()
            ?: scoreEl?.text()?.filter { it.isDigit() }?.toIntOrNull()
            ?: 0
        val numComments = doc.selectFirst("a.comments")?.text()
            ?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val imageUrl = doc.select("a.thumbnail img[src]")
            .firstOrNull { it.attr("src").contains("redd.it/") }
            ?.attr("src").orEmpty()
        OldRedditStats(score, numComments, imageUrl)
    } catch (e: Exception) {
        OldRedditStats(0, 0, "")
    }
}

internal data class RedditFeedInfo(
    val title: String,
    val author: String,
    val imageUrl: String
)

/**
 * Extracts the post (not the comments) from a Reddit Atom feed.
 *
 * The feed for a comments URL contains one entry per comment after the
 * submission; entries are distinguishable by id — the post is "t3_...",
 * comments are "t1_...". The image comes from media:thumbnail when present,
 * otherwise from the first redd.it <img> in the entry's HTML content (video
 * and some post types carry the thumbnail there instead).
 */
internal fun parseRedditFeed(xml: String): RedditFeedInfo? {
    return try {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val entries = doc.select("entry")
        val postEntry = entries.firstOrNull { it.selectFirst("id")?.text()?.startsWith("t3_") == true }
            ?: entries.firstOrNull() ?: return null

        var imageUrl = postEntry.getElementsByTag("media:thumbnail").attr("url")
        if (imageUrl.isBlank()) {
            // <content> holds entity-escaped HTML; text() gives it back decoded.
            val contentHtml = postEntry.selectFirst("content")?.text().orEmpty()
            if (contentHtml.isNotBlank()) {
                imageUrl = Jsoup.parse(contentHtml)
                    .select("img[src]")
                    .firstOrNull { it.attr("src").contains("redd.it") }
                    ?.attr("src").orEmpty()
            }
        }

        RedditFeedInfo(
            title = postEntry.selectFirst("title")?.text()?.trim().orEmpty(),
            author = postEntry.selectFirst("author > name")?.text()?.trim().orEmpty().removePrefix("/u/"),
            imageUrl = imageUrl
        )
    } catch (e: Exception) {
        null
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
 * Domains with no public surface at all (verified October 2026: these serve a
 * JS-only shell to every crawler identity tried — facebookexternalhit,
 * Discordbot, Googlebot, TelegramBot). Don't burn 10 seconds of timeouts on
 * them — go straight to the WebView attempt or a placeholder card.
 *
 * Facebook, Pinterest and LinkedIn used to be listed here too, but all three
 * now serve complete OG tags to crawler user-agents, which is exactly what
 * MetadataFetcher.fetchLocally sends (facebookexternalhit/1.1). Their login
 * walls are caught by BLOCKED_TITLE_PATTERN instead.
 */
val NO_PREVIEW_DOMAINS = setOf(
    "snapchat.com", "threads.net"
)

fun isNoPreviewDomain(domain: String) =
    NO_PREVIEW_DOMAINS.any { domain == it || domain.endsWith(".$it") }