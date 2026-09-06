package com.tvmime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogSyncDao {
    
    @Query("DELETE FROM channel_import_stage WHERE portalId = :portalId")
    suspend fun clearStagingTable(portalId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stageChannelBatch(channels: List<com.tvmime.db.entity.ChannelImportStageEntity>)

    // 1. Insert new channels that don't exist in the main table
    @Query("""
        INSERT INTO channels (
            id, portalId, streamId, num, name, type, streamIcon, 
            epgChannelId, categoryId, containerExtension, hasArchive, 
            archiveDuration, directSourceUrl, isFavorite, lastWatchedEpoch
        )
        SELECT 
            s.id, s.portalId, s.streamId, s.num, s.name, s.type, s.streamIcon, 
            s.epgChannelId, s.categoryId, s.containerExtension, s.hasArchive, 
            s.archiveDuration, s.directSourceUrl, 0, 0
        FROM channel_import_stage s
        WHERE s.portalId = :portalId 
        AND NOT EXISTS (SELECT 1 FROM channels c WHERE c.id = s.id)
    """)
    suspend fun insertMissingChannelsFromStage(portalId: String)

    // 2. Update existing channels where properties have changed
    // In a real app we'd compare the syncFingerprint, but for this surgery we'll just update based on streamId
    @Query("""
        UPDATE channels 
        SET 
            name = (SELECT name FROM channel_import_stage WHERE id = channels.id),
            streamIcon = (SELECT streamIcon FROM channel_import_stage WHERE id = channels.id),
            epgChannelId = (SELECT epgChannelId FROM channel_import_stage WHERE id = channels.id),
            categoryId = (SELECT categoryId FROM channel_import_stage WHERE id = channels.id),
            containerExtension = (SELECT containerExtension FROM channel_import_stage WHERE id = channels.id),
            directSourceUrl = (SELECT directSourceUrl FROM channel_import_stage WHERE id = channels.id)
        WHERE portalId = :portalId 
        AND EXISTS (SELECT 1 FROM channel_import_stage WHERE id = channels.id)
    """)
    suspend fun updateChangedChannelsFromStage(portalId: String)

    // 3. Delete stale channels that are no longer in the provider's list
    @Query("""
        DELETE FROM channels 
        WHERE portalId = :portalId 
        AND NOT EXISTS (SELECT 1 FROM channel_import_stage WHERE id = channels.id)
    """)
    suspend fun deleteStaleChannelsForStage(portalId: String)
}
