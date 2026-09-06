package com.tvmime.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channel_import_stage",
    indices = [
        Index(value = ["portalId", "streamId"])
    ]
)
data class ChannelImportStageEntity(
    @PrimaryKey val id: String, // "${portalId}_${type}_${streamId}"
    val portalId: String,
    val streamId: Int,
    val num: Int = 0,
    val name: String,
    val type: String,
    val streamIcon: String? = null,
    val epgChannelId: String? = null,
    val categoryId: String,
    val containerExtension: String = "ts",
    val hasArchive: Boolean = false,
    val archiveDuration: Int = 0,
    val directSourceUrl: String = "",
    val syncFingerprint: String // Hash of properties to determine if an update is needed
)
