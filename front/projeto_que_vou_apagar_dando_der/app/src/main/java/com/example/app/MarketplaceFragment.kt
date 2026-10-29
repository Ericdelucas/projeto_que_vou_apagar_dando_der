package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.databinding.FragmentMarketplaceBinding
import com.example.app.marketplace.MarketplaceAdapter
import com.example.app.favorites.FavoritesViewModel
import com.example.app.favorites.observeFavorites
import com.example.app.network.ApiClient
import com.example.app.network.MarketplaceFilters
import com.example.app.network.ProductTag
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MarketplaceFragment : Fragment() {
    private var binding: FragmentMarketplaceBinding? = null
    private var productsCall: Call<List<ProductTag>>? = null
    private var filtersCall: Call<MarketplaceFilters>? = null
    private var productsAdapter: MarketplaceAdapter? = null
    private var options: MarketplaceFilters? = null
    private var selectedModel: String? = null
    private var selectedCategory: String? = null
    private var selectedLocation: String? = null
    private lateinit var favorites: FavoritesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedModel = savedInstanceState?.getString("model")
        selectedCategory = savedInstanceState?.getString("category")
        selectedLocation = savedInstanceState?.getString("location")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentMarketplaceBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ui = binding ?: return
        productsAdapter = MarketplaceAdapter(onProductClick = { productId ->
            findNavController().navigate(R.id.action_marketplace_to_product,
                Bundle().apply { putInt("product_id", productId) })
        }, onFavoriteClick = { favorites.toggle(it) })
        favorites = observeFavorites { productsAdapter?.updateFavorites(it) }
        ui.productsGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        ui.productsGrid.adapter = productsAdapter
        ui.btnBackMarketplace.setOnClickListener {
            if (!findNavController().popBackStack()) findNavController().navigate(R.id.nav_home)
        }
        ui.btnChatMarketplace.setOnClickListener { findNavController().navigate(R.id.nav_users) }
        ui.retryButton.setOnClickListener { loadProducts() }
        // Os próprios pills do desenho abrem os dropdowns, sem adicionar outra barra.
        ui.filterPill1.setOnClickListener { anchor ->
            showDropdown(anchor, options?.models.orEmpty(), selectedModel) {
                selectedModel = it; updateLabels(); loadProducts()
            }
        }
        ui.filterPill2.setOnClickListener { anchor ->
            showDropdown(anchor, options?.locations.orEmpty(), selectedLocation) {
                selectedLocation = it; updateLabels(); loadProducts()
            }
        }
        ui.filterPill3.setOnClickListener { anchor ->
            showDropdown(anchor, options?.categories.orEmpty(), selectedCategory) {
                selectedCategory = it; updateLabels(); loadProducts()
            }
        }
        updateLabels()
        setFiltersEnabled(options != null)
        if (options == null) loadFilters()
        loadProducts()
    }

    private fun showDropdown(anchor: View, values: List<String>, selected: String?, onSelect: (String?) -> Unit) {
        val choices = listOf<String?>(null) + values
        PopupMenu(requireContext(), anchor).apply {
            choices.forEachIndexed { index, value ->
                menu.add(0, index, index, value ?: getString(R.string.marketplace_all)).apply {
                    isCheckable = true
                    isChecked = value == selected
                }
            }
            menu.setGroupCheckable(0, true, true)
            setOnMenuItemClickListener { item ->
                val value = choices[item.itemId]
                if (value != selected) onSelect(value)
                true
            }
            show()
        }
    }

    private fun updateLabels() {
        binding?.apply {
            txtTipage.text = selectedModel?.let { getString(R.string.marketplace_model_value, it) }
                ?: getString(R.string.tipage_modelo)
            txtCategoria.text = selectedCategory?.let { getString(R.string.marketplace_category_value, it) }
                ?: getString(R.string.categoria_ano)
            txtLocal.text = selectedLocation?.let { getString(R.string.marketplace_location_value, it) }
                ?: getString(R.string.marketplace_location)
        }
    }

    private fun setFiltersEnabled(enabled: Boolean) {
        binding?.apply {
            listOf(filterPill1, filterPill2, filterPill3).forEach {
                it.isEnabled = enabled
                it.alpha = if (enabled) 1f else 0.5f
            }
        }
    }

    private fun loadFilters() {
        if (filtersCall != null) return
        val call = ApiClient.marketplace.getFilters()
        filtersCall = call
        call.enqueue(object : Callback<MarketplaceFilters> {
            override fun onResponse(call: Call<MarketplaceFilters>, response: Response<MarketplaceFilters>) {
                if (filtersCall !== call) return
                filtersCall = null
                if (binding == null) return
                val result = response.body()
                if (response.isSuccessful && result != null) {
                    options = result
                    setFiltersEnabled(true)
                } else {
                    response.errorBody()?.close()
                    showFiltersError()
                }
            }

            override fun onFailure(call: Call<MarketplaceFilters>, error: Throwable) {
                if (filtersCall !== call) return
                filtersCall = null
                if (!call.isCanceled) showFiltersError()
            }
        })
    }

    private fun showFiltersError() {
        binding?.let {
            Snackbar.make(it.root, R.string.marketplace_filters_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.feed_retry) { loadFilters() }.show()
        }
    }

    private fun loadProducts() {
        // Cancela a seleção anterior e ignora callbacks antigos para evitar resultados trocados.
        val previous = productsCall
        productsCall = null
        previous?.cancel()
        binding?.apply {
            marketplaceProgress.visibility = View.VISIBLE
            productsGrid.visibility = View.GONE
            marketplaceMessage.visibility = View.GONE
            retryButton.visibility = View.GONE
        }
        val call = ApiClient.marketplace.getProducts(selectedModel, selectedCategory, selectedLocation)
        productsCall = call
        call.enqueue(object : Callback<List<ProductTag>> {
            override fun onResponse(call: Call<List<ProductTag>>, response: Response<List<ProductTag>>) {
                if (productsCall !== call) return
                productsCall = null
                val ui = binding ?: return
                ui.marketplaceProgress.visibility = View.GONE
                val products = response.body()
                if (!response.isSuccessful || products == null) {
                    response.errorBody()?.close()
                    showMessage(R.string.marketplace_error, true)
                    return
                }
                productsAdapter?.submitList(products)
                if (products.isEmpty()) showMessage(R.string.marketplace_empty, false)
                else ui.productsGrid.visibility = View.VISIBLE
            }

            override fun onFailure(call: Call<List<ProductTag>>, error: Throwable) {
                if (productsCall !== call) return
                productsCall = null
                if (!call.isCanceled) showMessage(R.string.marketplace_error, true)
            }
        })
    }

    private fun showMessage(message: Int, retry: Boolean) {
        binding?.apply {
            marketplaceProgress.visibility = View.GONE
            productsGrid.visibility = View.GONE
            marketplaceMessage.setText(message)
            marketplaceMessage.visibility = View.VISIBLE
            retryButton.visibility = if (retry) View.VISIBLE else View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("model", selectedModel)
        outState.putString("category", selectedCategory)
        outState.putString("location", selectedLocation)
    }

    override fun onResume() {
        super.onResume()
        favorites.load()
    }

    override fun onDestroyView() {
        val products = productsCall
        val filters = filtersCall
        productsCall = null
        filtersCall = null
        products?.cancel()
        filters?.cancel()
        binding?.productsGrid?.adapter = null
        productsAdapter = null
        binding = null
        super.onDestroyView()
    }
}
