package com.example.moviestreamapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moviestreamapp.data.local.AppDatabase
import com.example.moviestreamapp.data.remote.RetrofitClient
import com.example.moviestreamapp.data.repository.TmdbMovieRepository
import com.example.moviestreamapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovieViewModel by viewModels {
        MovieViewModelFactory(
            TmdbMovieRepository(
                movieApi = RetrofitClient.movieApi,
                favoriteMovieDao = AppDatabase.getInstance(requireContext()).favoriteMovieDao()
            )
        )
    }

    private val movieAdapter = PagingMovieAdapter { movie ->
        val action = HomeFragmentDirections.actionHomeFragmentToMovieDetailFragment(movie.id)
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupRetryButton()
        observeMovies()
        observeLoadState()
    }

    override fun onDestroyView() {
        binding.shimmerFrameLayout.stopShimmer()
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.moviesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = movieAdapter.withLoadStateFooter(
                footer = MovieLoadStateAdapter {
                    movieAdapter.retry()
                }
            )
            setHasFixedSize(true)
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            movieAdapter.retry()
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun observeMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movies.collectLatest { pagingData ->
                    movieAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeLoadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieAdapter.loadStateFlow.collectLatest { loadStates ->
                    val refreshState = loadStates.refresh
                    val isInitialLoading = refreshState is LoadState.Loading
                    val isContentVisible = refreshState is LoadState.NotLoading
                    val isEmpty = isContentVisible && movieAdapter.itemCount == 0
                    val isError = refreshState is LoadState.Error

                    binding.progressBar.visibility = View.GONE
                    binding.shimmerFrameLayout.visibility = if (isInitialLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    if (isInitialLoading) {
                        binding.shimmerFrameLayout.startShimmer()
                    } else {
                        binding.shimmerFrameLayout.stopShimmer()
                    }

                    binding.moviesRecyclerView.visibility =
                        if (isContentVisible && !isEmpty) View.VISIBLE else View.GONE
                    binding.emptyTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.errorGroup.visibility = if (isError) View.VISIBLE else View.GONE
                    binding.errorTextView.text = (refreshState as? LoadState.Error)?.error?.message
                }
            }
        }
    }
}
