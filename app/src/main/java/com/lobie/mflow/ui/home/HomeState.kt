package com.lobie.mflow.ui.home

import androidx.compose.runtime.Immutable
import com.lobie.mflow.data.api.SearchItem
import com.lobie.mflow.data.local.entity.FavoriteEntity
import com.lobie.mflow.data.local.entity.HistoryEntity

@Immutable
data class HomeState(
    val isLoading: Boolean = false,
    val quickPicks: List<SearchItem> = emptyList(),
    val seedSongTitle: String? = null,
    val trendingHits: List<SearchItem> = emptyList(),
    val recentHistory: List<HistoryEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val errorMessage: String? = null
)
