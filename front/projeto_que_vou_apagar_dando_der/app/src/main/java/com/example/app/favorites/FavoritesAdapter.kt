package com.example.app.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemFavoriteBinding
import com.example.app.feed.formatPrice
import com.example.app.network.FavoriteItem

class FavoritesAdapter(
    private val onProductClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit,
) : ListAdapter<FavoriteItem, FavoritesAdapter.FavoriteHolder>(object : DiffUtil.ItemCallback<FavoriteItem>() {
    override fun areItemsTheSame(old: FavoriteItem, new: FavoriteItem) = old.id == new.id
    override fun areContentsTheSame(old: FavoriteItem, new: FavoriteItem) = old == new
}) {
    private var state = FavoritesState()

    fun updateState(newState: FavoritesState) {
        state = newState
        submitList(newState.items)
        notifyItemRangeChanged(0, itemCount, "favorites")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FavoriteHolder(
        ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        val favorite = getItem(position)
        val product = favorite.product
        holder.ui.apply {
            productTitle.text = product.title
            productPrice.text = formatPrice(product.price)
            authorName.text = root.context.getString(R.string.favorites_author,
                favorite.author.name ?: root.context.getString(R.string.feed_unknown_author))
            productImage.contentDescription = product.title
            Glide.with(root).load(product.imageUrl)
                .placeholder(R.drawable.ic_steering_wheel)
                .error(R.drawable.ic_steering_wheel).into(productImage)
            favoriteButton.bindFavorite(product.id, state, onFavoriteClick)
            root.setOnClickListener { onProductClick(product.id) }
        }
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) holder.ui.favoriteButton.bindFavorite(getItem(position).productId, state, onFavoriteClick)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: FavoriteHolder) {
        Glide.with(holder.itemView).clear(holder.ui.productImage)
        super.onViewRecycled(holder)
    }

    class FavoriteHolder(val ui: ItemFavoriteBinding) : RecyclerView.ViewHolder(ui.root)
}
