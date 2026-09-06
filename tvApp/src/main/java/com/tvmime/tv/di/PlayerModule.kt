package com.tvmime.tv.di

import android.content.Context
import com.tvmime.tv.player.LivePreviewEngine
import com.tvmime.tv.player.MainPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideMainPlayer(@ApplicationContext context: Context): MainPlayer {
        return MainPlayer(context)
    }

    @Provides
    @Singleton
    fun provideLivePreviewEngine(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        mainPlayer: MainPlayer
    ): LivePreviewEngine {
        return LivePreviewEngine(
            context = context,
            okHttpClient = okHttpClient,
            mainPlayerIsActive = { mainPlayer.hasActiveStream() }
        )
    }
}
