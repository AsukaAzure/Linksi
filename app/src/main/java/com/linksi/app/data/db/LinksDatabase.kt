package com.linksi.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LinkEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LinksDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao
}
