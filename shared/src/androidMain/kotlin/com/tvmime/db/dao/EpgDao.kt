package com.tvmime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tvmime.db.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE portalId = :portalId AND epgChannelId = :epgChannelId AND endEpoch >= :currentEpoch ORDER BY startEpoch ASC LIMIT :limit")
    fun getProgramsForChannel(portalId: String, epgChannelId: String, currentEpoch: Long, limit: Int = 10): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE portalId = :portalId AND epgChannelId = :epgChannelId AND startEpoch <= :currentEpoch AND endEpoch >= :currentEpoch LIMIT 1")
    suspend fun getCurrentProgram(portalId: String, epgChannelId: String, currentEpoch: Long): EpgProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramsBatch(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endEpoch < :cutoffEpoch")
    suspend fun purgeOldPrograms(cutoffEpoch: Long)

    @Query("DELETE FROM epg_programs WHERE portalId = :portalId")
    suspend fun deleteAllForPortal(portalId: String)
}
