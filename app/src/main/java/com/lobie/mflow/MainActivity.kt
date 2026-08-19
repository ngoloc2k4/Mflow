package com.lobie.mflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lobie.mflow.ui.common.MiniPlayer
import com.lobie.mflow.ui.common.MiniPlayerState
import com.lobie.mflow.ui.home.HomeScreen
import com.lobie.mflow.ui.home.HomeViewModel
import com.lobie.mflow.ui.player.PlayerScreen
import com.lobie.mflow.ui.player.PlayerViewModel
import com.lobie.mflow.ui.search.SearchScreen
import com.lobie.mflow.ui.search.SearchViewModel
import com.lobie.mflow.ui.theme.DarkBackground
import com.lobie.mflow.ui.theme.DarkSurface
import com.lobie.mflow.ui.theme.MflowTheme
import com.lobie.mflow.ui.theme.PrimaryRed
import com.lobie.mflow.ui.theme.TextPrimary
import com.lobie.mflow.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val app by lazy { application as MflowApplication }

    private val homeViewModel by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(app.musicRepository) as T
            }
        }
    }

    private val searchViewModel by viewModels<SearchViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(app.musicRepository) as T
            }
        }
    }

    private val playerViewModel by viewModels<PlayerViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PlayerViewModel(app.musicRepository, app.sponsorBlockRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel.initialize(this)

        setContent {
            MflowTheme {
                val homeState by homeViewModel.state.collectAsState()
                val searchState by searchViewModel.state.collectAsState()
                val playerState by playerViewModel.state.collectAsState()

                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                                label = { Text("Trang chủ") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryRed,
                                    selectedTextColor = PrimaryRed,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = DarkBackground
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                                label = { Text("Tìm kiếm") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryRed,
                                    selectedTextColor = PrimaryRed,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = DarkBackground
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                state = homeState,
                                onSongClick = { playerViewModel.playSong(it) }
                            )
                            1 -> SearchScreen(
                                state = searchState,
                                onQueryChange = { searchViewModel.onQueryChange(it) },
                                onSearch = { searchViewModel.performSearch(it) },
                                onFilterSelected = { searchViewModel.onFilterSelected(it) },
                                onSongClick = { playerViewModel.playSong(it) }
                            )
                        }

                        // Mini Player
                        if (playerState.currentSong != null && !playerState.isExpanded) {
                            MiniPlayer(
                                state = MiniPlayerState(
                                    currentSong = playerState.currentSong,
                                    isPlaying = playerState.isPlaying,
                                    progress = playerState.progress
                                ),
                                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                                onNextClick = { /* Next song */ },
                                onClick = { playerViewModel.setExpanded(true) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        // Fullscreen Player
                        AnimatedVisibility(
                            visible = playerState.isExpanded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            PlayerScreen(
                                state = playerState,
                                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                                onSeekTo = { playerViewModel.seekTo(it) },
                                onFavoriteClick = { playerViewModel.toggleFavorite() },
                                onCollapseClick = { playerViewModel.setExpanded(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
