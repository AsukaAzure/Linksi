package com.linksi.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {

    @Query("""
        SELECT l.* FROM links l 
        LEFT JOIN folders f ON l.folderId = f.id 
        WHERE l.inBin = 0 AND (:isFolderLockEnabled = 0 OR f.isLocked IS NULL OR f.isLocked = 0) 
        ORDER BY l.createdAt DESC
    """)
    fun getAllLinks(isFolderLockEnabled: Boolean): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE inBin = 0 AND folderId = :folderId ORDER BY createdAt DESC")
    fun getLinksByFolder(folderId: Long): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE inBin = 0 AND folderId IS NULL ORDER BY createdAt DESC")
    fun getUncategorizedLinks(): Flow<List<LinkEntity>>

    @Query("""
        SELECT l.* FROM links l 
        LEFT JOIN folders f ON l.folderId = f.id 
        WHERE l.inBin = 0 AND l.isFavorite = 1 AND (:isFolderLockEnabled = 0 OR f.isLocked IS NULL OR f.isLocked = 0) 
        ORDER BY l.createdAt DESC
    """)
    fun getFavoriteLinks(isFolderLockEnabled: Boolean): Flow<List<LinkEntity>>

    @Query("""
        SELECT l.* FROM links l 
        LEFT JOIN folders f ON l.folderId = f.id 
        WHERE l.inBin = 0 AND l.isRead = 0 AND (:isFolderLockEnabled = 0 OR f.isLocked IS NULL OR f.isLocked = 0) 
        ORDER BY l.createdAt DESC
    """)
    fun getUnreadLinks(isFolderLockEnabled: Boolean): Flow<List<LinkEntity>>

    @Query(
        """
        SELECT l.* FROM links l 
        LEFT JOIN folders f ON l.folderId = f.id 
        WHERE l.inBin = 0 AND (:isFolderLockEnabled = 0 OR f.isLocked IS NULL OR f.isLocked = 0) AND (
            url LIKE '%' || :query || '%' 
            OR title LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%'
            OR tags LIKE '%' || :query || '%'
            OR domain LIKE '%' || :query || '%'
        )
        ORDER BY l.createdAt DESC
    """
    )
    fun searchLinks(query: String, isFolderLockEnabled: Boolean): Flow<List<LinkEntity>>

    @Query("""
        SELECT l.* FROM links l 
        LEFT JOIN folders f ON l.folderId = f.id 
        WHERE l.inBin = 0 AND (:isFolderLockEnabled = 0 OR f.isLocked IS NULL OR f.isLocked = 0) 
        AND reminderAt IS NOT NULL AND reminderAt > :now 
        ORDER BY reminderAt ASC
    """)
    fun getLinksWithReminders(isFolderLockEnabled: Boolean, now: Long = System.currentTimeMillis()): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: LinkEntity): Long

    @Update
    suspend fun updateLink(link: LinkEntity)

    @Delete
    suspend fun deleteLink(link: LinkEntity)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun deleteLinkById(id: Long)

    @Query("UPDATE links SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE links SET isRead = :isRead WHERE id = :id")
    suspend fun markAsRead(id: Long, isRead: Boolean)

    @Query("UPDATE links SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("UPDATE links SET inBin = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToBin(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE links SET inBin = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromBin(id: Long)

    @Query("SELECT * FROM links WHERE inBin = 1 ORDER BY deletedAt DESC")
    fun getLinksInBin(): Flow<List<LinkEntity>>

    @Query("DELETE FROM links WHERE inBin = 1 AND deletedAt < :threshold")
    suspend fun cleanBin(threshold: Long)

    @Query("SELECT COUNT(*) FROM links WHERE inBin = 0")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM links WHERE id = :id")
    suspend fun getLinkById(id: Long): LinkEntity?

    @Query("SELECT * FROM links WHERE url = :url LIMIT 1")
    suspend fun getLinkByUrl(url: String): LinkEntity?

    @Query("UPDATE links SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE links SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String)

    @Query("UPDATE links SET expiresAt = :expiresAt WHERE id = :id")
    suspend fun setExpiry(id: Long, expiresAt: Long?)

    @Query("SELECT * FROM links WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun getExpiredLinks(now: Long = System.currentTimeMillis()): List<LinkEntity>

    @Query("SELECT tags FROM links WHERE tags != ''")
    suspend fun getAllTagStrings(): List<String>
}

@Dao
interface FolderDao {

    @Query(
        """
        SELECT f.*, COUNT(l.id) as link_count 
        FROM folders f 
        LEFT JOIN links l ON l.folderId = f.id AND l.inBin = 0
        WHERE f.parentId IS :parentId
        GROUP BY f.id
        ORDER BY f.createdAt ASC
    """
    )
    fun getFoldersByParentWithCount(parentId: Long?): Flow<List<FolderWithCount>>

    @Query(
        """
        SELECT f.*, COUNT(l.id) as link_count 
        FROM folders f 
        LEFT JOIN links l ON l.folderId = f.id AND l.inBin = 0
        GROUP BY f.id
        ORDER BY f.createdAt ASC
    """
    )
    fun getAllFoldersWithCount(): Flow<List<FolderWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Query("UPDATE folders SET isLocked = :isLocked WHERE id = :id")
    suspend fun toggleLock(id: Long, isLocked: Boolean)
}
