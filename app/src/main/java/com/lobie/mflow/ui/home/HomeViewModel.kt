package com.lobie.mflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lobie.mflow.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(isLoading = true))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var currentSeedVideoId: String? = null

    init {
        loadHomeFeed()
        observeHistoryAndFavorites()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val feed = musicRepository.getHomeFeed()
                _state.update {
                    it.copy(
                        trendingHits = feed,
                        quickPicks = if (it.quickPicks.isEmpty()) feed.take(10) else it.quickPicks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private fun observeHistoryAndFavorites() {
        musicRepository.getRecentHistory().onEach { history ->
            _state.update { it.copy(recentHistory = history) }

            // Smart Radio Recommendation: Seed from last played song
            val lastPlayed = history.firstOrNull()
            if (lastPlayed != null && lastPlayed.songId != currentSeedVideoId) {
                currentSeedVideoId = lastPlayed.songId
                loadRecommendations(lastPlayed.songId, lastPlayed.title)
            }
        }.launchIn(viewModelScope)

        musicRepository.getAllFavorites().onEach { favs ->
            _state.update { it.copy(favorites = favs) }
        }.launchIn(viewModelScope)
    }

    private fun loadRecommendations(seedId: String, seedTitle: String) {
        viewModelScope.launch {
            try {
                val recs = musicRepository.getRecommendations(seedId)
                if (recs.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            quickPicks = recs,
                            seedSongTitle = seedTitle
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
