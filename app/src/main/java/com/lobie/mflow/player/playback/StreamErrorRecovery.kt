package com.lobie.mflow.player.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.lobie.mflow.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreamErrorRecovery(
    private val player: Player,
    private val musicRepository: MusicRepository,
    private val scope: CoroutineScope
) {
    fun handlePlaybackError(error: PlaybackException, currentVideoId: String?) {
        val isHttpError = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.message?.contains("403") == true

        if (isHttpError && currentVideoId != null) {
            val currentPos = player.currentPosition
            scope.launch(Dispatchers.Main) {
                // Silent refresh stream URL from YouTube InnerTube
                val newStreamInfo = musicRepository.getStreamInfo(currentVideoId, forceRefresh = true)
                if (newStreamInfo != null) {
                    val currentItem = player.currentMediaItem
                    if (currentItem != null && currentItem.mediaId == currentVideoId) {
                        val updatedItem = currentItem.buildUpon()
                            .setUri(newStreamInfo.audioUrl)
                            .build()
                        player.setMediaItem(updatedItem, currentPos)
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
    }
}
