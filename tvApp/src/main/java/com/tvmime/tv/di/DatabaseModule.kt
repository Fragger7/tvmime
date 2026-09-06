package com.tvmime.tv.di

import android.content.Context
import com.tvmime.db.AppDatabase
import com.tvmime.db.dao.CatalogSyncDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideCatalogSyncDao(database: AppDatabase): CatalogSyncDao {
        return database.catalogSyncDao()
    }
}
