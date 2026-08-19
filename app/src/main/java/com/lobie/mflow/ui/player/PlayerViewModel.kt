package com.lobie.mflow.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lobie.mflow.data.repository.MusicRepository
import com.lobie.mflow.data.repository.SponsorBlockRepository
import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.player.service.MflowMediaSessionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val sponsorBlockRepository: SponsorBlockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    fun initialize(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MflowMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val song = MflowMediaItem.fromMediaItem(mediaItem)
                    _state.update {
                        it.copy(
                            currentSong = song,
                            durationMs = controller.duration.coerceAtLeast(0)
                        )
                    }
                    observeFavorite(song.id)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(durationMs = controller.duration.coerceAtLeast(0)) }
                }
            }
        })
    }

    fun playSong(song: MflowMediaItem) {
        _state.update { it.copy(currentSong = song, isExpanded = true) }
        viewModelScope.launch {
            val streamInfo = musicRepository.getStreamInfo(song.id)
            android.util.Log.d("MflowStream", "Resolved Stream URL for ${song.id}: ${streamInfo?.audioUrl}")
            if (streamInfo != null && streamInfo.audioUrl.isNotEmpty()) {
                val fullSong = song.copy(streamUrl = streamInfo.audioUrl)

                val controller = mediaController ?: return@launch
                controller.setMediaItem(fullSong.toMediaItem())
                controller.prepare()
                controller.play()

                _state.update {
                    it.copy(
                        currentSong = fullSong,
                        isPlaying = true,
                        isExpanded = true
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleFavorite() {
        val song = _state.value.currentSong ?: return
        val currentFav = _state.value.isFavorite
        viewModelScope.launch {
            musicRepository.toggleFavorite(song, currentFav)
            _state.update { it.copy(isFavorite = !currentFav) }
        }
    }

    private fun observeFavorite(songId: String) {
        viewModelScope.launch {
            musicRepository.isFavorite(songId).collect { isFav ->
                _state.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _state.update {
                        it.copy(
                            currentPositionMs = controller.currentPosition,
                            durationMs = controller.duration.coerceAtLeast(0)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun setExpanded(expanded: Boolean) {
        _state.update { it.copy(isExpanded = expanded) }
    }

    override fun onCleared() {
        stopProgressTracking()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
