package com.linksi.app.data.repository

import com.linksi.app.data.db.*
import com.linksi.app.domain.model.*
import com.linksi.app.utils.LinkMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRepository @Inject constructor(
    private val linkDao: LinkDao,
    private val folderDao: FolderDao,
    private val metadataCacheDao: MetadataCacheDao
) {
    // ─── Links ───────────────────────────────────────────────
    fun getAllLinks(isFolderLockEnabled: Boolean = true): Flow<List<Link>> =
        linkDao.getAllLinks(isFolderLockEnabled).map { it.map(::toLink) }

    fun getLinksByFolder(folderId: Long): Flow<List<Link>> =
        linkDao.getLinksByFolder(folderId).map { it.map(::toLink) }

    fun getUncategorizedLinks(): Flow<List<Link>> =
        linkDao.getUncategorizedLinks().map { it.map(::toLink) }

    fun getFavoriteLinks(isFolderLockEnabled: Boolean = true): Flow<List<Link>> =
        linkDao.getFavoriteLinks(isFolderLockEnabled).map { it.map(::toLink) }

    fun getUnreadLinks(isFolderLockEnabled: Boolean = true): Flow<List<Link>> =
        linkDao.getUnreadLinks(isFolderLockEnabled).map { it.map(::toLink) }

    fun searchLinks(query: String, isFolderLockEnabled: Boolean = true): Flow<List<Link>> =
        linkDao.searchLinks(query, isFolderLockEnabled).map { it.map(::toLink) }

    fun getLinksWithReminders(isFolderLockEnabled: Boolean = true): Flow<List<Link>> =
        linkDao.getLinksWithReminders(isFolderLockEnabled).map { it.map(::toLink) }

    suspend fun insertLink(link: Link): Long =
        linkDao.insertLink(toEntity(link))

    suspend fun updateLink(link: Link) =
        linkDao.updateLink(toEntity(link))

    suspend fun deleteLink(link: Link) =
        linkDao.deleteLink(toEntity(link))

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        linkDao.toggleFavorite(id, isFavorite)

    suspend fun markAsRead(id: Long, isRead: Boolean) =
        linkDao.markAsRead(id, isRead)

    suspend fun moveToFolder(id: Long, folderId: Long?) =
        linkDao.moveToFolder(id, folderId)

    suspend fun moveToBin(id: Long) =
        linkDao.moveToBin(id)

    suspend fun restoreFromBin(id: Long) =
        linkDao.restoreFromBin(id)

    fun getLinksInBin(): Flow<List<Link>> =
        linkDao.getLinksInBin().map { it.map(::toLink) }

    suspend fun cleanBin(days: Int = 30) {
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        linkDao.cleanBin(threshold)
    }

    suspend fun getLinkById(id: Long): Link? =
        linkDao.getLinkById(id)?.let(::toLink)

    suspend fun getTotalCount(): Int = linkDao.getTotalCount()

    suspend fun getAllTags(): List<String> {
        return linkDao.getAllTagStrings()
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }


    // ─── Folders ─────────────────────────────────────────────
    fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getAllFoldersWithCount().map { list ->
            list.map {
                Folder(
                    it.folder.id,
                    it.folder.name,
                    it.folder.icon,
                    it.folder.color,
                    it.folder.createdAt,
                    it.linkCount,
                    it.folder.isLocked,
                    it.folder.parentId,
                    it.latestImages?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                )
            }
        }

    fun getFoldersByParent(parentId: Long?): Flow<List<Folder>> =
        folderDao.getFoldersByParentWithCount(parentId).map { list ->
            list.map {
                Folder(
                    it.folder.id,
                    it.folder.name,
                    it.folder.icon,
                    it.folder.color,
                    it.folder.createdAt,
                    it.linkCount,
                    it.folder.isLocked,
                    it.folder.parentId,
                    it.latestImages?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                )
            }
        }

    suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insertFolder(
            FolderEntity(
                id = folder.id,
                name = folder.name,
                icon = folder.icon,
                color = folder.color,
                createdAt = folder.createdAt,
                isLocked = folder.isLocked,
                parentId = folder.parentId
            )
        )
    }

    suspend fun updateFolder(folder: Folder) =
        folderDao.updateFolder(
            FolderEntity(
                folder.id,
                folder.name,
                folder.icon,
                folder.color,
                folder.createdAt,
                isLocked = folder.isLocked,
                parentId = folder.parentId
            )
        )

    suspend fun deleteFolder(folder: Folder) =
        folderDao.deleteFolder(
            FolderEntity(
                folder.id,
                folder.name,
                folder.icon,
                folder.color,
                folder.createdAt,
                isLocked = folder.isLocked,
                parentId = folder.parentId
            )
        )

    suspend fun toggleFolderLock(id: Long, isLocked: Boolean) =
        folderDao.toggleLock(id, isLocked)

    suspend fun getFolderById(id: Long): Folder? =
        folderDao.getFolderById(id)?.let { toFolder(it) }

    suspend fun getFolderByName(name: String): Folder? =
        folderDao.getFolderByName(name)?.let { toFolder(it) }

    suspend fun getFolderByNameAndParent(name: String, parentId: Long?): Folder? =
        folderDao.getFolderByNameAndParent(name, parentId)?.let { toFolder(it) }

    suspend fun isUrlAlreadySaved(url: String): Boolean {
        return linkDao.getLinkByUrl(com.linksi.app.utils.normalizeUrl(url)) != null
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) = linkDao.setPinned(id, isPinned)
    suspend fun setNote(id: Long, note: String) = linkDao.setNote(id, note)
    suspend fun setExpiry(id: Long, expiresAt: Long?) = linkDao.setExpiry(id, expiresAt)
    suspend fun getExpiredLinks() = linkDao.getExpiredLinks().map(::toLink)

    // ─── Metadata Cache ──────────────────────────────────────
    suspend fun getMetadataFromCache(url: String): LinkMetadata? {
        return metadataCacheDao.getMetadata(url)?.let { entity ->
            LinkMetadata(
                title = entity.title,
                description = entity.description,
                faviconUrl = entity.faviconUrl,
                previewImageUrl = entity.previewImageUrl,
                domain = entity.domain
            )
        }
    }

    suspend fun saveMetadataToCache(url: String, meta: LinkMetadata) {
        metadataCacheDao.insertMetadata(
            MetadataCacheEntity(
                url = url,
                title = meta.title,
                description = meta.description,
                faviconUrl = meta.faviconUrl,
                previewImageUrl = meta.previewImageUrl,
                domain = meta.domain
            )
        )
    }

    suspend fun clearOldMetadataCache(days: Int = 7) {
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        metadataCacheDao.clearOldMetadata(threshold)
    }

    // ─── Mappers ─────────────────────────────────────────────
    private fun toLink(entity: LinkEntity) = Link(
        id = entity.id,
        url = entity.url,
        title = entity.title,
        description = entity.description,
        faviconUrl = entity.faviconUrl,
        folderId = entity.folderId,
        isFavorite = entity.isFavorite,
        isRead = entity.isRead,
        createdAt = entity.createdAt,
        reminderAt = entity.reminderAt,
        previewImageUrl = entity.previewImageUrl,
        domain = entity.domain,
        isPinned = entity.isPinned,
        note = entity.note,
        expiresAt = entity.expiresAt,
        tags = entity.tags.split(",").filter { it.isNotBlank() },
        inBin = entity.inBin,
        deletedAt = entity.deletedAt,
        preventScreenshot = entity.preventScreenshot
    )

    private fun toEntity(link: Link) = LinkEntity(
        id = link.id,
        url = link.url,
        title = link.title,
        description = link.description,
        faviconUrl = link.faviconUrl,
        folderId = link.folderId,
        isFavorite = link.isFavorite,
        isRead = link.isRead,
        createdAt = link.createdAt,
        reminderAt = link.reminderAt,
        previewImageUrl = link.previewImageUrl,
        domain = link.domain,
        isPinned = link.isPinned,
        note = link.note,
        expiresAt = link.expiresAt,
        tags = link.tags.joinToString(","),
        inBin = link.inBin,
        deletedAt = link.deletedAt,
        preventScreenshot = link.preventScreenshot
    )

    private fun toFolder(entity: FolderEntity) = Folder(
        id = entity.id,
        name = entity.name,
        icon = entity.icon,
        color = entity.color,
        createdAt = entity.createdAt,
        isLocked = entity.isLocked,
        parentId = entity.parentId
    )
}
