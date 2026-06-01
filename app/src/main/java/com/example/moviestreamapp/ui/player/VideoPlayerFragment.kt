package com.example.moviestreamapp.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.moviestreamapp.data.local.AppDatabase
import com.example.moviestreamapp.data.remote.RetrofitClient
import com.example.moviestreamapp.data.repository.TmdbMovieRepository
import com.example.moviestreamapp.databinding.FragmentVideoPlayerBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import kotlinx.coroutines.launch

class VideoPlayerFragment : Fragment() {
    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private val args: VideoPlayerFragmentArgs by navArgs()
    private val viewModel: VideoPlayerViewModel by viewModels {
        VideoPlayerViewModelFactory(
            movieId = args.movieId,
            repository = TmdbMovieRepository(
                movieApi = RetrofitClient.movieApi,
                favoriteMovieDao = AppDatabase.getInstance(requireContext()).favoriteMovieDao()
            )
        )
    }

    private var youTubePlayer: YouTubePlayer? = null
    private var pendingVideoId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleTextView.text = args.movieTitle
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        setupYouTubePlayer()
        setupRetryButton()
        observeTrailer()
    }

    override fun onResume() {
        super.onResume()
        enterFullscreen()
    }

    override fun onPause() {
        exitFullscreen()
        super.onPause()
    }

    override fun onDestroyView() {
        viewLifecycleOwner.lifecycle.removeObserver(binding.youtubePlayerView)
        youTubePlayer = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupYouTubePlayer() {
        viewLifecycleOwner.lifecycle.addObserver(binding.youtubePlayerView)
        binding.youtubePlayerView.addYouTubePlayerListener(
            object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    this@VideoPlayerFragment.youTubePlayer = youTubePlayer
                    pendingVideoId?.let { videoId ->
                        youTubePlayer.loadVideo(videoId, 0f)
                        pendingVideoId = null
                    }
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    binding.bufferingTextView.visibility =
                        if (state == PlayerConstants.PlayerState.BUFFERING) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    binding.errorTextView.text = error.toString()
                    binding.errorGroup.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            viewModel.loadTrailer()
        }
    }

    private fun observeTrailer() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    renderUiState(uiState)
                }
            }
        }
    }

    private fun renderUiState(uiState: VideoPlayerUiState) {
        binding.progressBar.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE

        when (uiState) {
            VideoPlayerUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is VideoPlayerUiState.Success -> {
                loadVideo(uiState.videoId)
            }

            is VideoPlayerUiState.Error -> {
                binding.errorGroup.visibility = View.VISIBLE
                binding.errorTextView.text = uiState.message
            }
        }
    }

    private fun loadVideo(videoId: String) {
        val player = youTubePlayer
        if (player == null) {
            pendingVideoId = videoId
        } else {
            player.loadVideo(videoId, 0f)
        }
    }

    private fun enterFullscreen() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitFullscreen() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }
}
