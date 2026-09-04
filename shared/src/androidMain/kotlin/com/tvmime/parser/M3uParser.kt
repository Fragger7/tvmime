package com.tvmime.parser

import com.tvmime.model.Channel
import com.tvmime.model.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {
    
    fun parseM3uStream(inputStream: InputStream): Flow<Channel> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line = reader.readLine()
        
        if (line == null || !line.trim().startsWith("#EXTM3U")) {
            // Not a valid M3U file
            reader.close()
            return@flow
        }

        var currentChannelName = ""
        var currentStreamIcon: String? = null
        var currentEpgChannelId: String? = null
        var currentGroupName = "Uncategorized"
        var streamIdCounter = 1

        while (true) {
            line = reader.readLine() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:")) {
                // Parse EXTINF metadata
                // Example: #EXTINF:-1 tvg-id="CNN" tvg-logo="http://logo.com/cnn.png" group-title="News",CNN HD
                
                val nameSegment = trimmed.substringAfterLast(",", "").trim()
                if (nameSegment.isNotBlank()) {
                    currentChannelName = nameSegment
                }

                // Extract tvg-id (EPG ID)
                val tvgIdMatch = Regex("tvg-id=\"([^\"]+)\"").find(trimmed)
                currentEpgChannelId = tvgIdMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

                // Extract tvg-logo
                val tvgLogoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(trimmed)
                currentStreamIcon = tvgLogoMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

                // Extract group-title (Category)
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmed)
                if (groupMatch != null) {
                    currentGroupName = groupMatch.groupValues[1].takeIf { it.isNotBlank() } ?: "Uncategorized"
                }

            } else if (!trimmed.startsWith("#")) {
                // This is the actual stream URL
                val url = trimmed
                
                // Determine StreamType heuristically from extension
                val type = when {
                    url.contains("/movie/") || url.endsWith(".mkv") || url.endsWith(".mp4") || url.endsWith(".avi") -> StreamType.MOVIE
                    url.contains("/series/") -> StreamType.SERIES
                    else -> StreamType.LIVE
                }

                if (currentChannelName.isBlank()) {
                    currentChannelName = "Channel $streamIdCounter"
                }
                
                emit(
                    Channel(
                        id = "m3u_${streamIdCounter}",
                        streamId = streamIdCounter,
                        num = streamIdCounter,
                        name = currentChannelName,
                        type = type,
                        streamIcon = currentStreamIcon,
                        epgChannelId = currentEpgChannelId,
                        categoryId = currentGroupName, // We will map groupName to categoryId in the repository
                        containerExtension = url.substringAfterLast(".", "ts"),
                        hasArchive = false,
                        archiveDuration = 0,
                        directSourceUrl = url
                    )
                )

                streamIdCounter++
                // Reset metadata for next entry
                currentChannelName = ""
                currentStreamIcon = null
                currentEpgChannelId = null
                currentGroupName = "Uncategorized"
            }
        }
        reader.close()
    }.flowOn(Dispatchers.IO)
}
