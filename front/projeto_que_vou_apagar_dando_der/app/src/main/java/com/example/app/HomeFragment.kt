package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.FragmentHomeBinding
import com.example.app.feed.FeedAdapter
import com.example.app.favorites.FavoritesViewModel
import com.example.app.favorites.observeFavorites
import com.example.app.network.ApiClient
import com.example.app.network.PostResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private var feedCall: Call<List<PostResponse>>? = null
    private var feedAdapter: FeedAdapter? = null
    private lateinit var favorites: FavoritesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentHomeBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ui = binding ?: return
        feedAdapter = FeedAdapter(onProductClick = { productId ->
            // O ID pertence ao item clicado, nunca à posição global da lista.
            val args = Bundle().apply { putInt("product_id", productId) }
            findNavController().navigate(R.id.action_home_to_product, args)
        }, onFavoriteClick = { favorites.toggle(it) })
        favorites = observeFavorites { feedAdapter?.updateFavorites(it) }
        ui.feedList.layoutManager = LinearLayoutManager(requireContext())
        ui.feedList.adapter = feedAdapter
        ui.retryButton.setOnClickListener { loadFeed() }
        ui.btnNotifications.setOnClickListener { findNavController().navigate(R.id.nav_users) }
        ui.btnChatHome.setOnClickListener { findNavController().navigate(R.id.nav_users) }
        loadFeed()
    }

    private fun loadFeed() {
        if (feedCall != null) return
        binding?.apply {
            feedProgress.visibility = View.VISIBLE
            feedMessage.visibility = View.GONE
            retryButton.visibility = View.GONE
        }
        val call = ApiClient.feed.getFeed()
        feedCall = call
        call.enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                if (feedCall !== call) return
                feedCall = null
                val ui = binding ?: return
                ui.feedProgress.visibility = View.GONE
                val posts = response.body()
                if (!response.isSuccessful || posts == null) {
                    response.errorBody()?.close()
                    showMessage(R.string.feed_error, true)
                    return
                }
                feedAdapter?.submitList(posts)
                if (posts.isEmpty()) showMessage(R.string.feed_empty, false)
            }

            override fun onFailure(call: Call<List<PostResponse>>, error: Throwable) {
                if (feedCall !== call) return
                feedCall = null
                if (!call.isCanceled) showMessage(R.string.feed_error, true)
            }
        })
    }

    private fun showMessage(message: Int, retry: Boolean) {
        binding?.apply {
            feedProgress.visibility = View.GONE
            feedMessage.setText(message)
            feedMessage.visibility = View.VISIBLE
            retryButton.visibility = if (retry) View.VISIBLE else View.GONE
        }
    }

    override fun onPause() {
        feedAdapter?.stopVideos()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        favorites.load()
        loadFeed()
    }

    override fun onDestroyView() {
        val pending = feedCall
        feedCall = null
        pending?.cancel()
        feedAdapter?.stopVideos()
        binding?.feedList?.adapter = null
        feedAdapter = null
        binding = null
        super.onDestroyView()
    }
}
