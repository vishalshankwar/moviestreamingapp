package com.example.moviestreamapp.data.remote

import com.example.moviestreamapp.data.remote.dto.MovieResponseDto
import com.example.moviestreamapp.data.remote.dto.MovieDetailDto
import com.example.moviestreamapp.data.remote.dto.VideoResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Response<MovieResponseDto>

    @GET("search/movie")
    suspend fun searchMovies(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): Response<MovieResponseDto>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Header("Authorization") authorization: String,
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): Response<MovieDetailDto>

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Header("Authorization") authorization: String,
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Response<MovieResponseDto>

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Header("Authorization") authorization: String,
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): Response<VideoResponseDto>
}
