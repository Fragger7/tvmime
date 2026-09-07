package com.tvmime.sync

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OtaManager {
    suspend fun checkForUpdates(): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = HttpClient()
                val response = client.get("https://api.github.com/repos/Fragger7/tvmime/releases/latest")
                if (response.status == HttpStatusCode.OK) {
                    "Update Available!"
                } else {
                    "You are on the latest version."
                }
            } catch (e: Exception) {
                "Failed to check for updates."
            }
        }
    }
}
