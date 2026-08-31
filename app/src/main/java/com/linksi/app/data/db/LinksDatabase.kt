package com.linksi.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.Migration_1_2

@Database(
    entities = [LinkEntity::class, FolderEntity::class, MetadataCacheEntity::class],
    version = 11,
    exportSchema = false
)
abstract class LinksDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao
    abstract fun metadataCacheDao(): MetadataCacheDao

    companion object {
        val MIGRATION_10_11 = object : Migration(10, 11) {
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
                        domain TEXT NOT NULL DEFAULT '',
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        expiresAt INTEGER,
                        tags TEXT NOT NULL DEFAULT '',
                        inBin INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        preventScreenshot INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                database.execSQL("""
                    INSERT INTO links_new (id, url, title, description, faviconUrl, folderId, isFavorite, isRead, createdAt, reminderAt, previewImageUrl, domain, isPinned, note, expiresAt, tags, inBin, deletedAt, preventScreenshot)
                    SELECT id, url, title, description, faviconUrl, folderId, isFavorite, isRead, createdAt, reminderAt, previewImageUrl, domain, isPinned, note, expiresAt, tags, inBin, deletedAt, preventScreenshot FROM links
                """)
                database.execSQL("DROP TABLE links")
                database.execSQL("ALTER TABLE links_new RENAME TO links")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_links_folderId ON links(folderId)")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_cache (
                        url TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        faviconUrl TEXT NOT NULL,
                        previewImageUrl TEXT NOT NULL,
                        domain TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with parentId and proper constraints
                database.execSQL("""
                    CREATE TABLE folders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL DEFAULT 'folder',
                        color TEXT NOT NULL DEFAULT '#6750A4',
                        createdAt INTEGER NOT NULL,
                        isLocked INTEGER NOT NULL DEFAULT 0,
                        parentId INTEGER,
                        FOREIGN KEY(parentId) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                
                // Copy existing data
                database.execSQL("""
                    INSERT INTO folders_new (id, name, icon, color, createdAt, isLocked, parentId)
                    SELECT id, name, icon, color, createdAt, isLocked, NULL FROM folders
                """)
                
                // Swap tables
                database.execSQL("DROP TABLE folders")
                database.execSQL("ALTER TABLE folders_new RENAME TO folders")
                
                // Create index
                database.execSQL("CREATE INDEX IF NOT EXISTS index_folders_parentId ON folders(parentId)")
            }
        }
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
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE links ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE links ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE links ADD COLUMN expiresAt INTEGER")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE links ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE folders ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE links ADD COLUMN inBin INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE links ADD COLUMN deletedAt INTEGER")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE links ADD COLUMN preventScreenshot INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
