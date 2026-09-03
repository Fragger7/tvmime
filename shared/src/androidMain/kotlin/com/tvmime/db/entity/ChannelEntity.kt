package com.tvmime.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tvmime.model.Channel
import com.tvmime.model.StreamType

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["portalId", "categoryId"]),
        Index(value = ["portalId", "type"]),
        Index(value = ["portalId", "isFavorite"]),
        Index(value = ["portalId", "streamId"]),
        Index(value = ["name"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String, // "${portalId}_${type}_${streamId}"
    val portalId: String,
    val streamId: Int,
    val num: Int = 0,
    val name: String,
    val type: String, // "LIVE", "MOVIE", "SERIES"
    val streamIcon: String? = null,
    val epgChannelId: String? = null,
    val categoryId: String,
    val containerExtension: String = "ts",
    val directSourceUrl: String = "",
    val isFavorite: Boolean = false,
    val lastWatchedEpoch: Long = 0L
) {
    fun toDomain(): Channel = Channel(
        id = id,
        streamId = streamId,
        num = num,
        name = name,
        type = runCatching { StreamType.valueOf(type) }.getOrDefault(StreamType.LIVE),
        streamIcon = streamIcon,
        epgChannelId = epgChannelId,
        categoryId = categoryId,
        containerExtension = containerExtension,
        directSourceUrl = directSourceUrl
    )

    companion object {
        fun fromDomain(ch: Channel, portalId: String, isFavorite: Boolean = false): ChannelEntity = ChannelEntity(
            id = "${portalId}_${ch.type.name}_${ch.streamId}",
            portalId = portalId,
            streamId = ch.streamId,
            num = ch.num,
            name = ch.name,
            type = ch.type.name,
            streamIcon = ch.streamIcon,
            epgChannelId = ch.epgChannelId,
            categoryId = ch.categoryId,
            containerExtension = ch.containerExtension,
            directSourceUrl = ch.directSourceUrl,
            isFavorite = isFavorite
        )
    }
}
