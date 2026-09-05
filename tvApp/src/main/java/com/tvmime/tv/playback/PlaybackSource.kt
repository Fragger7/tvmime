package com.tvmime.tv.playback

data class PlaybackSource(
    val sourceId: String,
    val channelId: String,
    val channelName: String,
    val streamUrl: String,
    val headers: Map<String, String>,
    val connectionLimit: Int
)

object PlaybackRequestExtras {
    const val VOD_CONTENT = "com.tvmime.tv.extra.VOD_CONTENT"
}
