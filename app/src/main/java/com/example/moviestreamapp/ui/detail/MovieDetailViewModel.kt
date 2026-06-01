package com.example.moviestreamapp.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviestreamapp.domain.model.MovieDetail
import com.example.moviestreamapp.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieDetailViewModel(
    private val movieId: Int,
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        loadMovieDetails()
        observeFavoriteState()
    }

    fun loadMovieDetails() {
        viewModelScope.launch {
            _uiState.value = MovieDetailUiState.Loading

            runCatching {
                repository.getMovieDetails(movieId)
            }.onSuccess { movieDetail ->
                _uiState.value = MovieDetailUiState.Success(movieDetail)
            }.onFailure { throwable ->
                _uiState.value = MovieDetailUiState.Error(
                    throwable.message ?: "Unable to load movie details"
                )
            }
        }
    }

    fun toggleFavorite(movieDetail: MovieDetail) {
        viewModelScope.launch {
            if (_isFavorite.value) {
                repository.removeFavorite(movieDetail.id)
            } else {
                repository.addFavorite(movieDetail)
            }
        }
    }

    private fun observeFavoriteState() {
        viewModelScope.launch {
            repository.observeIsFavorite(movieId).collect { isFavorite ->
                _isFavorite.value = isFavorite
            }
        }
    }
}
