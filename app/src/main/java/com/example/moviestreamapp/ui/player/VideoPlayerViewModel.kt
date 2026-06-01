package com.example.moviestreamapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviestreamapp.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPlayerViewModel(
    private val movieId: Int,
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoPlayerUiState>(VideoPlayerUiState.Loading)
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    init {
        loadTrailer()
    }

    fun loadTrailer() {
        viewModelScope.launch {
            _uiState.value = VideoPlayerUiState.Loading

            runCatching {
                repository.getMovieTrailer(movieId)
            }.onSuccess { trailer ->
                _uiState.value = if (trailer == null) {
                    VideoPlayerUiState.Error("Trailer not available for this movie")
                } else {
                    VideoPlayerUiState.Success(trailer.key)
                }
            }.onFailure { throwable ->
                _uiState.value = VideoPlayerUiState.Error(
                    throwable.message ?: "Unable to load trailer"
                )
            }
        }
    }
}
