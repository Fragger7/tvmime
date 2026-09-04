package com.tvmime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tvmime.db.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE portalId = :portalId AND categoryId = :categoryId ORDER BY num ASC, name ASC")
    fun getChannelsByCategory(portalId: String, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId IN (:portalIds) AND categoryId = :categoryId ORDER BY num ASC, name ASC")
    fun getChannelsByCategoryForPortals(portalIds: List<String>, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId = :portalId AND type = :type ORDER BY num ASC, name ASC")
    fun getAllChannelsByType(portalId: String, type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId IN (:portalIds) AND type = :type ORDER BY num ASC, name ASC")
    fun getAllChannelsByTypeForPortals(portalIds: List<String>, type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId = :portalId ORDER BY num ASC, name ASC LIMIT 1")
    suspend fun getFirstChannel(portalId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE portalId IN (:portalIds) ORDER BY num ASC, name ASC LIMIT 1")
    suspend fun getFirstChannelForPortals(portalIds: List<String>): ChannelEntity?

    @Query("SELECT * FROM channels WHERE portalId = :portalId AND isFavorite = 1 ORDER BY num ASC, name ASC")
    fun getFavorites(portalId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId IN (:portalIds) AND isFavorite = 1 ORDER BY num ASC, name ASC")
    fun getFavoritesForPortals(portalIds: List<String>): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId = :portalId AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 100")
    fun searchChannels(portalId: String, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId IN (:portalIds) AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 100")
    fun searchChannelsForPortals(portalIds: List<String>, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId = :portalId AND lastWatchedEpoch > 0 ORDER BY lastWatchedEpoch DESC LIMIT :limit")
    fun getRecentlyWatched(portalId: String, limit: Int = 20): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE portalId = :portalId AND streamId = :streamId LIMIT 1")
    suspend fun getChannelByStreamId(portalId: String, streamId: Int): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels WHERE portalId = :portalId AND type = :type")
    suspend fun getChannelCount(portalId: String, type: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelsBatch(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedEpoch = :epoch WHERE id = :channelId")
    suspend fun updateLastWatched(channelId: String, epoch: Long)

    @Query("DELETE FROM channels WHERE portalId = :portalId AND type = :type")
    suspend fun deleteChannelsForPortal(portalId: String, type: String)

    @Query("DELETE FROM channels WHERE portalId = :portalId")
    suspend fun deleteAllForPortal(portalId: String)
}
