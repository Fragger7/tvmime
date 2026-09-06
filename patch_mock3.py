import re

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'r') as f:
    content = f.read()

player_engine_proxy = """
    // Player Engine Mocks
    @Provides @Singleton @com.streamvault.app.di.MainPlayerEngine fun provideMainPlayerEngine(engineController: com.tvmime.tv.player.EngineController): com.streamvault.player.PlayerEngine {
        return createMock { method, args ->
            when (method.name) {
                "prepare" -> {
                    val streamInfo = args?.get(0) as? com.streamvault.domain.model.StreamInfo
                    streamInfo?.url?.let { engineController.startLivePreview(it) }
                    Unit
                }
                "renewStreamUrl" -> {
                    val streamInfo = args?.get(0) as? com.streamvault.domain.model.StreamInfo
                    streamInfo?.url?.let { engineController.handoffToMainPlayer(it) }
                    Unit
                }
                "stop", "release" -> {
                    engineController.teardownAll()
                    Unit
                }
                "play" -> Unit
                "pause" -> Unit
                "getPlaybackState", "playbackState" -> kotlinx.coroutines.flow.MutableStateFlow(com.streamvault.player.PlaybackState.READY)
                "getIsPlaying", "isPlaying" -> kotlinx.coroutines.flow.MutableStateFlow(true)
                else -> null
            }
        }
    }
"""

content = re.sub(r'// Player Engine Mocks.*?@Provides @Singleton fun provideMedia3PlayerEngine.*?createMock\(\)', player_engine_proxy, content, flags=re.DOTALL)

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'w') as f:
    f.write(content)
