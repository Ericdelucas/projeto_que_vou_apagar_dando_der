package com.example.app.feed

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemPostBinding
import com.example.app.network.PostResponse
import com.example.app.favorites.FavoritesState
import com.example.app.favorites.bindFavorite
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

fun formatPrice(price: String): String =
    price.toBigDecimalOrNull()?.let {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(it)
    } ?: "—"

class FeedAdapter(
    private val onProductClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit,
) :
    ListAdapter<PostResponse, FeedAdapter.PostHolder>(object : DiffUtil.ItemCallback<PostResponse>() {
        override fun areItemsTheSame(old: PostResponse, new: PostResponse) = old.id == new.id
        override fun areContentsTheSame(old: PostResponse, new: PostResponse) = old == new
    }) {
    private val attachedHolders = mutableSetOf<PostHolder>()
    private var favorites = FavoritesState()

    fun updateFavorites(state: FavoritesState) {
        favorites = state
        notifyItemRangeChanged(0, itemCount, "favorites")
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) holder.bindFavorite(getItem(position))
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PostHolder(
        ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PostHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewAttachedToWindow(holder: PostHolder) {
        super.onViewAttachedToWindow(holder)
        attachedHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: PostHolder) {
        holder.stopVideo()
        attachedHolders.remove(holder)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: PostHolder) {
        holder.stopVideo()
        Glide.with(holder.itemView).clear(holder.ui.postImage)
        super.onViewRecycled(holder)
    }

    fun stopVideos() = attachedHolders.forEach { it.stopVideo() }

    inner class PostHolder(val ui: ItemPostBinding) : RecyclerView.ViewHolder(ui.root) {
        private var videoUrl: String? = null

        fun bind(post: PostResponse) {
            stopVideo()
            val context = ui.root.context
            ui.authorName.text = post.user.name ?: context.getString(R.string.feed_unknown_author)
            ui.postDescription.text = post.description.orEmpty()
            ui.postDescription.visibility = if (post.description.isNullOrBlank()) View.GONE else View.VISIBLE
            ui.postCounts.text = context.getString(R.string.feed_counts, post.likesCount, post.commentsCount)
            val product = post.product
            ui.productTag.visibility = if (product == null) View.GONE else View.VISIBLE
            ui.productTitle.text = product?.title.orEmpty()
            ui.productPrice.text = product?.let { formatPrice(it.price) }.orEmpty()
            ui.buyButton.setOnClickListener { product?.let { onProductClick(it.id) } }
            bindFavorite(post)
            ui.postImage.contentDescription = product?.title ?: post.description
            Glide.with(ui.root).load(post.imageUrl ?: product?.imageUrl)
                .placeholder(R.drawable.ic_steering_wheel)
                .error(R.drawable.ic_steering_wheel).into(ui.postImage)
            ui.postImage.visibility = if ((post.imageUrl ?: product?.imageUrl).isNullOrBlank()) View.GONE else View.VISIBLE
            videoUrl = post.videoUrl?.takeIf { it.isNotBlank() } ?: product?.videoUrl?.takeIf { it.isNotBlank() }
            ui.playVideo.visibility = if (videoUrl == null) View.GONE else View.VISIBLE
            ui.playVideo.setOnClickListener {
                val url = videoUrl ?: return@setOnClickListener
                stopVideos()
                ui.postVideo.visibility = View.VISIBLE
                ui.playVideo.visibility = View.GONE
                ui.postImage.visibility = View.GONE
                val controls = MediaController(context)
                controls.setAnchorView(ui.postVideo)
                ui.postVideo.setMediaController(controls)
                ui.postVideo.setOnPreparedListener { it.start() }
                ui.postVideo.setOnErrorListener { _, _, _ ->
                    stopVideo()
                    Snackbar.make(ui.root, R.string.feed_video_error, Snackbar.LENGTH_LONG).show()
                    true
                }
                ui.postVideo.setVideoURI(Uri.parse(url))
            }
        }

        fun bindFavorite(post: PostResponse) {
            post.product?.let { ui.favoriteButton.bindFavorite(it.id, favorites, onFavoriteClick) }
        }

        fun stopVideo() {
            ui.postVideo.setOnPreparedListener(null)
            ui.postVideo.setOnErrorListener(null)
            ui.postVideo.stopPlayback()
            ui.postVideo.visibility = View.GONE
            ui.playVideo.visibility = if (videoUrl == null) View.GONE else View.VISIBLE
            val post = currentList.getOrNull(bindingAdapterPosition)
            ui.postImage.visibility = if (post?.product?.imageUrl.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }
}
