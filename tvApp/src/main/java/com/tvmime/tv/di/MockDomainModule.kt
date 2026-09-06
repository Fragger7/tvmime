package com.tvmime.tv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Singleton
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.ActiveLiveSource

@Module
@InstallIn(SingletonComponent::class)
object MockDomainModule {

    private val memoryPrefs = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<Any>>()

    private inline fun <reified T> createMock(crossinline customHandler: (Method, Array<out Any>?) -> Any? = { _, _ -> null }): T {
        val handler = InvocationHandler { proxy, method, args ->
            val customResult = customHandler(method, args)
            if (customResult != null) return@InvocationHandler customResult

            val returnType = method.returnType
            when {
                returnType == kotlinx.coroutines.flow.Flow::class.java -> {
                    val propertyName = method.name.removePrefix("get").replaceFirstChar { it.lowercase() }
                    // Default values for common preference types to avoid cast exceptions
                    val defaultVal: Any = when {
                        method.name.contains("Timeout") || method.name.contains("Height") -> 0
                        method.name.contains("Speed") || method.name.contains("Scale") -> 1.0f
                        method.name.contains("Language") || method.name.contains("Endpoint") -> ""
                        else -> false
                    }
                    val flow = memoryPrefs.getOrPut(propertyName) { kotlinx.coroutines.flow.MutableStateFlow(defaultVal) }
                    flow
                }
                returnType == List::class.java -> emptyList<Any>()
                returnType == Boolean::class.java -> false
                returnType == Int::class.java -> 0
                returnType == Long::class.java -> 0L
                returnType == String::class.java -> ""
                returnType == Unit::class.java -> {
                    if (method.name.startsWith("set")) {
                        val propertyName = method.name.removePrefix("set").replaceFirstChar { it.lowercase() }
                        args?.firstOrNull()?.let { value ->
                            (memoryPrefs[propertyName] as? kotlinx.coroutines.flow.MutableStateFlow<Any>)?.value = value
                        }
                    }
                    Unit
                }
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
            handler
        ) as T
    }

    @Provides @Singleton fun provideProviderRepository(): com.streamvault.domain.repository.ProviderRepository {
        return createMock { method, _ ->
            when (method.name) {
                "getActiveProvider" -> flowOf(LegacyProvider(id = 1L, name = "TVMime Cloud", type = ProviderType.M3U, serverUrl = "https://tvmime.com"))
                else -> null
            }
        }
    }

    @Provides @Singleton fun provideCombinedM3uRepository(): com.streamvault.domain.repository.CombinedM3uRepository {
        return createMock { method, _ ->
            when (method.name) {
                "getActiveLiveSource" -> flowOf(ActiveLiveSource.ProviderSource(providerId = 1L))
                else -> null
            }
        }
    }

    @Provides @Singleton fun provideCategoryRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.CategoryRepository {
        return createMock { method, args ->
            when (method.name) {
                "getCategories" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    kotlinx.coroutines.flow.map(database.categoryDao().getCategories(providerId.toString(), "LIVE")) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Category(
                                id = entity.categoryId.hashCode().toLong(),
                                roomId = entity.categoryId.hashCode().toLong(),
                                name = entity.categoryName,
                                type = com.streamvault.domain.model.ContentType.LIVE,
                                count = 10,
                                providerOrder = entity.sortOrder
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }

    @Provides @Singleton fun provideChannelRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.ChannelRepository {
        return createMock { method, args ->
            when (method.name) {
                "getChannelsByCategory" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    val catHash = args?.get(1) as? Long ?: 0L
                    kotlinx.coroutines.flow.map(database.channelDao().getAllChannelsByType(providerId.toString(), "LIVE")) { entities ->
                        entities.filter { it.categoryId.hashCode().toLong() == catHash }.map { entity ->
                            com.streamvault.domain.model.Channel(
                                id = entity.id.hashCode().toLong(),
                                name = entity.name,
                                streamUrl = entity.directSourceUrl,
                                categoryId = catHash,
                                categoryName = "Live TV",
                                providerId = providerId,
                                number = entity.num
                            )
                        }
                    }
                }
                "getChannels" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    kotlinx.coroutines.flow.map(database.channelDao().getAllChannelsByType(providerId.toString(), "LIVE")) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Channel(
                                id = entity.id.hashCode().toLong(),
                                name = entity.name,
                                streamUrl = entity.directSourceUrl,
                                providerId = providerId,
                                number = entity.num
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }

    @Provides @Singleton fun provideDownloadManager(): com.streamvault.domain.repository.DownloadManager = createMock()
    @Provides @Singleton fun provideEpgRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.EpgRepository {
        return createMock { method, args ->
            when (method.name) {
                "getProgramsForChannel" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    val channelId = args?.get(1) as? String ?: ""
                    val startTime = args?.get(2) as? Long ?: 0L
                    kotlinx.coroutines.flow.map(database.epgDao().getProgramsForChannel(providerId.toString(), channelId, startTime)) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Program(
                                id = entity.id.hashCode().toLong(),
                                channelId = entity.epgChannelId,
                                title = entity.title,
                                description = entity.description ?: "",
                                startTime = entity.startEpoch,
                                endTime = entity.endEpoch,
                                providerId = providerId
                            )
                        }
                    }
                }
                "getNowPlaying" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    val channelId = args?.get(1) as? String ?: ""
                    kotlinx.coroutines.flow.map(database.epgDao().getProgramsForChannel(providerId.toString(), channelId, System.currentTimeMillis(), 1)) { entities ->
                        entities.firstOrNull()?.let { entity ->
                            com.streamvault.domain.model.Program(
                                id = entity.id.hashCode().toLong(),
                                channelId = entity.epgChannelId,
                                title = entity.title,
                                description = entity.description ?: "",
                                startTime = entity.startEpoch,
                                endTime = entity.endEpoch,
                                providerId = providerId,
                                isNowPlaying = true
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }
    @Provides @Singleton fun provideEpgSourceRepository(): com.streamvault.domain.repository.EpgSourceRepository = createMock()
    @Provides @Singleton fun provideExternalRatingsRepository(): com.streamvault.domain.repository.ExternalRatingsRepository = createMock()
    @Provides @Singleton fun provideExternalSubtitleRepository(): com.streamvault.domain.repository.ExternalSubtitleRepository = createMock()
    @Provides @Singleton fun provideFavoriteRepository(): com.streamvault.domain.repository.FavoriteRepository = createMock()
    @Provides @Singleton fun provideM3uClassificationRepository(): com.streamvault.domain.repository.M3uClassificationRepository = createMock()
    @Provides @Singleton fun provideMovieRepository(): com.streamvault.domain.repository.MovieRepository = createMock()
    @Provides @Singleton fun providePlaybackCompatibilityRepository(): com.streamvault.domain.repository.PlaybackCompatibilityRepository = createMock()
    @Provides @Singleton fun providePlaybackHistoryRepository(): com.streamvault.domain.repository.PlaybackHistoryRepository = createMock()
    @Provides @Singleton fun provideProviderSnapshotRepository(): com.streamvault.domain.repository.ProviderSnapshotRepository = createMock()
    @Provides @Singleton fun provideSearchRepository(): com.streamvault.domain.repository.SearchRepository = createMock()
    @Provides @Singleton fun provideSeriesRepository(): com.streamvault.domain.repository.SeriesRepository = createMock()
    @Provides @Singleton fun provideSyncMetadataRepository(): com.streamvault.domain.repository.SyncMetadataRepository = createMock()
    @Provides @Singleton fun provideVodRepository(): com.streamvault.domain.repository.VodRepository = createMock()
    
    
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

    @Provides @Singleton fun provideAudioCompatibilityMemoryStore(): com.streamvault.player.AudioCompatibilityMemoryStore = createMock()
    
    // Preferences Mock
    @Provides @Singleton fun providePreferencesRepository(): com.streamvault.data.preferences.PreferencesRepository {
        return createMock { method, _ ->
            when (method.name) {
                "getAppTopLevelDestinations", "appTopLevelDestinations" -> flowOf(listOf(com.streamvault.domain.model.AppTopLevelDestination.HOME))
                "getAppLandingDestination", "appLandingDestination" -> flowOf(com.streamvault.domain.model.AppLandingDestination.HOME)
                "getShowFavoritesCategory", "showFavoritesCategory" -> flowOf(true)
                else -> null
            }
        }
    }
}
