package com.lobie.mflow.player.playback

import androidx.media3.common.Player
import com.lobie.mflow.player.model.SkipSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SponsorBlockSkipHandler(
    private val player: Player,
    private val scope: CoroutineScope,
    private val onSkipped: (SkipSegment) -> Unit = {}
) {
    private var activeSegments: List<SkipSegment> = emptyList()
    private var checkJob: Job? = null
    var isEnabled: Boolean = true

    fun setSegments(segments: List<SkipSegment>) {
        activeSegments = segments.sortedBy { it.startMs }
        restartChecking()
    }

    private fun restartChecking() {
        checkJob?.cancel()
        if (activeSegments.isEmpty() || !isEnabled) return

        checkJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    val segmentToSkip = activeSegments.firstOrNull { segment ->
                        currentPos in segment.startMs..(segment.endMs - 100)
                    }

                    if (segmentToSkip != null) {
                        player.seekTo(segmentToSkip.endMs)
                        onSkipped(segmentToSkip)
                    }
                }
                delay(250) // 250ms periodic skip check for minimum CPU overhead
            }
        }
    }

    fun stop() {
        checkJob?.cancel()
        checkJob = null
        activeSegments = emptyList()
    }
}
