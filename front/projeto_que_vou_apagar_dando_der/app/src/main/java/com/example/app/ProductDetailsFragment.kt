package com.example.app

import android.os.Bundle
import android.net.Uri
import android.widget.MediaController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentProductDetailsBinding
import com.example.app.feed.formatPrice
import com.example.app.favorites.FavoritesViewModel
import com.example.app.favorites.bindFavorite
import com.example.app.favorites.observeFavorites
import com.example.app.network.ApiClient
import com.example.app.network.ProductTag
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.snackbar.Snackbar

class ProductDetailsFragment : Fragment() {
    private var binding: FragmentProductDetailsBinding? = null
    private var productCall: Call<ProductTag>? = null
    private lateinit var favorites: FavoritesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentProductDetailsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favorites = observeFavorites { state ->
            val id = arguments?.getInt("product_id") ?: 0
            binding?.favoriteButton?.bindFavorite(id, state) { favorites.toggle(it) }
        }
        binding?.backButton?.setOnClickListener { findNavController().popBackStack() }
        binding?.retryButton?.setOnClickListener { loadProduct() }
        loadProduct()
    }

    private fun loadProduct() {
        val id = arguments?.getInt("product_id") ?: 0
        if (id <= 0) { showError(R.string.product_not_found, false); return }
        if (productCall != null) return
        binding?.apply {
            productProgress.visibility = View.VISIBLE
            productMessage.visibility = View.GONE
            retryButton.visibility = View.GONE
            productContent.visibility = View.GONE
        }
        val call = ApiClient.feed.getProduct(id)
        productCall = call
        call.enqueue(object : Callback<ProductTag> {
            override fun onResponse(call: Call<ProductTag>, response: Response<ProductTag>) {
                if (productCall !== call) return
                productCall = null
                val ui = binding ?: return
                ui.productProgress.visibility = View.GONE
                val product = response.body()
                if (!response.isSuccessful || product == null) {
                    response.errorBody()?.close()
                    showError(if (response.code() == 404) R.string.product_not_found else R.string.product_error,
                        response.code() != 404)
                    return
                }
                ui.productContent.visibility = View.VISIBLE
                ui.productTitle.text = product.title
                ui.productPrice.text = formatPrice(product.price)
                ui.productInfo.text = getString(R.string.product_info,
                    product.category ?: "—", product.model ?: "—", product.location ?: "—")
                ui.productImage.contentDescription = product.title
                ui.productImage.visibility = if (product.imageUrl.isNullOrBlank()) View.GONE else View.VISIBLE
                Glide.with(this@ProductDetailsFragment).load(com.example.app.network.MediaUrl.resolve(product.imageUrl))
                    .placeholder(R.drawable.ic_steering_wheel)
                    .error(R.drawable.ic_steering_wheel).into(ui.productImage)
                ui.playVideo.visibility = if (product.videoUrl.isNullOrBlank()) View.GONE else View.VISIBLE
                ui.playVideo.setOnClickListener {
                    val url = product.videoUrl ?: return@setOnClickListener
                    ui.productVideo.visibility = View.VISIBLE
                    ui.playVideo.visibility = View.GONE
                    val controls = MediaController(requireContext())
                    controls.setAnchorView(ui.productVideo)
                    ui.productVideo.setMediaController(controls)
                    ui.productVideo.setOnPreparedListener { it.start() }
                    ui.productVideo.setOnErrorListener { _, _, _ ->
                        stopVideo()
                        Snackbar.make(ui.root, R.string.feed_video_error, Snackbar.LENGTH_LONG).show()
                        true
                    }
                    ui.productVideo.setVideoURI(Uri.parse(url))
                }
            }

            override fun onFailure(call: Call<ProductTag>, error: Throwable) {
                if (productCall !== call) return
                productCall = null
                if (!call.isCanceled) showError(R.string.product_error, true)
            }
        })
    }

    private fun showError(message: Int, retry: Boolean) {
        binding?.apply {
            productProgress.visibility = View.GONE
            productContent.visibility = View.GONE
            productMessage.setText(message)
            productMessage.visibility = View.VISIBLE
            retryButton.visibility = if (retry) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        favorites.load()
    }

    override fun onDestroyView() {
        stopVideo()
        val pending = productCall
        productCall = null
        pending?.cancel()
        binding = null
        super.onDestroyView()
    }

    private fun stopVideo() {
        binding?.apply {
            productVideo.setOnPreparedListener(null)
            productVideo.setOnErrorListener(null)
            productVideo.stopPlayback()
            if (productVideo.visibility == View.VISIBLE) playVideo.visibility = View.VISIBLE
            productVideo.visibility = View.GONE
        }
    }

    override fun onPause() {
        stopVideo()
        super.onPause()
    }
}
