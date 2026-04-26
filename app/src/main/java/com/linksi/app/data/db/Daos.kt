package com.linksi.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {

    @Query("SELECT * FROM links ORDER BY createdAt DESC")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getLinksByFolder(folderId: Long): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE folderId IS NULL ORDER BY createdAt DESC")
    fun getUncategorizedLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadLinks(): Flow<List<LinkEntity>>

    @Query("""
        SELECT * FROM links 
        WHERE url LIKE '%' || :query || '%' 
        OR title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        OR domain LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchLinks(query: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE reminderAt IS NOT NULL AND reminderAt > :now ORDER BY reminderAt ASC")
    fun getLinksWithReminders(now: Long = System.currentTimeMillis()): Flow<List<LinkEntity>>

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

    @Query("SELECT COUNT(*) FROM links")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM links WHERE id = :id")
    suspend fun getLinkById(id: Long): LinkEntity?
}

@Dao
interface FolderDao {

    @Query("""
        SELECT f.*, COUNT(l.id) as link_count 
        FROM folders f 
        LEFT JOIN links l ON l.folderId = f.id 
        GROUP BY f.id
        ORDER BY f.createdAt ASC
    """)
    fun getAllFoldersWithCount(): Flow<List<FolderWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?
}
