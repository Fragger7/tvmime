package com.tvmime.sync

import com.tvmime.model.PortalConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Lightweight Firebase Authentication & Firestore REST client.
 * 
 * Bypasses heavyweight Google Play Services SDKs, allowing pure KMP REST execution
 * on any Android TV or Firestick device with minimal RAM overhead.
 */
class FirebaseSyncClient(
    private val httpClient: HttpClient = createClient(),
    private val apiKey: String = FIREBASE_API_KEY,
    private val projectId: String = FIREBASE_PROJECT_ID
) {
    companion object {
        const val FIREBASE_API_KEY = "AIzaSyDhPNLT3YUqW6I6KVIt5-Kbop9mlaSRufw"
        const val FIREBASE_PROJECT_ID = "tvmime-65909"

        private fun createClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    })
                }
                install(HttpTimeout) {
                    connectTimeoutMillis = 8000L
                    socketTimeoutMillis = 8000L
                    requestTimeoutMillis = 12000L
                }
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseSession> {
        return try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("email", email.trim())
                        put("password", pass)
                        put("returnSecureToken", true)
                    }.toString()
                )
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = response.bodyAsText()
                return Result.failure(Exception("Login failed: ${parseFirebaseError(errorBody)}"))
            }

            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val idToken = json["idToken"]?.jsonPrimitive?.content ?: ""
            val localId = json["localId"]?.jsonPrimitive?.content ?: ""
            val emailRes = json["email"]?.jsonPrimitive?.content ?: email

            Result.success(FirebaseSession(idToken = idToken, userId = localId, email = emailRes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPortals(session: FirebaseSession): Result<List<PortalConfig>> {
        return try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery"
            val queryBody = buildJsonObject {
                put("structuredQuery", buildJsonObject {
                    put("from", buildJsonArray {
                        add(buildJsonObject { put("collectionId", "user_portals") })
                    })
                    put("where", buildJsonObject {
                        put("fieldFilter", buildJsonObject {
                            put("field", buildJsonObject { put("fieldPath", "userId") })
                            put("op", "EQUAL")
                            put("value", buildJsonObject { put("stringValue", session.userId) })
                        })
                    })
                })
            }

            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${session.idToken}")
                setBody(queryBody.toString())
            }

            if (response.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Failed to fetch cloud portals: HTTP ${response.status.value}"))
            }

            val rawArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            val portals = mutableListOf<PortalConfig>()

            for (element in rawArray) {
                val docObj = element.jsonObject["document"]?.jsonObject ?: continue
                val docName = docObj["name"]?.jsonPrimitive?.content ?: ""
                val docId = docName.substringAfterLast("/")
                val fields = docObj["fields"]?.jsonObject ?: continue

                val name = fields["name"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Portal"
                val serverUrl = fields["serverUrl"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                val username = fields["username"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                val password = fields["password"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                val m3uUrl = fields["m3uUrl"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull
                val type = fields["type"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "xtream"
                val isActive = fields["isActive"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: true
                val syncLive = fields["syncLive"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: true
                val syncMovies = fields["syncMovies"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: true
                val syncSeries = fields["syncSeries"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: true
                val expiryDate = fields["expiryDate"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull

                if (serverUrl.isNotBlank() && username.isNotBlank()) {
                    portals.add(
                        PortalConfig(
                            id = docId,
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
                    )
                }
            }

            Result.success(portals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFirebaseError(jsonStr: String): String {
        return try {
            val json = Json.parseToJsonElement(jsonStr).jsonObject
            json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
        } catch (e: Exception) {
            "Authentication failed"
        }
    }
}

@Serializable
data class FirebaseSession(
    val idToken: String,
    val userId: String,
    val email: String
)
