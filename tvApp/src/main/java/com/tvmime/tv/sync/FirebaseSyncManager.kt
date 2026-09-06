package com.tvmime.tv.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SyncCredentials(
    val portalUrl: String,
    val username: String?,
    val password: String?
)

@Singleton
class FirebaseSyncManager @Inject constructor() {
    
    // We instantiate Firestore here. 
    // IMPORTANT: The app requires google-services.json to be present in tvApp/
    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Listens to a specific session code document in Firestore.
     * When the web portal writes the credentials, this Flow emits them and deletes the document.
     */
    fun listenForCredentials(sessionCode: String): Flow<SyncCredentials> = callbackFlow {
        val docRef = db.collection("tv_sessions").document(sessionCode)
        
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val portalUrl = snapshot.getString("portalUrl")
                val username = snapshot.getString("username")
                val password = snapshot.getString("password")

                if (!portalUrl.isNullOrBlank()) {
                    // We have the payload! 
                    trySend(SyncCredentials(portalUrl, username, password))
                    
                    // Security: Delete the document immediately after reading
                    docRef.delete()
                    
                    // Close the flow since we only need a one-time sync
                    close()
                }
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}
