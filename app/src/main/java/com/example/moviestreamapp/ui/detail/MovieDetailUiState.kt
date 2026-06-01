package com.example.moviestreamapp.ui.detail

import com.example.moviestreamapp.domain.model.MovieDetail

sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState
    data class Success(val movieDetail: MovieDetail) : MovieDetailUiState
    data class Error(val message: String) : MovieDetailUiState
}
