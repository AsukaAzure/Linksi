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
            put("createdAt", folder.createdAt)
            put("isLocked", folder.isLocked)
            put("parentId", folder.parentId ?: JSONObject.NULL)
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

    val links = mutableListOf<Link>()
    val folders = root.optJSONArray("folders")?.let { arr ->
        (0 until arr.length()).map { i ->
            val f = arr.getJSONObject(i)
            Folder(
                id = f.optLong("id", 0),
                name = f.getString("name"),
                icon = f.optString("icon", "folder"),
                color = f.optString("color", "#6750A4"),
                createdAt = f.optLong("createdAt", System.currentTimeMillis()),
                isLocked = f.optBoolean("isLocked", false),
                parentId = if (f.isNull("parentId")) null else f.optLong("parentId")
            )
        }
    } ?: emptyList()

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
            folderId = if (l.isNull("folderId")) null else l.optLong("folderId"),
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
    val folders = mutableListOf<Folder>()
    
    // Improved regex-based stack parsing for HTML bookmarks
    val pattern = Regex("""<(H3|A|/DL)(?:\s+HREF="([^"]+)")?[^>]*>([^<]+)?""", RegexOption.IGNORE_CASE)
    
    val folderStack = mutableListOf<Long>()
    var folderCounter = 1L

    pattern.findAll(html).forEach { match ->
        val tag = match.groupValues[1].uppercase()
        val url = match.groupValues[2]
        val text = match.groupValues[3].trim()

        when (tag) {
            "H3" -> {
                val folderName = text
                if (folderName.isNotBlank() && folderName.lowercase() != "bookmarks bar" && folderName.lowercase() != "other bookmarks") {
                    val tempId = folderCounter++
                    folders.add(Folder(id = tempId, name = folderName, parentId = folderStack.lastOrNull()))
                    folderStack.add(tempId)
                }
            }
            "A" -> {
                if (url.isNotBlank()) {
                    links.add(Link(
                        id = 0,
                        url = url,
                        title = text,
                        folderId = folderStack.lastOrNull(),
                        domain = extractDomain(url),
                        faviconUrl = "https://www.google.com/s2/favicons?domain=${extractDomain(url)}&sz=64"
                    ))
                }
            }
            "/DL" -> {
                if (folderStack.isNotEmpty()) {
                    folderStack.removeAt(folderStack.size - 1)
                }
            }
        }
    }

    return ImportResult(links, folders, "Browser", links.size)
}

// ── Export as CSV ─────────────────────────────────────────────
fun exportLinksToCsv(links: List<Link>, folders: List<Folder>): String {
    val sb = StringBuilder()
    sb.appendLine("url,title,description,domain,tags,isFavorite,isRead,isPinned,createdAt,note,previewImageUrl,folder")
    val folderMap = folders.associateBy { it.id }
    links.forEach { link ->
        val folderName = link.folderId?.let { folderMap[it]?.name } ?: ""
        sb.appendLine(
            "${csvEscape(link.url)},${csvEscape(link.title)},${csvEscape(link.description)}," +
                    "${csvEscape(link.domain)},${csvEscape(link.tags.joinToString(";"))}," +
                    "${link.isFavorite},${link.isRead},${link.isPinned},${link.createdAt}," +
                    "${csvEscape(link.note)},${csvEscape(link.previewImageUrl)},${csvEscape(folderName)}"
        )
    }
    return sb.toString()
}

// ── Import from CSV ───────────────────────────────────────────
fun importFromCsv(context: Context, uri: Uri): ImportResult {
    val csv = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()?.readText() ?: throw Exception(context.getString(R.string.cannot_read_file))

    val lines = csv.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) throw Exception(context.getString(R.string.cannot_read_file))

    val header = lines[0].split(",")
    val links = mutableListOf<Link>()
    val folders = mutableListOf<Folder>()
    val folderMap = mutableMapOf<String, Long>()
    var folderCounter = 1L

    for (i in 1 until lines.size) {
        val parts = parseCsvLine(lines[i])
        if (parts.size < header.size) continue

        val linkData = header.zip(parts).toMap()
        val folderName = linkData["folder"]?.trim() ?: ""
        var folderId: Long? = null

        if (folderName.isNotBlank()) {
            folderId = folderMap[folderName]
            if (folderId == null) {
                folderId = folderCounter++
                folderMap[folderName] = folderId
                folders.add(Folder(id = folderId, name = folderName))
            }
        }

        links.add(Link(
            id = 0,
            url = linkData["url"] ?: "",
            title = linkData["title"] ?: "",
            description = linkData["description"] ?: "",
            domain = linkData["domain"] ?: extractDomain(linkData["url"] ?: ""),
            tags = linkData["tags"]?.split(";")?.filter { it.isNotBlank() } ?: emptyList(),
            isFavorite = linkData["isFavorite"]?.toBoolean() ?: false,
            isRead = linkData["isRead"]?.toBoolean() ?: false,
            isPinned = linkData["isPinned"]?.toBoolean() ?: false,
            createdAt = linkData["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
            note = linkData["note"] ?: "",
            previewImageUrl = linkData["previewImageUrl"] ?: "",
            folderId = folderId
        ))
    }

    return ImportResult(links, folders, "CSV", links.size)
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                current.append('\"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == ',' && !inQuotes) {
            result.add(current.toString())
            current = StringBuilder()
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString())
    return result
}

// ── Export as HTML ────────────────────────────────────────────
fun exportLinksToHtml(links: List<Link>, folders: List<Folder>): String {
    val sb = StringBuilder()
    sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
    sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
    sb.appendLine("<TITLE>Bookmarks</TITLE>")
    sb.appendLine("<H1>Bookmarks</H1>")
    sb.appendLine("<DL><p>")

    val foldersByParent = folders.groupBy { it.parentId }
    val linksByFolder = links.groupBy { it.folderId }

    fun writeFolder(parentId: Long?, indent: String) {
        // Folders at this level
        foldersByParent[parentId]?.forEach { folder ->
            sb.appendLine("$indent    <DT><H3 ADD_DATE=\"${folder.createdAt / 1000}\">${folder.name}</H3>")
            sb.appendLine("$indent    <DL><p>")
            
            // Sub-folders
            writeFolder(folder.id, "$indent    ")
            
            // Links in this folder
            linksByFolder[folder.id]?.forEach { link ->
                val addDate = link.createdAt / 1000
                sb.appendLine("$indent        <DT><A HREF=\"${link.url}\" ADD_DATE=\"$addDate\">${link.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</A>")
            }
            sb.appendLine("$indent    </DL><p>")
        }
    }

    // Start with root folders
    writeFolder(null, "")

    // Export uncategorized links (at root)
    linksByFolder[null]?.forEach { link ->
        val addDate = link.createdAt / 1000
        sb.appendLine("    <DT><A HREF=\"${link.url}\" ADD_DATE=\"$addDate\">${link.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</A>")
    }

    sb.appendLine("</DL><p>")
    return sb.toString()
}

private fun csvEscape(value: String) = "\"${value.replace("\"", "\"\"")}\""

// ── File name helpers ─────────────────────────────────────────
fun exportFileName(format: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "linksi-export-$date.$format"
}
