package com.lobie.mflow.ui.player

import androidx.compose.runtime.Immutable
import com.lobie.mflow.data.parser.LyricLine
import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.player.model.SkipSegment

@Immutable
data class PlayerState(
    val currentSong: MflowMediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val isFavorite: Boolean = false,
    val isSponsorBlockActive: Boolean = true,
    val activeSkipSegments: List<SkipSegment> = emptyList(),
    val lyrics: List<LyricLine> = emptyList(),
    val isExpanded: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
