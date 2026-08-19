package com.lobie.mflow.player.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lobie.mflow.MflowApplication
import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.player.playback.ExoPlayerFactory
import com.lobie.mflow.player.playback.SponsorBlockSkipHandler
import com.lobie.mflow.player.playback.StreamErrorRecovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MflowMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var sponsorBlockHandler: SponsorBlockSkipHandler
    private lateinit var errorRecovery: StreamErrorRecovery

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayerFactory.create(this)
        setMediaNotificationProvider(MflowNotificationProvider(this))

        val app = application as MflowApplication
        sponsorBlockHandler = SponsorBlockSkipHandler(player, serviceScope)
        errorRecovery = StreamErrorRecovery(player, app.musicRepository, serviceScope)

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val videoId = mediaItem.mediaId
                    serviceScope.launch {
                        val segments = app.sponsorBlockRepository.getSegments(videoId)
                        sponsorBlockHandler.setSegments(segments)

                        val item = MflowMediaItem.fromMediaItem(mediaItem)
                        app.musicRepository.recordHistory(item)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                val currentVideoId = player.currentMediaItem?.mediaId
                errorRecovery.handlePlaybackError(error, currentVideoId)
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        sponsorBlockHandler.stop()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
