package com.lobie.mflow.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lobie.mflow.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }

        searchJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce 400ms
            performSearch(newQuery)
        }
    }

    fun performSearch(query: String = _state.value.query) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, errorMessage = null) }
            try {
                val results = musicRepository.search(query)
                _state.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun onFilterSelected(filter: String) {
        _state.update { it.copy(selectedFilter = filter) }
    }
}
