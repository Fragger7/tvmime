package com.tvmime.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tvmime.model.EpgProgram

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["portalId", "epgChannelId", "startEpoch"]),
        Index(value = ["endEpoch"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey val id: String, // "${portalId}_${epgChannelId}_${startEpoch}"
    val portalId: String,
    val epgChannelId: String,
    val title: String,
    val description: String? = null,
    val startEpoch: Long,
    val endEpoch: Long
) {
    fun toDomain(): EpgProgram = EpgProgram(
        id = id,
        epgChannelId = epgChannelId,
        title = title,
        description = description,
        startEpoch = startEpoch,
        endEpoch = endEpoch
    )

    companion object {
        fun fromDomain(prog: EpgProgram, portalId: String): EpgProgramEntity = EpgProgramEntity(
            id = "${portalId}_${prog.epgChannelId}_${prog.startEpoch}",
            portalId = portalId,
            epgChannelId = prog.epgChannelId,
            title = prog.title,
            description = prog.description,
            startEpoch = prog.startEpoch,
            endEpoch = prog.endEpoch
        )
    }
}
