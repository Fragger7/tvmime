package com.tvmime.tv.playback

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.tvmime.db.AppDatabase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class TvMimePlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var mediaSourceFactory: DefaultMediaSourceFactory
    private val activeBufferProfile = PlaybackBufferProfile.LOW_LATENCY
    private var shuttingDown = false
    @Volatile private var activeSource: PlaybackSource? = null

    override fun onCreate() {
        super.onCreate()
        val upstreamFactory = DefaultDataSource.Factory(this)
        val resolvingFactory = ResolvingDataSource.Factory(upstreamFactory, ::resolveDataSpec)
        mediaSourceFactory = DefaultMediaSourceFactory(resolvingFactory)
        
        player = createPlayer(activeBufferProfile)
        mediaSession = MediaSession.Builder(this, player)
            .setId("TvMimePlaybackSession")
            .setCallback(SessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        player.stop()
        player.clearMediaItems()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        shuttingDown = true
        activeSource = null
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val source = activeSource
            ?.takeIf { dataSpec.uri.scheme == "tvmime" }
            ?: throw IOException("Playback source is no longer available")
        return dataSpec
            .withUri(Uri.parse(source.streamUrl))
            .withAdditionalHeaders(source.headers)
    }

    private fun createPlayer(bufferProfile: PlaybackBufferProfile): ExoPlayer {
        val builder = ExoPlayer.Builder(this)
            .setRenderersFactory(
                DefaultRenderersFactory(this)
                    .setEnableDecoderFallback(true),
            )
            .setMediaSourceFactory(mediaSourceFactory)
        PlaybackBufferPolicy.loadControl(bufferProfile)?.let(builder::setLoadControl)
        return builder.build().also { exoPlayer ->
            exoPlayer.videoChangeFrameRateStrategy = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
            exoPlayer.addListener(
                object : Player.Listener {
                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        if (exoPlayer.mediaItemCount == 0) {
                            activeSource = null
                        }
                    }
                },
            )
        }
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            if (controller.packageName == packageName || controller.isTrusted || session.isMediaNotificationController(controller)) {
                MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            } else {
                MediaSession.ConnectionResult.reject()
            }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val result = SettableFuture.create<List<MediaItem>>()
            val request = mediaItems.firstOrNull()
            if (request == null || request.mediaId.isBlank()) {
                result.setException(IllegalArgumentException("A channel ID is required"))
                return result
            }
            
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    activeSource = null
                    val db = AppDatabase.getInstance(this@TvMimePlaybackService)
                    val channelId = request.mediaId
                    val channel = db.channelDao().getChannelById(channelId) ?: throw IOException("Channel not found in database")
                    val portal = db.portalDao().getPortalById(channel.portalId) ?: throw IOException("Portal not found")
                    
                    val source = PlaybackSource(
                        sourceId = channel.portalId,
                        channelId = channel.id,
                        channelName = channel.name,
                        streamUrl = "${portal.url}/live/${portal.username}/${portal.password}/${channel.streamId}.${channel.containerExtension}",
                        headers = mapOf("User-Agent" to "IPTVSmartersPro/1.1.1"),
                        connectionLimit = 1
                    )
                    
                    activeSource = source
                    val newItem = request.buildUpon()
                        .setUri(Uri.parse("tvmime://channel/${channel.id}"))
                        .build()
                    listOf(newItem)
                }.onSuccess {
                    result.set(it)
                }.onFailure {
                    result.setException(it)
                }
            }
            return result
        }
    }
}
