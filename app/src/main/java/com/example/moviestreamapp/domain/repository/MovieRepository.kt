package com.example.moviestreamapp.domain.repository

import com.example.moviestreamapp.domain.model.Movie
import com.example.moviestreamapp.domain.model.MovieDetail
import com.example.moviestreamapp.domain.model.MovieVideo
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getPopularMovies(page: Int = 1): List<Movie>
    fun getPopularMoviesPaging(): Flow<PagingData<Movie>>
    suspend fun searchMovies(query: String, page: Int = 1): List<Movie>
    fun searchMoviesPaging(query: String): Flow<PagingData<Movie>>
    suspend fun getMovieDetails(movieId: Int): MovieDetail
    suspend fun getMovieTrailer(movieId: Int): MovieVideo?
    fun observeIsFavorite(movieId: Int): Flow<Boolean>
    suspend fun addFavorite(movieDetail: MovieDetail)
    suspend fun removeFavorite(movieId: Int)
}
