package com.example.moviestreamapp.data.repository

import com.example.moviestreamapp.BuildConfig
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.moviestreamapp.data.local.FavoriteMovieDao
import com.example.moviestreamapp.data.local.FavoriteMovieEntity
import com.example.moviestreamapp.data.paging.MoviePagingSource
import com.example.moviestreamapp.data.remote.MovieApi
import com.example.moviestreamapp.domain.model.Movie
import com.example.moviestreamapp.domain.model.MovieDetail
import com.example.moviestreamapp.domain.model.MovieVideo
import com.example.moviestreamapp.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class TmdbMovieRepository(
    private val movieApi: MovieApi,
    private val favoriteMovieDao: FavoriteMovieDao
) : MovieRepository {

    override suspend fun getPopularMovies(page: Int): List<Movie> {
        val response = movieApi.getPopularMovies(
            authorization = authorizationHeader,
            page = page
        )

        if (!response.isSuccessful) {
            throw IllegalStateException("TMDB request failed: ${response.code()}")
        }

        return response.body()?.results.orEmpty().map { movieDto ->
            movieDto.toDomain()
        }
    }

    override fun getPopularMoviesPaging(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MoviePagingSource(
                    movieApi = movieApi,
                    authorizationHeader = authorizationHeader
                )
            }
        ).flow
    }

    override suspend fun searchMovies(query: String, page: Int): List<Movie> {
        val response = movieApi.searchMovies(
            authorization = authorizationHeader,
            query = query,
            page = page
        )

        if (!response.isSuccessful) {
            throw IllegalStateException("Movie search request failed: ${response.code()}")
        }

        return response.body()?.results.orEmpty().map { movieDto ->
            movieDto.toDomain()
        }
    }

    override fun searchMoviesPaging(query: String): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MoviePagingSource(
                    movieApi = movieApi,
                    authorizationHeader = authorizationHeader,
                    query = query
                )
            }
        ).flow
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDetail {
        val detailsResponse = movieApi.getMovieDetails(
            authorization = authorizationHeader,
            movieId = movieId
        )

        if (!detailsResponse.isSuccessful) {
            throw IllegalStateException("Movie detail request failed: ${detailsResponse.code()}")
        }

        val details = detailsResponse.body()
            ?: throw IllegalStateException("Movie detail response is empty")

        val similarResponse = movieApi.getSimilarMovies(
            authorization = authorizationHeader,
            movieId = movieId
        )

        val similarMovies = if (similarResponse.isSuccessful) {
            similarResponse.body()?.results.orEmpty().take(10).map { movieDto ->
                movieDto.toDomain()
            }
        } else {
            emptyList()
        }

        return MovieDetail(
            id = details.id,
            title = details.title,
            overview = details.overview,
            posterPath = details.posterPath,
            backdropPath = details.backdropPath,
            releaseDate = details.releaseDate,
            voteAverage = details.voteAverage,
            runtime = details.runtime,
            genres = details.genres.map { genreDto -> genreDto.name },
            similarMovies = similarMovies
        )
    }

    override suspend fun getMovieTrailer(movieId: Int): MovieVideo? {
        val response = movieApi.getMovieVideos(
            authorization = authorizationHeader,
            movieId = movieId
        )

        if (!response.isSuccessful) {
            throw IllegalStateException("Movie videos request failed: ${response.code()}")
        }

        return response.body()?.results.orEmpty()
            .firstOrNull { videoDto ->
                videoDto.site.equals("YouTube", ignoreCase = true) &&
                    videoDto.type.equals("Trailer", ignoreCase = true)
            }
            ?.let { videoDto ->
                MovieVideo(
                    key = videoDto.key,
                    name = videoDto.name,
                    site = videoDto.site,
                    type = videoDto.type
                )
            }
    }

    override fun observeIsFavorite(movieId: Int): Flow<Boolean> {
        return favoriteMovieDao.observeIsFavorite(movieId)
    }

    override suspend fun addFavorite(movieDetail: MovieDetail) {
        favoriteMovieDao.insertFavorite(
            FavoriteMovieEntity(
                id = movieDetail.id,
                title = movieDetail.title,
                overview = movieDetail.overview,
                posterPath = movieDetail.posterPath,
                releaseDate = movieDetail.releaseDate,
                voteAverage = movieDetail.voteAverage
            )
        )
    }

    override suspend fun removeFavorite(movieId: Int) {
        favoriteMovieDao.deleteFavoriteById(movieId)
    }

    private val authorizationHeader: String
        get() = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"

    private fun com.example.moviestreamapp.data.remote.dto.MovieDto.toDomain(): Movie {
        return Movie(
            id = id,
            title = title,
            overview = overview,
            posterPath = posterPath,
            releaseDate = releaseDate,
            voteAverage = voteAverage
        )
    }

    private companion object {
        const val NETWORK_PAGE_SIZE = 20
    }
}
