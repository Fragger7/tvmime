with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

replacement = """            val portals = portalsResult.getOrNull() ?: emptyList()
            val activePortal = portals.firstOrNull { it.isActive && !it.m3uUrl.isNullOrBlank() }

            if (activePortal != null) {
                // We have a valid portal with an M3U! Trigger the ingestion engine.
                runCatching {
                    database.portalDao().insertOrUpdate(
                        com.tvmime.db.entity.PortalEntity.fromDomain(activePortal, System.currentTimeMillis())
                    )
                    syncManager.importPlaylist(activePortal.id, activePortal.m3uUrl!!)
                    onSuccess()
                }.onFailure {
                    onError("Failed to ingest playlist.")
                }
            } else {
                // Fallback to mock sync for MVP testing
                triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                onSuccess()
            }"""

old_logic = """            val portals = portalsResult.getOrNull() ?: emptyList()
            val activePortal = portals.firstOrNull { it.isActive && !it.m3uUrl.isNullOrBlank() }

            if (activePortal != null) {
                // We have a valid portal with an M3U! Trigger the ingestion engine.
                runCatching {
                    syncManager.importPlaylist(activePortal.id, activePortal.m3uUrl!!)
                    onSuccess()
                }.onFailure {
                    onError("Failed to ingest playlist.")
                }
            } else {
                onError("No active M3U portal found on this account.")
            }"""

content = content.replace(old_logic, replacement)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
