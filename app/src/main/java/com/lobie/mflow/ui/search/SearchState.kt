package com.lobie.mflow.ui.search

import androidx.compose.runtime.Immutable
import com.lobie.mflow.data.api.SearchItem

@Immutable
data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchItem> = emptyList(),
    val selectedFilter: String = "Tất cả",
    val errorMessage: String? = null
)
