package com.lobie.mflow.player.model

import androidx.compose.runtime.Immutable

@Immutable
data class SkipSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long
)
