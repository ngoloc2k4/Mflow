package com.lobie.mflow.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.ui.common.AsyncImageCompat
import com.lobie.mflow.ui.home.SongRowItem
import com.lobie.mflow.ui.theme.DarkBackground
import com.lobie.mflow.ui.theme.DarkSurface
import com.lobie.mflow.ui.theme.DarkSurfaceVariant
import com.lobie.mflow.ui.theme.PrimaryRed
import com.lobie.mflow.ui.theme.TextPrimary
import com.lobie.mflow.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFilterSelected: (String) -> Unit,
    onSongClick: (MflowMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val filters = listOf("Tất cả", "Bài hát", "Video", "Nghệ sĩ", "Playlist")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 16.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Tìm kiếm bài hát, nghệ sĩ...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = PrimaryRed,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch(state.query)
            })
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = state.selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryRed,
                        selectedLabelColor = TextPrimary,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        if (state.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.searchResults, key = { it.id }) { item ->
                    SongRowItem(
                        title = item.title,
                        artist = item.artist,
                        thumbnailUrl = item.thumbnailUrl,
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
}
