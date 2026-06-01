package com.example.moviestreamapp.ui.player

sealed interface VideoPlayerUiState {
    data object Loading : VideoPlayerUiState
    data class Success(val videoId: String) : VideoPlayerUiState
    data class Error(val message: String) : VideoPlayerUiState
}
