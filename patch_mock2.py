import re

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'r') as f:
    content = f.read()

# Add in-memory state map for PreferencesRepository
state_map = """
    // In-memory state for UI interactions
    private val memoryPrefs = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<Any>>()
    private fun <T : Any> getPrefFlow(key: String, default: T): kotlinx.coroutines.flow.MutableStateFlow<T> {
        return memoryPrefs.getOrPut(key) { kotlinx.coroutines.flow.MutableStateFlow(default) } as kotlinx.coroutines.flow.MutableStateFlow<T>
    }
"""

content = content.replace("object MockDomainModule {", "object MockDomainModule {" + state_map)

# Replace providePreferencesRepository
proxy_pref = """
    @Provides @Singleton fun providePreferencesRepository(): com.streamvault.data.preferences.PreferencesRepository {
        return createMock { method, args ->
            val name = method.name
            when {
                name == "getAppTopLevelDestinations" || name == "getAppTopLevelDestinations" -> flowOf(listOf(com.streamvault.domain.model.AppTopLevelDestination.HOME))
                name == "getAppLandingDestination" || name == "getAppLandingDestination" -> flowOf(com.streamvault.domain.model.AppLandingDestination.HOME)
                name.startsWith("get") || name.startsWith("is") -> {
                    val propertyName = name.removePrefix("get").replaceFirstChar { it.lowercase() }
                    getPrefFlow(propertyName, false) // Generic false or 0 or whatever, UI will cast safely due to erasure? 
                    // Actually type erasure means Flow<Any> works if we provide the right type. Let's return flowOf(false) statically to avoid ClassCastExceptions.
                }
                name.startsWith("set") -> {
                    // Update flow if it exists
                    val propertyName = name.removePrefix("set").replaceFirstChar { it.lowercase() }
                    args?.firstOrNull()?.let { value ->
                        (memoryPrefs[propertyName] as? kotlinx.coroutines.flow.MutableStateFlow<Any>)?.value = value
                    }
                    Unit
                }
                else -> null
            }
        }
    }
"""
content = re.sub(r'@Provides @Singleton fun providePreferencesRepository\(\).*?\}\s*\}', proxy_pref, content, flags=re.DOTALL)

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'w') as f:
    f.write(content)
