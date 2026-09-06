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

    private inline fun <reified T> createMock(crossinline customHandler: (Method, Array<out Any>?) -> Any? = { _, _ -> null }): T {
        val handler = InvocationHandler { proxy, method, args ->
            val customResult = customHandler(method, args)
            if (customResult != null) return@InvocationHandler customResult

            val returnType = method.returnType
            when {
                returnType == kotlinx.coroutines.flow.Flow::class.java -> emptyFlow<Any>()
                returnType == List::class.java -> emptyList<Any>()
                returnType == Boolean::class.java -> false
                returnType == Int::class.java -> 0
                returnType == Long::class.java -> 0L
                returnType == String::class.java -> ""
                returnType == Unit::class.java -> Unit
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
                    // StreamVault calls getChannelsByCategory(providerId, categoryId)
                    // Unfortunately categoryId in StreamVault is a Long, but in TVMime it's a String.
                    // But wait, the categories we mapped above used `categoryId.hashCode().toLong()`.
                    // We can't reverse a hash easily. We should probably just return ALL channels for the provider, 
                    // or better yet, since it's a proxy hack, we just fetch ALL channels and filter by hash in memory!
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
    @Provides @Singleton fun provideEpgRepository(): com.streamvault.domain.repository.EpgRepository = createMock()
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
    @Provides @Singleton fun provideMedia3PlayerEngine(): com.streamvault.player.Media3PlayerEngine = createMock()
    @Provides @Singleton fun provideAudioCompatibilityMemoryStore(): com.streamvault.player.AudioCompatibilityMemoryStore = createMock()
    
    // Preferences Mock
    @Provides @Singleton fun providePreferencesRepository(): com.streamvault.data.preferences.PreferencesRepository {
        return createMock { method, _ ->
            when (method.name) {
                "getAppTopLevelDestinations", "appTopLevelDestinations" -> flowOf(listOf(com.streamvault.domain.model.AppTopLevelDestination.HOME))
                "getAppLandingDestination", "appLandingDestination" -> flowOf(com.streamvault.domain.model.AppLandingDestination.HOME)
                else -> null
            }
        }
    }
}
