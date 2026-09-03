package com.tvmime.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tvmime.db.entity.PortalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortalDao {
    @Query("SELECT * FROM portals WHERE isActive = 1 LIMIT 1")
    fun getActivePortal(): Flow<PortalEntity?>

    @Query("SELECT * FROM portals WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePortalSync(): PortalEntity?

    @Query("SELECT * FROM portals ORDER BY name ASC")
    fun getAllPortals(): Flow<List<PortalEntity>>

    @Query("SELECT * FROM portals WHERE id = :id LIMIT 1")
    suspend fun getPortalById(id: String): PortalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(portal: PortalEntity)

    @Delete
    suspend fun deletePortal(portal: PortalEntity)

    @Query("UPDATE portals SET isActive = 0")
    suspend fun clearAllActive()

    @Transaction
    suspend fun setActivePortal(portalId: String) {
        clearAllActive()
        setActive(portalId)
    }

    @Query("UPDATE portals SET isActive = 1 WHERE id = :portalId")
    suspend fun setActive(portalId: String)

    @Query("UPDATE portals SET lastSyncedAt = :timestamp WHERE id = :portalId")
    suspend fun updateLastSynced(portalId: String, timestamp: Long)
}
