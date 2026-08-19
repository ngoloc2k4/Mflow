package com.lobie.mflow.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lobie.mflow.data.parser.LyricLine
import com.lobie.mflow.ui.theme.PrimaryRed
import com.lobie.mflow.ui.theme.TextSecondary
import com.lobie.mflow.ui.theme.TextTertiary

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Không có lời bài hát",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary
            )
        }
        return
    }

    val currentLineIndex = lyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 48.dp, horizontal = 24.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isCurrent = index == currentLineIndex
            val textColor by animateColorAsState(
                targetValue = if (isCurrent) PrimaryRed else TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "lyric_color"
            )

            Text(
                text = line.text,
                color = textColor,
                fontSize = if (isCurrent) 20.sp else 16.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }
}
