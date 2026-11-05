package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.TokenStore
import com.example.app.databinding.FragmentVenderBinding
import com.example.app.network.ApiClient
import com.example.app.network.SellerProduct
import com.example.app.seller.NewProductDialogFragment
import com.example.app.seller.SellerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VenderFragment : Fragment() {
    private var directoryCall: Call<List<com.example.app.network.UserSearchItem>>? = null
    private var directoryAdapter: com.example.app.users.UsersAdapter? = null
    private var binding: FragmentVenderBinding? = null
    private var productsCall: Call<List<SellerProduct>>? = null
    private var sellerAdapter: SellerAdapter? = null
    private var products = emptyList<SellerProduct>()
    private var selectedModel: String? = null
    private var selectedCategory: String? = null
    private var selectedPiece: String? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        selectedModel = state?.getString("model")
        selectedCategory = state?.getString("category")
        selectedPiece = state?.getString("piece")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentVenderBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        val ui = binding ?: return
        directoryAdapter = com.example.app.users.UsersAdapter { id ->
            findNavController().navigate(R.id.nav_user_profile, Bundle().apply { putInt("userId", id) })
        }
        ui.sellersList.layoutManager = LinearLayoutManager(requireContext())
        ui.sellersList.adapter = directoryAdapter
        ui.allSellers.setOnClickListener {
            findNavController().navigate(R.id.nav_users, Bundle().apply { putString("filter", "vendedor") })
        }
        ui.directoryMessage.setOnClickListener { loadDirectory() }
        loadDirectory()
        sellerAdapter = SellerAdapter { product ->
            MaterialAlertDialogBuilder(requireContext()).setTitle(product.title)
                .setMessage(getString(R.string.seller_requests_summary, product.requestsCount,
                    product.pendingRequestsCount, product.acceptedRequestsCount, product.rejectedRequestsCount))
                .setPositiveButton(android.R.string.ok, null).show()
        }
        ui.productsList.layoutManager = LinearLayoutManager(requireContext())
        ui.productsList.adapter = sellerAdapter
        ui.addProduct.setOnClickListener {
            if (childFragmentManager.findFragmentByTag("new_product") == null) {
                NewProductDialogFragment().show(childFragmentManager, "new_product")
            }
        }
        ui.btnChatVender.setOnClickListener { findNavController().navigate(R.id.nav_users) }
        ui.modelFilter.setOnClickListener { anchor ->
            showFilter(anchor, products.mapNotNull { it.model }.distinct().sorted(), selectedModel) {
                selectedModel = it; renderProducts()
            }
        }
        ui.categoryFilter.setOnClickListener { anchor ->
            showFilter(anchor, products.mapNotNull { it.category }.distinct().sorted(), selectedCategory) {
                selectedCategory = it; renderProducts()
            }
        }
        ui.pieceFilter.setOnClickListener { anchor ->
            showFilter(anchor, products.map { it.title }.distinct().sorted(), selectedPiece) {
                selectedPiece = it; renderProducts()
            }
        }
        childFragmentManager.setFragmentResultListener("seller_refresh", viewLifecycleOwner) { _, _ ->
            val previous = productsCall
            productsCall = null
            previous?.cancel()
            loadProducts()
        }
        loadProducts()
    }

    private fun loadDirectory() {
        val ui = binding ?: return
        if (directoryCall != null) return
        val token = TokenStore(requireContext()).getToken()
        if (token.isNullOrBlank()) {
            ui.directoryProgress.visibility = View.GONE
            ui.directoryMessage.setText(R.string.favorites_session_expired)
            ui.directoryMessage.visibility = View.VISIBLE
            return
        }
        ui.directoryProgress.visibility = View.VISIBLE
        ui.directoryMessage.visibility = View.GONE
        val call = ApiClient.users.search("Bearer $token", "", "vendedor")
        directoryCall = call
        call.enqueue(object : Callback<List<com.example.app.network.UserSearchItem>> {
            override fun onResponse(call: Call<List<com.example.app.network.UserSearchItem>>, response: Response<List<com.example.app.network.UserSearchItem>>) {
                if (directoryCall !== call) return
                directoryCall = null
                val ui = binding ?: return
                ui.directoryProgress.visibility = View.GONE
                val sellers = response.body()
                if (response.isSuccessful && sellers != null) {
                    directoryAdapter?.submitList(sellers)
                    ui.sellersList.visibility = if (sellers.isEmpty()) View.GONE else View.VISIBLE
                    ui.directoryMessage.setText(R.string.users_empty)
                    ui.directoryMessage.visibility = if (sellers.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    response.errorBody()?.close()
                    ui.directoryMessage.setText(if (response.code() == 401) R.string.favorites_session_expired else R.string.seller_directory_retry)
                    ui.directoryMessage.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<List<com.example.app.network.UserSearchItem>>, error: Throwable) {
                if (directoryCall !== call) return
                directoryCall = null
                val ui = binding ?: return
                if (call.isCanceled) return
                ui.directoryProgress.visibility = View.GONE
                ui.directoryMessage.setText(R.string.seller_directory_retry)
                ui.directoryMessage.visibility = View.VISIBLE
            }
        })
    }

    private fun showFilter(anchor: View, values: List<String>, selected: String?, apply: (String?) -> Unit) {
        val choices = listOf<String?>(null) + values
        PopupMenu(requireContext(), anchor).apply {
            choices.forEachIndexed { index, value ->
                menu.add(0, index, index, value ?: getString(R.string.marketplace_all)).apply {
                    isCheckable = true; isChecked = value == selected
                }
            }
            menu.setGroupCheckable(0, true, true)
            setOnMenuItemClickListener { apply(choices[it.itemId]); true }
            show()
        }
    }

    private fun loadProducts() {
        if (productsCall != null) return
        val ui = binding ?: return
        ui.addProduct.isEnabled = false
        ui.pieceFilter.isEnabled = false
        ui.modelFilter.isEnabled = false
        ui.categoryFilter.isEnabled = false
        ui.sellerProgress.visibility = View.VISIBLE
        ui.sellerMessage.visibility = View.GONE
        ui.retryButton.visibility = View.GONE
        ui.productsList.visibility = View.GONE
        val token = TokenStore(requireContext()).getToken()
        if (token.isNullOrBlank()) { showError(R.string.favorites_session_expired, login = true); return }
        val call = ApiClient.seller.getProducts("Bearer $token")
        productsCall = call
        call.enqueue(object : Callback<List<SellerProduct>> {
            override fun onResponse(call: Call<List<SellerProduct>>, response: Response<List<SellerProduct>>) {
                if (productsCall !== call) return
                productsCall = null
                val ui = binding ?: return
                val result = response.body()
                if (!response.isSuccessful || result == null) {
                    response.errorBody()?.close()
                    when (response.code()) {
                        401 -> {
                            val store = TokenStore(requireContext())
                            if (store.getToken() == token) store.clear()
                            showError(R.string.favorites_session_expired, login = true)
                        }
                        403 -> { ui.productFilters.visibility = View.GONE; showError(R.string.seller_forbidden, retry = false) }
                        else -> showError(R.string.seller_load_error)
                    }
                    return
                }
                ui.productFilters.visibility = View.VISIBLE
                products = result
                ui.addProduct.isEnabled = true
                ui.pieceFilter.isEnabled = true
                ui.modelFilter.isEnabled = true
                ui.categoryFilter.isEnabled = true
                renderProducts()
            }

            override fun onFailure(call: Call<List<SellerProduct>>, error: Throwable) {
                if (productsCall !== call) return
                productsCall = null
                if (!call.isCanceled) showError(R.string.seller_load_error)
            }
        })
    }

    private fun renderProducts() {
        val ui = binding ?: return
        val visible = com.example.app.seller.SellerFilters.apply(products, selectedModel, selectedCategory, selectedPiece)
        ui.modelFilter.text = selectedModel ?: getString(R.string.filter_model)
        ui.categoryFilter.text = selectedCategory ?: getString(R.string.filter_category)
        ui.pieceFilter.text = selectedPiece ?: getString(R.string.filter_piece)
        sellerAdapter?.submitList(visible)
        ui.sellerProgress.visibility = View.GONE
        ui.productsList.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        ui.sellerMessage.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        ui.sellerMessage.setText(if (products.isEmpty()) R.string.seller_empty else R.string.marketplace_empty)
        ui.retryButton.visibility = View.GONE
    }

    private fun showError(message: Int, retry: Boolean = true, login: Boolean = false) {
        binding?.apply {
            sellerProgress.visibility = View.GONE
            productsList.visibility = View.GONE
            sellerMessage.setText(message)
            sellerMessage.visibility = View.VISIBLE
            retryButton.visibility = if (retry || login) View.VISIBLE else View.GONE
            retryButton.setText(if (login) R.string.login_enter else R.string.feed_retry)
            retryButton.setOnClickListener {
                if (login) findNavController().navigate(R.id.nav_login, null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
                else loadProducts()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("model", selectedModel)
        outState.putString("category", selectedCategory)
        outState.putString("piece", selectedPiece)
    }

    override fun onDestroyView() {
        val directory = directoryCall
        directoryCall = null
        directory?.cancel()
        binding?.sellersList?.adapter = null
        directoryAdapter = null
        val call = productsCall
        productsCall = null
        call?.cancel()
        binding?.productsList?.adapter = null
        sellerAdapter = null
        products = emptyList()
        binding = null
        super.onDestroyView()
    }
}
