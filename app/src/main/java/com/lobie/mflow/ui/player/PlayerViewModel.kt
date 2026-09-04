package com.lobie.mflow.ui.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.withTimeoutOrNull

class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val sponsorBlockRepository: SponsorBlockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var retryCount = 0
    private val maxRetryCount = 3

    fun initialize(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MflowMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (e: Exception) {
                Log.e("MflowPlayer", "Failed to initialize MediaController", e)
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
                    retryCount = 0
                } else if (playbackState == Player.STATE_ENDED) {
                    retryCount = 0
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("MflowPlayer", "Playback error: ${error.message}", error)
                val currentSong = _state.value.currentSong
                if (currentSong != null && retryCount < maxRetryCount) {
                    retryCount++
                    Log.d("MflowPlayer", "Retrying playback ($retryCount/$maxRetryCount) for ${currentSong.id}")
                    playSong(currentSong, isRetry = true)
                } else {
                    _state.update { 
                        it.copy(
                            currentSong = currentSong?.copyWithError("Không thể phát bài hát: ${error.message}"),
                            isPlaying = false
                        )
                    }
                    retryCount = 0
                }
            }
        })
    }

    fun playSong(song: MflowMediaItem, isRetry: Boolean = false) {
        val songWithLoading = if (!isRetry) song.copyWithLoading() else song
        _state.update { 
            it.copy(
                currentSong = if (isRetry) it.currentSong else songWithLoading,
                isExpanded = true,
                isPlaying = false
            ) 
        }
        
        viewModelScope.launch {
            try {
                if (!isRetry) {
                    retryCount = 0
                }
                
                // Fetch stream info with timeout protection
                val streamInfo = withTimeoutOrNull(15000) {
                    musicRepository.getStreamInfo(song.id, forceRefresh = isRetry)
                }
                
                Log.d("MflowStream", "Resolved Stream URL for ${song.id}: ${streamInfo?.audioUrl}")
                
                if (streamInfo != null && streamInfo.audioUrl.isNotEmpty()) {
                    val fullSong = song.copyWithStreamUrl(streamInfo.audioUrl)
                    
                    val controller = mediaController ?: run {
                        _state.update { it.copy(currentSong = fullSong.copyWithError("Trình phát chưa sẵn sàng")) }
                        return@launch
                    }
                    
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
                    
                    // Record history after successful playback start
                    musicRepository.recordHistory(fullSong)
                } else {
                    Log.e("MflowStream", "Failed to get stream info for ${song.id}")
                    val errorMsg = when {
                        streamInfo == null -> "Không thể lấy thông tin luồng từ YouTube"
                        streamInfo.audioUrl.isEmpty() -> "Luồng phát trống"
                        else -> "Không tìm thấy luồng phát cho bài hát này"
                    }
                    _state.update {
                        it.copy(
                            currentSong = song.copyWithError(errorMsg),
                            isPlaying = false
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("MflowStream", "Timeout fetching stream info for ${song.id}", e)
                _state.update {
                    it.copy(
                        currentSong = song.copyWithError("Quá thời gian chờ kết nối. Vui lòng thử lại."),
                        isPlaying = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MflowStream", "Error fetching stream info for ${song.id}: ${e.message}", e)
                val errorDetail = when {
                    e.message?.contains("403") == true -> "Liên kết hết hạn. Đang thử lại..."
                    e.message?.contains("404") == true -> "Video không tồn tại hoặc đã bị xóa"
                    e.message?.contains("timeout") == true -> "Quá thời gian chờ kết nối"
                    else -> "Lỗi kết nối: ${e.localizedMessage ?: "Không rõ"}"
                }
                _state.update {
                    it.copy(
                        currentSong = song.copyWithError(errorDetail),
                        isPlaying = false
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
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0)
            }
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
