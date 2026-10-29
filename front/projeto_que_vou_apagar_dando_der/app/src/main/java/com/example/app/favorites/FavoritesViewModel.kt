package com.example.app.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.FavoriteItem
import com.example.app.network.FavoriteRequest
import com.example.app.network.FavoriteResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class FavoritesState(
    val items: List<FavoriteItem> = emptyList(),
    val productIds: Set<Int> = emptySet(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val pendingProductId: Int? = null,
    val error: Int? = null,
    val authRequired: Boolean = false,
) {
    val canToggle: Boolean get() = loaded && !loading && pendingProductId == null && error == null && !authRequired
}

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val tokens = TokenStore(application)
    private var sessionToken: String? = null
    private var listCall: Call<List<FavoriteItem>>? = null
    private var updateCall: Call<FavoriteResult>? = null
    private val mutableState = MutableLiveData(FavoritesState())
    val state: LiveData<FavoritesState> = mutableState
    private val mutableMessage = MutableLiveData<Int?>()
    val message: LiveData<Int?> = mutableMessage
    private val current: FavoritesState get() = mutableState.value ?: FavoritesState()

    private fun syncSession(): String? {
        val token = tokens.getToken()
        if (token != sessionToken) {
            cancelRequests()
            sessionToken = token
            mutableState.value = FavoritesState(authRequired = token.isNullOrBlank())
        }
        if (token.isNullOrBlank()) {
            mutableState.value = FavoritesState(authRequired = true, error = R.string.favorites_session_expired)
            return null
        }
        return token
    }

    fun load() {
        val token = syncSession() ?: return
        if (listCall != null || updateCall != null) return
        mutableState.value = current.copy(loading = true, error = null)
        val call = ApiClient.favorites.getFavorites("Bearer $token")
        listCall = call
        call.enqueue(object : Callback<List<FavoriteItem>> {
            override fun onResponse(call: Call<List<FavoriteItem>>, response: Response<List<FavoriteItem>>) {
                if (listCall !== call) return
                listCall = null
                val items = response.body()
                if (response.code() == 401) {
                    response.errorBody()?.close()
                    expireSession()
                } else if (response.isSuccessful && items != null) {
                    mutableState.value = FavoritesState(items, items.map { it.productId }.toSet(), loaded = true)
                } else {
                    response.errorBody()?.close()
                    failLoad()
                }
            }

            override fun onFailure(call: Call<List<FavoriteItem>>, error: Throwable) {
                if (listCall !== call) return
                listCall = null
                if (!call.isCanceled) failLoad()
            }
        })
    }

    fun toggle(productId: Int) {
        val token = syncSession() ?: run {
            mutableMessage.value = R.string.favorites_session_expired
            return
        }
        if (!current.canToggle || productId <= 0) return
        mutableState.value = current.copy(pendingProductId = productId)
        val desired = productId !in current.productIds
        val call = ApiClient.favorites.setFavorite("Bearer $token", FavoriteRequest(productId, desired))
        updateCall = call
        call.enqueue(object : Callback<FavoriteResult> {
            override fun onResponse(call: Call<FavoriteResult>, response: Response<FavoriteResult>) {
                if (updateCall !== call) return
                updateCall = null
                val result = response.body()
                if (response.code() == 401) {
                    response.errorBody()?.close()
                    expireSession()
                    return
                }
                if (!response.isSuccessful || result == null || result.productId != productId) {
                    response.errorBody()?.close()
                    failUpdate()
                    return
                }
                val ids = current.productIds.toMutableSet()
                if (result.isFavorite) ids.add(productId) else ids.remove(productId)
                mutableState.value = current.copy(
                    productIds = ids,
                    items = if (result.isFavorite) current.items else current.items.filterNot { it.productId == productId },
                    pendingProductId = null,
                )
                if (result.isFavorite) load() // Carrega também o autor e a data para a tela de Salvos.
            }

            override fun onFailure(call: Call<FavoriteResult>, error: Throwable) {
                if (updateCall !== call) return
                updateCall = null
                if (!call.isCanceled) failUpdate()
            }
        })
    }

    private fun failLoad() {
        mutableState.value = current.copy(loading = false, error = R.string.favorites_load_error)
        mutableMessage.value = R.string.favorites_load_error
    }

    private fun failUpdate() {
        mutableState.value = current.copy(pendingProductId = null)
        mutableMessage.value = R.string.favorites_update_error
        // A resposta pode ter se perdido após o commit. Reconsulta em vez de repetir o toggle.
        load()
    }

    private fun expireSession() {
        if (tokens.getToken() == sessionToken) tokens.clear()
        cancelRequests()
        sessionToken = null
        mutableState.value = FavoritesState(authRequired = true, error = R.string.favorites_session_expired)
        mutableMessage.value = R.string.favorites_session_expired
    }

    fun consumeMessage() { mutableMessage.value = null }

    private fun cancelRequests() {
        val list = listCall
        val update = updateCall
        listCall = null
        updateCall = null
        list?.cancel()
        update?.cancel()
    }

    override fun onCleared() {
        cancelRequests()
        super.onCleared()
    }
}
