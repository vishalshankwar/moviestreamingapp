package com.example.moviestreamapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.moviestreamapp.data.remote.MovieApi
import com.example.moviestreamapp.data.remote.dto.MovieDto
import com.example.moviestreamapp.domain.model.Movie

class MoviePagingSource(
    private val movieApi: MovieApi,
    private val authorizationHeader: String,
    private val query: String? = null
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        return try {
            val page = params.key?: FIRST_PAGE
            val response = if (query.isNullOrBlank()) {
                movieApi.getPopularMovies(
                    authorization = authorizationHeader,
                    page = page
                )
            } else {
                movieApi.searchMovies(
                    authorization = authorizationHeader,
                    query = query,
                    page = page
                )
            }

            if (!response.isSuccessful) {
                return LoadResult.Error(
                    IllegalStateException("TMDB request failed: ${response.code()}")
                )
            }

            val body = response.body()
            val movies = body?.results.orEmpty().map { movieDto ->
                movieDto.toDomain()
            }
            val totalPages = body?.totalPages ?: page

            LoadResult.Page(
                data = movies,
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (page >= totalPages) null else page + 1
            )
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    private fun MovieDto.toDomain(): Movie {
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
        const val FIRST_PAGE = 1
    }
}
