with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

replacement = """    fun triggerMockSync(testM3uUrl: String) {
        viewModelScope.launch {
            // Fake portal for the UI
            database.portalDao().insertOrUpdate(
                com.tvmime.db.entity.PortalEntity(
                    id = activePortalId,
                    name = "Demo IPTV Provider",
                    serverUrl = "",
                    username = "",
                    password = "",
                    m3uUrl = testM3uUrl,
                    type = "m3u",
                    isActive = true,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
            // Fake categories for the UI
            database.categoryDao().insertCategories(listOf(
"""

content = content.replace("""    fun triggerMockSync(testM3uUrl: String) {
        viewModelScope.launch {
            // Fake categories for the UI
            database.categoryDao().insertCategories(listOf(""", replacement)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
