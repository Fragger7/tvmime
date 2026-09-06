package com.tvmime.tv.sync

import com.tvmime.db.AppDatabase
import com.tvmime.db.dao.CatalogSyncDao
import com.tvmime.db.entity.ChannelImportStageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManagerM3uImporter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val catalogSyncDao: CatalogSyncDao,
    private val appDatabase: AppDatabase
) {
    private val batchSize = 1000

    suspend fun importPlaylist(portalId: String, m3uUrl: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(m3uUrl).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Failed to fetch M3U: ${response.code}")

        val inputStream = response.body?.byteStream() ?: throw Exception("Empty body")

        // 1. Clear staging table for this portal
        catalogSyncDao.clearStagingTable(portalId)

        val channelBatch = ArrayList<ChannelImportStageEntity>(batchSize)

        // 2. Stream parse the 50MB M3U line-by-line (Zero-Allocation Buffer)
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line = reader.readLine()
            var currentExtInf = ""

            while (line != null) {
                if (line.startsWith("#EXTINF:")) {
                    currentExtInf = line
                } else if (line.isNotBlank() && !line.startsWith("#")) {
                    val streamUrl = line
                    val parsedChannel = parseM3uEntry(portalId, currentExtInf, streamUrl)
                    
                    if (parsedChannel != null) {
                        channelBatch.add(parsedChannel)
                    }

                    // Flush chunk to SQLite
                    if (channelBatch.size >= batchSize) {
                        catalogSyncDao.stageChannelBatch(channelBatch)
                        channelBatch.clear()
                    }
                    currentExtInf = "" // reset
                }
                line = reader.readLine()
            }

            // Flush remaining
            if (channelBatch.isNotEmpty()) {
                catalogSyncDao.stageChannelBatch(channelBatch)
                channelBatch.clear()
            }
        }

        // 3. Final Pure SQL Reconciliation Transaction
        appDatabase.runInTransaction {
            kotlinx.coroutines.runBlocking {
                catalogSyncDao.insertMissingChannelsFromStage(portalId)
                catalogSyncDao.updateChangedChannelsFromStage(portalId)
                catalogSyncDao.deleteStaleChannelsForStage(portalId)
            }
        }
    }

    private fun parseM3uEntry(portalId: String, extInf: String, url: String): ChannelImportStageEntity? {
        if (extInf.isBlank()) return null
        // Basic naive parser for demonstration (StreamVault uses Regex)
        val name = extInf.substringAfterLast(",").trim()
        val streamId = url.hashCode() // Mocking streamId generation for raw M3Us

        return ChannelImportStageEntity(
            id = "${portalId}_LIVE_${streamId}",
            portalId = portalId,
            streamId = streamId,
            name = name,
            type = "LIVE",
            categoryId = "all", // Simplified
            directSourceUrl = url,
            syncFingerprint = name.hashCode().toString()
        )
    }
}
