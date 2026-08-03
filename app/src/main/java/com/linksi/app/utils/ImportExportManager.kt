package com.linksi.app.utils

import android.content.Context
import android.net.Uri
import com.linksi.app.domain.model.Link
import com.linksi.app.domain.model.Folder
import com.linksi.app.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── Linksi JSON format ────────────────────────────────────────
fun exportLinksToJson(links: List<Link>, folders: List<Folder>): String {
    val root = JSONObject()
    root.put("app", "Linksi")
    root.put("version", 12)
    root.put("exportedAt", System.currentTimeMillis())

    val foldersArr = JSONArray()
    folders.forEach { folder ->
        foldersArr.put(JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("icon", folder.icon)
            put("color", folder.color)
            put("createdAt", folder.createdAt)
            put("isLocked", folder.isLocked)
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
            put("isPinned", link.isPinned)
            put("createdAt", link.createdAt)
            put("reminderAt", link.reminderAt ?: JSONObject.NULL)
            put("expiresAt", link.expiresAt ?: JSONObject.NULL)
            put("deletedAt", link.deletedAt ?: JSONObject.NULL)
            put("previewImageUrl", link.previewImageUrl)
            put("note", link.note)
            put("inBin", link.inBin)
            put("preventScreenshot", link.preventScreenshot)
            put("tags", JSONArray(link.tags))
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
        ?.bufferedReader()?.readText() ?: throw Exception(context.getString(R.string.cannot_read_file))

    val root = JSONObject(json)
    if (root.optString("app") != "Linksi") throw Exception(context.getString(R.string.not_a_linksi_file))

    val folders = mutableListOf<Folder>()
    val foldersArr = root.optJSONArray("folders")
    if (foldersArr != null) {
        for (i in 0 until foldersArr.length()) {
            val f = foldersArr.getJSONObject(i)
            folders.add(Folder(
                id = 0, // reset ID, will be re-inserted
                name = f.getString("name"),
                icon = f.optString("icon", "folder"),
                color = f.optString("color", "#6750A4"),
                createdAt = f.optLong("createdAt", System.currentTimeMillis()),
                isLocked = f.optBoolean("isLocked", false)
            ))
        }
    }

    val links = mutableListOf<Link>()
    val linksArr = root.getJSONArray("links")
    for (i in 0 until linksArr.length()) {
        val l = linksArr.getJSONObject(i)
        
        val tags = mutableListOf<String>()
        val tagsArr = l.optJSONArray("tags")
        if (tagsArr != null) {
            for (j in 0 until tagsArr.length()) {
                tags.add(tagsArr.getString(j))
            }
        }

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
            isPinned = l.optBoolean("isPinned"),
            createdAt = l.optLong("createdAt", System.currentTimeMillis()),
            reminderAt = if (l.isNull("reminderAt")) null else l.optLong("reminderAt"),
            expiresAt = if (l.isNull("expiresAt")) null else l.optLong("expiresAt"),
            deletedAt = if (l.isNull("deletedAt")) null else l.optLong("deletedAt"),
            previewImageUrl = l.optString("previewImageUrl"),
            note = l.optString("note"),
            inBin = l.optBoolean("inBin"),
            preventScreenshot = l.optBoolean("preventScreenshot"),
            tags = tags
        ))
    }

    return ImportResult(links, folders, "Linksi", links.size)
}

// ── Import Chrome / Browser HTML bookmarks ────────────────────
fun importFromBrowserHtml(context: Context, uri: Uri): ImportResult {
    val html = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()?.readText() ?: throw Exception(context.getString(R.string.cannot_read_file))

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
    sb.appendLine("url,title,description,domain,tags,isFavorite,isRead,isPinned,createdAt,note,previewImageUrl")
    links.forEach { link ->
        sb.appendLine(
            "${csvEscape(link.url)},${csvEscape(link.title)},${csvEscape(link.description)}," +
                    "${csvEscape(link.domain)},${csvEscape(link.tags.joinToString(";"))}," +
                    "${link.isFavorite},${link.isRead},${link.isPinned},${link.createdAt}," +
                    "${csvEscape(link.note)},${csvEscape(link.previewImageUrl)}"
        )
    }
    return sb.toString()
}

// ── Export as HTML ────────────────────────────────────────────
fun exportLinksToHtml(links: List<Link>): String {
    val sb = StringBuilder()
    sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
    sb.appendLine("<!-- This is an automatically generated file.")
    sb.appendLine("     It will be read and written.")
    sb.appendLine("     DO NOT EDIT! -->")
    sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
    sb.appendLine("<TITLE>Bookmarks</TITLE>")
    sb.appendLine("<H1>Bookmarks</H1>")
    sb.appendLine("<DL><p>")
    sb.appendLine("    <DT><H3 ADD_DATE=\"${System.currentTimeMillis() / 1000}\" LAST_MODIFIED=\"${System.currentTimeMillis() / 1000}\">Linksi Export</H3>")
    sb.appendLine("    <DL><p>")

    links.forEach { link ->
        val addDate = link.createdAt / 1000
        sb.appendLine("        <DT><A HREF=\"${link.url}\" ADD_DATE=\"$addDate\">${link.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</A>")
    }

    sb.appendLine("    </DL><p>")
    sb.appendLine("</DL><p>")
    return sb.toString()
}

private fun csvEscape(value: String) = "\"${value.replace("\"", "\"\"")}\""

// ── File name helpers ─────────────────────────────────────────
fun exportFileName(format: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "linksi-export-$date.$format"
}
