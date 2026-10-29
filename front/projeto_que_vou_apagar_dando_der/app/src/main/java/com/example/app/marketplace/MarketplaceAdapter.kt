package com.example.app.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemMarketplaceProductBinding
import com.example.app.feed.formatPrice
import com.example.app.network.ProductTag
import com.example.app.favorites.FavoritesState
import com.example.app.favorites.bindFavorite

class MarketplaceAdapter(
    private val onProductClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit,
) :
    ListAdapter<ProductTag, MarketplaceAdapter.ProductHolder>(object : DiffUtil.ItemCallback<ProductTag>() {
        override fun areItemsTheSame(old: ProductTag, new: ProductTag) = old.id == new.id
        override fun areContentsTheSame(old: ProductTag, new: ProductTag) = old == new
    }) {
    private var favorites = FavoritesState()

    fun updateFavorites(state: FavoritesState) {
        favorites = state
        notifyItemRangeChanged(0, itemCount, "favorites")
    }

    override fun onBindViewHolder(holder: ProductHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) holder.ui.favoriteButton.bindFavorite(getItem(position).id, favorites, onFavoriteClick)
        else super.onBindViewHolder(holder, position, payloads)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProductHolder(
        ItemMarketplaceProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ProductHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
        holder.ui.favoriteButton.bindFavorite(product.id, favorites, onFavoriteClick)
        holder.ui.root.setOnClickListener { onProductClick(product.id) }
    }

    class ProductHolder(val ui: ItemMarketplaceProductBinding) : RecyclerView.ViewHolder(ui.root) {
        fun bind(product: ProductTag) {
            ui.productTitle.text = product.title
            ui.productPrice.text = formatPrice(product.price)
            ui.productImage.contentDescription = product.title
            Glide.with(ui.root).load(product.imageUrl)
                .placeholder(R.drawable.ic_steering_wheel)
                .error(R.drawable.ic_steering_wheel).into(ui.productImage)
        }
    }

    override fun onViewRecycled(holder: ProductHolder) {
        Glide.with(holder.itemView).clear(holder.ui.productImage)
        holder.ui.root.setOnClickListener(null)
        super.onViewRecycled(holder)
    }
}
