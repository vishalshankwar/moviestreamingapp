package com.example.moviestreamapp.ui.home

import com.example.moviestreamapp.domain.model.Movie

sealed interface MovieUiState {
    data object Loading : MovieUiState
    data class Success(val movies: List<Movie>) : MovieUiState
    data object Empty : MovieUiState
    data class Error(val message: String) : MovieUiState
}
