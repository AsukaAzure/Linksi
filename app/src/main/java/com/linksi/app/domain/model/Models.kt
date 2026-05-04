package com.linksi.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Link(
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
) : Parcelable

@Parcelize
data class Folder(
    val id: Long = 0,
    val name: String,
    val icon: String = "folder",
    val color: String = "#6750A4",
    val createdAt: Long = System.currentTimeMillis(),
    val linkCount: Int = 0
) : Parcelable

enum class SortOption {
    DATE_NEWEST, DATE_OLDEST, TITLE_AZ, TITLE_ZA, DOMAIN
}

enum class FilterOption {
    ALL, FAVORITES, UNREAD, READ, HAS_REMINDER
}
