package com.tvmime.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tvmime.db.dao.CategoryDao
import com.tvmime.db.dao.ChannelDao
import com.tvmime.db.dao.EpgDao
import com.tvmime.db.dao.PortalDao
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.EpgProgramEntity
import com.tvmime.db.entity.PortalEntity

@Database(
    entities = [
        PortalEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portalDao(): PortalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tvmime.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
