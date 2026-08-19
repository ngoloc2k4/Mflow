package com.lobie.mflow.player.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.collect.ImmutableList
import com.lobie.mflow.MainActivity
import com.lobie.mflow.MflowApplication
import com.lobie.mflow.R

@OptIn(UnstableApi::class)
class MflowNotificationProvider(private val context: Context) : MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val player = mediaSession.player
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString() ?: context.getString(R.string.app_name)
        val artist = metadata.artist?.toString() ?: ""

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MflowApplication.PLAYBACK_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(player.isPlaying)

        val isPlaying = player.isPlaying

        // Play/Pause Action
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                actionFactory.createMediaActionPendingIntent(mediaSession, Player.COMMAND_PLAY_PAUSE.toLong())
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                actionFactory.createMediaActionPendingIntent(mediaSession, Player.COMMAND_PLAY_PAUSE.toLong())
            )
        }

        // Previous Action
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            actionFactory.createMediaActionPendingIntent(mediaSession, Player.COMMAND_SEEK_TO_PREVIOUS.toLong())
        )

        // Next Action
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            actionFactory.createMediaActionPendingIntent(mediaSession, Player.COMMAND_SEEK_TO_NEXT.toLong())
        )

        builder.addAction(prevAction)
        builder.addAction(playPauseAction)
        builder.addAction(nextAction)

        val mediaStyle = androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)

        builder.setStyle(mediaStyle)

        return MediaNotification(1001, builder.build())
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = false
}
