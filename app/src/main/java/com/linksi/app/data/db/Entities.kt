package com.linksi.app.data.db

import androidx.room.*

@Entity(
    tableName = "links",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
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
    val domain: String = "",
    val isPinned: Boolean = false,
    val note: String = "",
    val expiresAt: Long? = null,
    val tags: String = "",
    val inBin: Boolean = false,
    val deletedAt: Long? = null,
    val preventScreenshot: Boolean = false
)

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "folder",
    val color: String = "#6750A4",
    val createdAt: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false,
    val parentId: Long? = null
)

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    @ColumnInfo(name = "link_count") val linkCount: Int = 0,
    @ColumnInfo(name = "latest_images") val latestImages: String? = null
)

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey val url: String,
    val title: String = "",
    val description: String = "",
    val faviconUrl: String = "",
    val previewImageUrl: String = "",
    val domain: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
