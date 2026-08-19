package com.lobie.mflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lobie.mflow.data.api.SearchItem
import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.ui.common.AsyncImageCompat
import com.lobie.mflow.ui.theme.DarkBackground
import com.lobie.mflow.ui.theme.PrimaryRed
import com.lobie.mflow.ui.theme.TextPrimary
import com.lobie.mflow.ui.theme.TextSecondary
import com.lobie.mflow.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    state: HomeState,
    onSongClick: (MflowMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.trendingHits.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryRed)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item {
            Text(
                text = "Khám phá",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
            )
        }

        // Section 1: Quick Picks / Smart Radio Recommendations
        if (state.quickPicks.isNotEmpty()) {
            item {
                val headerText = if (!state.seedSongTitle.isNullOrEmpty()) {
                    "Gợi ý từ: ${state.seedSongTitle}"
                } else {
                    "Gợi ý cho bạn (Quick Picks)"
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.quickPicks, key = { it.id }) { item ->
                        QuickPickCard(
                            item = item,
                            onClick = {
                                onSongClick(
                                    MflowMediaItem(
                                        id = item.id,
                                        title = item.title,
                                        artist = item.artist,
                                        album = item.album,
                                        thumbnailUrl = item.thumbnailUrl
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Favorites
        if (state.favorites.isNotEmpty()) {
            item {
                Text(
                    text = "Bài hát yêu thích",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(state.favorites, key = { it.songId }) { fav ->
                SongRowItem(
                    title = fav.title,
                    artist = fav.artist,
                    thumbnailUrl = fav.thumbnailUrl,
                    onClick = {
                        onSongClick(
                            MflowMediaItem(
                                id = fav.songId,
                                title = fav.title,
                                artist = fav.artist,
                                album = fav.album,
                                durationMs = fav.durationMs,
                                thumbnailUrl = fav.thumbnailUrl,
                                isFavorite = true
                            )
                        )
                    }
                )
            }
        }

        // Section 3: Recent History
        if (state.recentHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Đã nghe gần đây",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(state.recentHistory, key = { it.historyId }) { history ->
                SongRowItem(
                    title = history.title,
                    artist = history.artist,
                    thumbnailUrl = history.thumbnailUrl,
                    onClick = {
                        onSongClick(
                            MflowMediaItem(
                                id = history.songId,
                                title = history.title,
                                artist = history.artist,
                                thumbnailUrl = history.thumbnailUrl
                            )
                        )
                    }
                )
            }
        }

        // Section 4: Trending Hits
        if (state.trendingHits.isNotEmpty()) {
            item {
                Text(
                    text = "Bảng xếp hạng thịnh hành (Trending Hits)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(state.trendingHits, key = { "trend_${it.id}" }) { trend ->
                SongRowItem(
                    title = trend.title,
                    artist = trend.artist,
                    thumbnailUrl = trend.thumbnailUrl,
                    onClick = {
                        onSongClick(
                            MflowMediaItem(
                                id = trend.id,
                                title = trend.title,
                                artist = trend.artist,
                                album = trend.album,
                                thumbnailUrl = trend.thumbnailUrl
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    item: SearchItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImageCompat(
            url = item.thumbnailUrl,
            contentDescription = item.title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongRowItem(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageCompat(
            url = thumbnailUrl,
            contentDescription = title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
