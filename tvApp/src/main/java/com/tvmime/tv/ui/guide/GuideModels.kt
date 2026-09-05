package com.tvmime.tv.ui.guide

data class GuideTimelineProgramme(
    val id: String,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val categories: List<String>,
    val startEpochMillis: Long,
    val stopEpochMillis: Long,
)

data class GuideTimelineChannel(
    val sourceId: String,
    val sourceName: String,
    val sourcePriority: Int,
    val id: String,
    val name: String,
    val groupTitle: String?,
    val logoUrl: String?,
    val playlistOrder: Int,
    val catchupType: String? = null,
    val catchupSource: String? = null,
    val catchupDays: Int? = null,
    val programmes: List<GuideTimelineProgramme>,
    val organizationGroupKey: String = groupTitle ?: "",
    val legacyPosition: Long? = null,
)

data class GuideSelection(
    val channel: GuideTimelineChannel,
    val programme: GuideTimelineProgramme?,
)
