package com.tvmime.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tvmime.model.PortalConfig

@Entity(tableName = "portals")
data class PortalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val m3uUrl: String? = null,
    val type: String = "xtream",
    val isActive: Boolean = true,
    val syncLive: Boolean = true,
    val syncMovies: Boolean = true,
    val syncSeries: Boolean = true,
    val expiryDate: String? = null,
    val lastSyncedAt: Long = 0L
) {
    fun toDomain(): PortalConfig = PortalConfig(
        id = id,
        name = name,
        serverUrl = serverUrl,
        username = username,
        password = password,
        m3uUrl = m3uUrl,
        type = type,
        isActive = isActive,
        syncLive = syncLive,
        syncMovies = syncMovies,
        syncSeries = syncSeries,
        expiryDate = expiryDate
    )

    companion object {
        fun fromDomain(config: PortalConfig, lastSyncedAt: Long = 0L): PortalEntity = PortalEntity(
            id = config.id.ifBlank { java.util.UUID.randomUUID().toString() },
            name = config.name,
            serverUrl = config.serverUrl,
            username = config.username,
            password = config.password,
            m3uUrl = config.m3uUrl,
            type = config.type,
            isActive = config.isActive,
            syncLive = config.syncLive,
            syncMovies = config.syncMovies,
            syncSeries = config.syncSeries,
            expiryDate = config.expiryDate,
            lastSyncedAt = lastSyncedAt
        )
    }
}
