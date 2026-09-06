package com.tvmime.tv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.emptyFlow
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MockDomainModule {

    private inline fun <reified T> createMock(): T {
        val handler = InvocationHandler { proxy, method, args ->
            val returnType = method.returnType
            when {
                returnType == kotlinx.coroutines.flow.Flow::class.java -> emptyFlow<Any>()
                returnType == List::class.java -> emptyList<Any>()
                returnType == Boolean::class.java -> false
                returnType == Int::class.java -> 0
                returnType == Long::class.java -> 0L
                returnType == String::class.java -> ""
                returnType == Unit::class.java -> Unit
                // For coroutine suspend functions that return nullable types or objects, this naive proxy might crash if invoked,
                // but our goal is just to satisfy Dagger Hilt at compile-time and boot the empty UI shells.
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
            handler
        ) as T
    }

    @Provides @Singleton fun provideCategoryRepository(): com.streamvault.domain.repository.CategoryRepository = createMock()
    @Provides @Singleton fun provideChannelRepository(): com.streamvault.domain.repository.ChannelRepository = createMock()
    @Provides @Singleton fun provideCombinedM3uRepository(): com.streamvault.domain.repository.CombinedM3uRepository = createMock()
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
    @Provides @Singleton fun provideProviderRepository(): com.streamvault.domain.repository.ProviderRepository = createMock()
    @Provides @Singleton fun provideProviderSnapshotRepository(): com.streamvault.domain.repository.ProviderSnapshotRepository = createMock()
    @Provides @Singleton fun provideSearchRepository(): com.streamvault.domain.repository.SearchRepository = createMock()
    @Provides @Singleton fun provideSeriesRepository(): com.streamvault.domain.repository.SeriesRepository = createMock()
    @Provides @Singleton fun provideSyncMetadataRepository(): com.streamvault.domain.repository.SyncMetadataRepository = createMock()
    @Provides @Singleton fun provideVodRepository(): com.streamvault.domain.repository.VodRepository = createMock()
    
    // Player Engine Mocks
    @Provides @Singleton fun provideMedia3PlayerEngine(): com.streamvault.player.Media3PlayerEngine = createMock()
    @Provides @Singleton fun provideAudioCompatibilityMemoryStore(): com.streamvault.player.AudioCompatibilityMemoryStore = createMock()
}
