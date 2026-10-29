package com.example.app.seller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.NewSellerProduct
import com.example.app.network.SellerProduct
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ProductCreationState(
    val saving: Boolean = false,
    val productId: Int? = null,
    val error: Int? = null,
    val mustClose: Boolean = false,
)

class NewProductViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableLiveData(ProductCreationState())
    val state: LiveData<ProductCreationState> = mutableState
    private var call: Call<SellerProduct>? = null

    fun create(product: NewSellerProduct) {
        val current = mutableState.value ?: ProductCreationState()
        if (current.saving || current.productId != null || current.mustClose) return
        val store = TokenStore(getApplication())
        val token = store.getToken()
        if (token.isNullOrBlank()) {
            mutableState.value = ProductCreationState(error = R.string.favorites_session_expired, mustClose = true)
            return
        }
        mutableState.value = ProductCreationState(saving = true)
        val pending = ApiClient.seller.createProduct("Bearer $token", product)
        call = pending
        pending.enqueue(object : Callback<SellerProduct> {
            override fun onResponse(call: Call<SellerProduct>, response: Response<SellerProduct>) {
                if (this@NewProductViewModel.call !== call) return
                this@NewProductViewModel.call = null
                val created = response.body()
                if (response.code() == 201 && created != null) {
                    mutableState.value = ProductCreationState(productId = created.id)
                } else {
                    response.errorBody()?.close()
                    val error = when (response.code()) {
                        400 -> R.string.seller_invalid_data
                        401 -> R.string.favorites_session_expired
                        403 -> R.string.seller_forbidden
                        else -> R.string.seller_create_uncertain
                    }
                    if (response.code() == 401 && store.getToken() == token) store.clear()
                    mutableState.value = ProductCreationState(error = error, mustClose = response.code() != 400)
                }
            }

            override fun onFailure(call: Call<SellerProduct>, error: Throwable) {
                if (this@NewProductViewModel.call !== call) return
                this@NewProductViewModel.call = null
                if (!call.isCanceled) mutableState.value = ProductCreationState(
                    error = R.string.seller_create_uncertain, mustClose = true)
            }
        })
    }

    override fun onCleared() {
        val pending = call
        call = null
        pending?.cancel()
        super.onCleared()
    }
}
