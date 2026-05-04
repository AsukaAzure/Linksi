package com.linksi.app.utils

import android.content.Context
import android.net.Uri
import com.linksi.app.domain.model.Link
import com.linksi.app.domain.model.Folder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── Linksi JSON format ────────────────────────────────────────
fun exportLinksToJson(links: List<Link>, folders: List<Folder>): String {
    val root = JSONObject()
    root.put("app", "Linksi")
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())

    val foldersArr = JSONArray()
    folders.forEach { folder ->
        foldersArr.put(JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("icon", folder.icon)
            put("color", folder.color)
        })
    }
    root.put("folders", foldersArr)

    val linksArr = JSONArray()
    links.forEach { link ->
        linksArr.put(JSONObject().apply {
            put("id", link.id)
            put("url", link.url)
            put("title", link.title)
            put("description", link.description)
            put("domain", link.domain)
            put("faviconUrl", link.faviconUrl)
            put("folderId", link.folderId ?: JSONObject.NULL)
            put("isFavorite", link.isFavorite)
            put("isRead", link.isRead)
            put("createdAt", link.createdAt)
        })
    }
    root.put("links", linksArr)
    return root.toString(2)
}

data class ImportResult(
    val links: List<Link>,
    val folders: List<Folder>,
    val source: String,
    val count: Int
)

// ── Import Linksi JSON ────────────────────────────────────────
fun importFromLinksJson(context: Context, uri: Uri): ImportResult {
    val json = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()?.readText() ?: throw Exception("Cannot read file")

    val root = JSONObject(json)
    if (root.optString("app") != "Linksi") throw Exception("Not a Linksi export file")

    val folders = mutableListOf<Folder>()
    val foldersArr = root.optJSONArray("folders")
    if (foldersArr != null) {
        for (i in 0 until foldersArr.length()) {
            val f = foldersArr.getJSONObject(i)
            folders.add(Folder(
                id = 0, // reset ID, will be re-inserted
                name = f.getString("name"),
                icon = f.optString("icon", "folder"),
                color = f.optString("color", "#6750A4")
            ))
        }
    }

    val links = mutableListOf<Link>()
    val linksArr = root.getJSONArray("links")
    for (i in 0 until linksArr.length()) {
        val l = linksArr.getJSONObject(i)
        links.add(Link(
            id = 0,
            url = l.getString("url"),
            title = l.optString("title"),
            description = l.optString("description"),
            domain = l.optString("domain"),
            faviconUrl = l.optString("faviconUrl"),
            folderId = null, // re-mapped after folder insert
            isFavorite = l.optBoolean("isFavorite"),
            isRead = l.optBoolean("isRead"),
            createdAt = l.optLong("createdAt", System.currentTimeMillis())
        ))
    }

    return ImportResult(links, folders, "Linksi", links.size)
}

// ── Import Chrome / Browser HTML bookmarks ────────────────────
fun importFromBrowserHtml(context: Context, uri: Uri): ImportResult {
    val html = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()?.readText() ?: throw Exception("Cannot read file")

    val links = mutableListOf<Link>()
    // Match <A HREF="url" ...>title</A>
    val pattern = Regex("""<A\s+HREF="(https?://[^"]+)"[^>]*>([^<]+)</A>""",
        RegexOption.IGNORE_CASE)

    pattern.findAll(html).forEach { match ->
        val url = match.groupValues[1]
        val title = match.groupValues[2].trim()
        links.add(Link(
            id = 0,
            url = url,
            title = title,
            domain = extractDomain(url),
            faviconUrl = "https://www.google.com/s2/favicons?domain=${extractDomain(url)}&sz=64"
        ))
    }

    return ImportResult(links, emptyList(), "Browser", links.size)
}

// ── Export as CSV ─────────────────────────────────────────────
fun exportLinksToCsv(links: List<Link>): String {
    val sb = StringBuilder()
    sb.appendLine("url,title,description,domain,tags,isFavorite,isRead,createdAt")
    links.forEach { link ->
        sb.appendLine(
            "${csvEscape(link.url)},${csvEscape(link.title)},${csvEscape(link.description)}," +
                    "${csvEscape(link.domain)}," +
                    "${link.isFavorite},${link.isRead},${link.createdAt}"
        )
    }
    return sb.toString()
}

private fun csvEscape(value: String) = "\"${value.replace("\"", "\"\"")}\""

// ── File name helpers ─────────────────────────────────────────
fun exportFileName(format: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "linksi-export-$date.$format"
}
