with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

replacement = """    private val prefs = application.getSharedPreferences("tvmime_prefs", Context.MODE_PRIVATE)

    val isUserLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)

    fun logout(onComplete: () -> Unit) {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        viewModelScope.launch {
            database.clearAllTables() // Clear the local cache
            onComplete()
        }
    }

    private val firebaseClient = com.tvmime.sync.FirebaseSyncClient()"""

content = content.replace("    private val firebaseClient = com.tvmime.sync.FirebaseSyncClient()", replacement)

# Now inject saving the session in onSuccess
success_replace = """                    triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    onSuccess()"""
content = content.replace("""                    triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                    onSuccess()""", success_replace)

success_email = """                runCatching {
                    database.portalDao().insertOrUpdate(
                        com.tvmime.db.entity.PortalEntity.fromDomain(activePortal, System.currentTimeMillis())
                    )
                    syncManager.importPlaylist(activePortal.id, activePortal.m3uUrl!!)
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    onSuccess()"""
content = content.replace("""                runCatching {
                    database.portalDao().insertOrUpdate(
                        com.tvmime.db.entity.PortalEntity.fromDomain(activePortal, System.currentTimeMillis())
                    )
                    syncManager.importPlaylist(activePortal.id, activePortal.m3uUrl!!)
                    onSuccess()""", success_email)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
