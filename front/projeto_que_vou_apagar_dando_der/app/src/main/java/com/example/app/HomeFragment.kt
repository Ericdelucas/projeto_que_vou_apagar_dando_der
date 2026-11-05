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
    private var posts = emptyList<PostResponse>()
    private var modelFilter: String? = null
    private var categoryFilter: String? = null
    private var tagFilter: String? = null
    private var query = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        modelFilter = state?.getString("model")
        categoryFilter = state?.getString("category")
        tagFilter = state?.getString("tag")
        query = state?.getString("query").orEmpty()
    }

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
        ui.feedSearch.setText(query)
        ui.feedSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString(); renderPosts() }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        ui.modelFilter.setOnClickListener { choose(it, posts.mapNotNull { post -> post.product?.model }, modelFilter) { modelFilter = it; renderPosts() } }
        ui.categoryFilter.setOnClickListener { choose(it, posts.mapNotNull { post -> post.product?.category }, categoryFilter) { categoryFilter = it; renderPosts() } }
        ui.tagFilter.setOnClickListener { choose(it, posts.mapNotNull { post -> post.product?.title }, tagFilter) { tagFilter = it; renderPosts() } }
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
                this@HomeFragment.posts = posts
                renderPosts()
            }

            override fun onFailure(call: Call<List<PostResponse>>, error: Throwable) {
                if (feedCall !== call) return
                feedCall = null
                if (!call.isCanceled) showMessage(R.string.feed_error, true)
            }
        })
    }

    private fun choose(anchor: View, values: List<String>, selected: String?, apply: (String?) -> Unit) {
        val choices = listOf<String?>(null) + values.distinct().sorted()
        androidx.appcompat.widget.PopupMenu(requireContext(), anchor).apply {
            choices.forEachIndexed { index, value -> menu.add(0, index, index, value ?: getString(R.string.marketplace_all)).apply { isCheckable = true; isChecked = value == selected } }
            menu.setGroupCheckable(0, true, true)
            setOnMenuItemClickListener { apply(choices[it.itemId]); true }
            show()
        }
    }

    private fun renderPosts() {
        val ui = binding ?: return
        val visible = com.example.app.feed.FeedFilters.apply(posts, modelFilter, categoryFilter, tagFilter, query)
        ui.modelFilter.text = modelFilter ?: getString(R.string.filter_model)
        ui.categoryFilter.text = categoryFilter ?: getString(R.string.filter_category)
        ui.tagFilter.text = tagFilter ?: getString(R.string.filter_piece)
        feedAdapter?.submitList(visible)
        ui.feedMessage.visibility = if (visible.isEmpty() && feedCall == null) View.VISIBLE else View.GONE
        ui.feedMessage.setText(if (posts.isEmpty()) R.string.feed_empty else R.string.marketplace_empty)
        ui.retryButton.visibility = View.GONE
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("model", modelFilter); out.putString("category", categoryFilter)
        out.putString("tag", tagFilter); out.putString("query", query)
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
