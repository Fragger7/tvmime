import re

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'r') as f:
    content = f.read()

# Add the new AuxiliaryPlayerEngine provider right after the MainPlayerEngine provider
aux_engine_provider = """
    @Provides @Singleton @com.streamvault.app.di.AuxiliaryPlayerEngine fun provideAuxiliaryPlayerEngine(): com.streamvault.player.PlayerEngine {
        return createMock { method, _ ->
            when (method.name) {
                "getPlaybackState", "playbackState" -> kotlinx.coroutines.flow.MutableStateFlow(com.streamvault.player.PlaybackState.READY)
                "getIsPlaying", "isPlaying" -> kotlinx.coroutines.flow.MutableStateFlow(false)
                else -> null
            }
        }
    }
"""

content = content.replace("@Provides @Singleton fun provideAudioCompatibilityMemoryStore()", aux_engine_provider + "\n    @Provides @Singleton fun provideAudioCompatibilityMemoryStore()")

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'w') as f:
    f.write(content)
