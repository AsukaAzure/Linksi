package com.linksi.app.data.db

import androidx.room.*

@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String = "",
    val description: String = "",
    val faviconUrl: String = "",
    val folderId: Long? = null,
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val previewImageUrl: String = "",
    val domain: String = ""
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "folder",
    val color: String = "#6750A4",
    val createdAt: Long = System.currentTimeMillis()
)

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    @ColumnInfo(name = "link_count") val linkCount: Int = 0
)
