package com.tvmime.tv.ui.player

data class EnrichedMetadata(
    val title: String?,
    val synopsis: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseYear: Int?,
    val rating: String?,
    val genres: List<String>
)
