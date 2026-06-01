package com.example.moviestreamapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.moviestreamapp.R
import com.example.moviestreamapp.data.local.AppDatabase
import com.example.moviestreamapp.data.remote.RetrofitClient
import com.example.moviestreamapp.data.repository.TmdbMovieRepository
import com.example.moviestreamapp.databinding.FragmentMovieDetailBinding
import com.example.moviestreamapp.domain.model.MovieDetail
import com.example.moviestreamapp.ui.home.MovieAdapter
import kotlinx.coroutines.launch

class MovieDetailFragment : Fragment() {
    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val args: MovieDetailFragmentArgs by navArgs()
    private val viewModel: MovieDetailViewModel by viewModels {
        MovieDetailViewModelFactory(
            movieId = args.movieId,
            repository = TmdbMovieRepository(
                movieApi = RetrofitClient.movieApi,
                favoriteMovieDao = AppDatabase.getInstance(requireContext()).favoriteMovieDao()
            )
        )
    }

    private val similarMoviesAdapter = MovieAdapter { movie ->
        val action = MovieDetailFragmentDirections
            .actionMovieDetailFragmentSelf(movie.id)
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSimilarMovies()
        setupRetryButton()
        observeMovieDetails()
        observeFavoriteState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSimilarMovies() {
        binding.similarMoviesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = similarMoviesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            viewModel.loadMovieDetails()
        }
    }

    private fun observeMovieDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    renderUiState(uiState)
                }
            }
        }
    }

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFavorite ->
                    binding.favoriteButton.text = if (isFavorite) {
                        getString(R.string.remove_favorite)
                    } else {
                        getString(R.string.add_favorite)
                    }
                }
            }
        }
    }

    private fun renderUiState(uiState: MovieDetailUiState) {
        binding.progressBar.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE

        when (uiState) {
            MovieDetailUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is MovieDetailUiState.Success -> {
                binding.contentScrollView.visibility = View.VISIBLE
                bindMovieDetails(uiState.movieDetail)
            }

            is MovieDetailUiState.Error -> {
                binding.errorGroup.visibility = View.VISIBLE
                binding.errorTextView.text = uiState.message
            }
        }
    }

    private fun bindMovieDetails(movieDetail: MovieDetail) {
        binding.titleTextView.text = movieDetail.title
        binding.overviewTextView.text = movieDetail.overview
        binding.ratingTextView.text = getString(
            R.string.movie_rating_format,
            movieDetail.voteAverage
        )
        binding.releaseDateTextView.text = movieDetail.releaseDate ?: getString(R.string.unknown_release_date)
        binding.runtimeTextView.text = movieDetail.runtime?.let { runtime ->
            getString(R.string.runtime_format, runtime)
        } ?: getString(R.string.unknown_runtime)
        binding.genresTextView.text = if (movieDetail.genres.isEmpty()) {
            getString(R.string.unknown_genres)
        } else {
            movieDetail.genres.joinToString(separator = ", ")
        }
        binding.playTrailerButton.setOnClickListener {
            val action = MovieDetailFragmentDirections
                .actionMovieDetailFragmentToVideoPlayerFragment(
                    movieId = movieDetail.id,
                    movieTitle = movieDetail.title
                )
            findNavController().navigate(action)
        }
        binding.favoriteButton.setOnClickListener {
            viewModel.toggleFavorite(movieDetail)
        }

        val posterUrl = movieDetail.posterPath?.let { posterPath ->
            "https://image.tmdb.org/t/p/w500$posterPath"
        }
        Glide.with(binding.posterImageView)
            .load(posterUrl)
            .placeholder(R.drawable.ic_movie_placeholder)
            .error(R.drawable.ic_movie_placeholder)
            .centerCrop()
            .into(binding.posterImageView)

        similarMoviesAdapter.submitList(movieDetail.similarMovies)
        binding.similarMoviesTitleTextView.visibility = if (movieDetail.similarMovies.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.similarMoviesRecyclerView.visibility = if (movieDetail.similarMovies.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
