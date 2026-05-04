package com.linksi.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LinkEntity::class, FolderEntity::class],
    version = 3,  // bumped
    exportSchema = false
)
abstract class LinksDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE folders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL DEFAULT 'folder',
                        color TEXT NOT NULL DEFAULT '#6750A4',
                        createdAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    INSERT INTO folders_new (id, name, icon, color, createdAt)
                    SELECT id, name, 'folder', color, createdAt FROM folders
                """)
                database.execSQL("DROP TABLE folders")
                database.execSQL("ALTER TABLE folders_new RENAME TO folders")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE links_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        faviconUrl TEXT NOT NULL DEFAULT '',
                        folderId INTEGER,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        reminderAt INTEGER,
                        previewImageUrl TEXT NOT NULL DEFAULT '',
                        domain TEXT NOT NULL DEFAULT ''
                    )
                """)
                database.execSQL("""
                    INSERT INTO links_new
                    SELECT id, url, title, description, faviconUrl, folderId,
                           isFavorite, isRead, createdAt, reminderAt, previewImageUrl, domain
                    FROM links
                """)
                database.execSQL("DROP TABLE links")
                database.execSQL("ALTER TABLE links_new RENAME TO links")
            }
        }
    }
}
