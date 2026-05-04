package com.linksi.app.data.repository

import com.linksi.app.data.db.*
import com.linksi.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRepository @Inject constructor(
    private val linkDao: LinkDao,
    private val folderDao: FolderDao
) {
    // ─── Links ───────────────────────────────────────────────
    fun getAllLinks(): Flow<List<Link>> =
        linkDao.getAllLinks().map { it.map(::toLink) }

    fun getLinksByFolder(folderId: Long): Flow<List<Link>> =
        linkDao.getLinksByFolder(folderId).map { it.map(::toLink) }

    fun getUncategorizedLinks(): Flow<List<Link>> =
        linkDao.getUncategorizedLinks().map { it.map(::toLink) }

    fun getFavoriteLinks(): Flow<List<Link>> =
        linkDao.getFavoriteLinks().map { it.map(::toLink) }

    fun getUnreadLinks(): Flow<List<Link>> =
        linkDao.getUnreadLinks().map { it.map(::toLink) }

    fun searchLinks(query: String): Flow<List<Link>> =
        linkDao.searchLinks(query).map { it.map(::toLink) }

    fun getLinksWithReminders(): Flow<List<Link>> =
        linkDao.getLinksWithReminders().map { it.map(::toLink) }

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

    suspend fun getLinkById(id: Long): Link? =
        linkDao.getLinkById(id)?.let(::toLink)

    suspend fun getTotalCount(): Int = linkDao.getTotalCount()

    // ─── Folders ─────────────────────────────────────────────
    fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getAllFoldersWithCount().map { list ->
            list.map { Folder(it.folder.id, it.folder.name, it.folder.icon, it.folder.color, it.folder.createdAt, it.linkCount) }
        }

    suspend fun insertFolder(folder: Folder): Long =
        folderDao.insertFolder(FolderEntity(folder.id, folder.name, folder.icon, folder.color, folder.createdAt))

    suspend fun updateFolder(folder: Folder) =
        folderDao.updateFolder(FolderEntity(folder.id, folder.name, folder.icon, folder.color, folder.createdAt))

    suspend fun deleteFolder(folder: Folder) =
        folderDao.deleteFolder(FolderEntity(folder.id, folder.name, folder.icon, folder.color, folder.createdAt))

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
        domain = entity.domain
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
        domain = link.domain
    )
}
