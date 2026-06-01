package com.example.moviestreamapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moviestreamapp.R
import com.example.moviestreamapp.databinding.ItemMovieBinding
import com.example.moviestreamapp.domain.model.Movie

class PagingMovieAdapter(
    private val onMovieClick: (Movie) -> Unit
) : PagingDataAdapter<Movie, PagingMovieAdapter.MovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        getItem(position)?.let { movie ->
            holder.bind(movie, onMovieClick)
        }
    }

    class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie, onMovieClick: (Movie) -> Unit) {
            binding.titleTextView.text = movie.title
            binding.overviewTextView.text = movie.overview
            binding.releaseDateTextView.text = movie.releaseDate ?: "Release date unavailable"
            binding.ratingTextView.text = binding.root.context.getString(
                R.string.movie_rating_format,
                movie.voteAverage
            )

            val posterUrl = movie.posterPath?.let { posterPath ->
                "https://image.tmdb.org/t/p/w500$posterPath"
            }

            Glide.with(binding.posterImageView)
                .load(posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(binding.posterImageView)

            binding.root.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    private object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}
