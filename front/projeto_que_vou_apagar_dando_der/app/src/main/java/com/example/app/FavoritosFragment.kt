package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.FragmentFavoritosBinding
import com.example.app.favorites.FavoritesAdapter
import com.example.app.favorites.FavoritesState
import com.example.app.favorites.FavoritesViewModel
import com.example.app.favorites.observeFavorites
import com.example.app.favorites.openFavoritesLogin

class FavoritosFragment : Fragment() {
    private var binding: FragmentFavoritosBinding? = null
    private var favoritesAdapter: FavoritesAdapter? = null
    private lateinit var favorites: FavoritesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentFavoritosBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ui = binding ?: return
        favoritesAdapter = FavoritesAdapter(
            onProductClick = { id ->
                findNavController().navigate(R.id.action_favorites_to_product,
                    Bundle().apply { putInt("product_id", id) })
            },
            onFavoriteClick = { favorites.toggle(it) },
        )
        ui.favoritesList.layoutManager = LinearLayoutManager(requireContext())
        ui.favoritesList.adapter = favoritesAdapter
        ui.btnBackFavoritos.setOnClickListener { findNavController().navigateUp() }
        favorites = observeFavorites { render(it) }
    }

    private fun render(state: FavoritesState) {
        val ui = binding ?: return
        favoritesAdapter?.updateState(state)
        ui.favoritesProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
        val message = when {
            state.authRequired -> R.string.favorites_session_expired
            state.error != null -> state.error
            state.loaded && state.items.isEmpty() -> R.string.favorites_empty
            else -> null
        }
        ui.favoritesMessage.visibility = if (message == null) View.GONE else View.VISIBLE
        message?.let { ui.favoritesMessage.setText(it) }
        ui.favoritesList.visibility = if (message == null) View.VISIBLE else View.GONE
        ui.retryButton.visibility = if (state.error != null || state.authRequired) View.VISIBLE else View.GONE
        ui.retryButton.setText(if (state.authRequired) R.string.login_enter else R.string.feed_retry)
        ui.retryButton.setOnClickListener {
            if (state.authRequired) openFavoritesLogin() else favorites.load()
        }
    }

    override fun onResume() {
        super.onResume()
        favorites.load()
    }

    override fun onDestroyView() {
        binding?.favoritesList?.adapter = null
        favoritesAdapter = null
        binding = null
        super.onDestroyView()
    }
}
