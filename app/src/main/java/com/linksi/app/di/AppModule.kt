package com.linksi.app.di

import android.content.Context
import androidx.room.Room
import com.linksi.app.data.db.FolderDao
import com.linksi.app.data.db.LinkDao
import com.linksi.app.data.db.LinksDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LinksDatabase =
        Room.databaseBuilder(context, LinksDatabase::class.java, "linksi_db")
            .addMigrations(LinksDatabase.MIGRATION_1_2, LinksDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideLinkDao(db: LinksDatabase): LinkDao = db.linkDao()

    @Provides
    fun provideFolderDao(db: LinksDatabase): FolderDao = db.folderDao()
}
