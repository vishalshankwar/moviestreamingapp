package com.example.moviestreamapp.domain.model

data class MovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val runtime: Int?,
    val genres: List<String>,
    val similarMovies: List<Movie>
)
