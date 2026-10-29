package com.example.app.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.BlockedUser
import com.example.app.network.BlockUserRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class SettingsState(
    val items: List<BlockedUser> = emptyList(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val deleting: Boolean = false,
    val error: Int? = null,
    val authRequired: Boolean = false,
    val accountDeleted: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val tokens = TokenStore(application)
    private var session: String? = null
    private var request: Call<*>? = null
    private val mutable = MutableLiveData(SettingsState())
    val state: LiveData<SettingsState> = mutable
    private val current get() = mutable.value ?: SettingsState()

    private fun token(): String? {
        val value = tokens.getToken()
        if (value != session) {
            cancel()
            session = value
            mutable.value = SettingsState()
        }
        if (value.isNullOrBlank()) { expire(value); return null }
        return value
    }

    fun load() {
        val token = token() ?: return
        if (current.busy) return
        mutable.value = current.copy(busy = true, error = null)
        execute(ApiClient.settings.blocked("Bearer $token"), token) { response ->
            val items = response.body()
            if (items == null) fail(R.string.settings_error)
            else mutable.value = current.copy(items = items, loaded = true, busy = false, error = null)
        }
    }

    fun block(userId: Int) {
        val token = token() ?: return
        if (current.busy) return
        mutable.value = current.copy(busy = true, error = null)
        execute(ApiClient.settings.block("Bearer $token", BlockUserRequest(userId)), token) { response ->
            val block = response.body()
            if (block == null) fail(R.string.settings_error)
            else mutable.value = current.copy(items = listOf(block) + current.items.filter { it.id != block.id },
                busy = false, error = null)
        }
    }

    fun unblock(id: Int) {
        val token = token() ?: return
        if (current.busy) return
        mutable.value = current.copy(busy = true, error = null)
        execute(ApiClient.settings.unblock("Bearer $token", id), token) {
            mutable.value = current.copy(items = current.items.filter { it.id != id }, busy = false, error = null)
        }
    }

    fun deleteAccount() {
        val token = token() ?: return
        if (current.busy) return
        mutable.value = current.copy(busy = true, deleting = true, error = null)
        execute(ApiClient.settings.deleteAccount("Bearer $token"), token) {
            // Só encerra a sessão depois da confirmação do servidor.
            if (tokens.getToken() == token) tokens.clear()
            mutable.value = SettingsState(accountDeleted = true)
        }
    }

    fun clearError() { mutable.value = current.copy(error = null) }

    fun expire(token: String?) {
        if (tokens.getToken() == token) tokens.clear()
        cancel()
        mutable.value = SettingsState(authRequired = true, error = R.string.favorites_session_expired)
    }

    private fun fail(error: Int) {
        mutable.value = current.copy(busy = false, deleting = false, error = error)
    }

    private fun <T> execute(call: Call<T>, token: String, success: (Response<T>) -> Unit) {
        request = call
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (request !== call) return
                request = null
                if (tokens.getToken() != token) { response.errorBody()?.close(); this@SettingsViewModel.token(); return }
                if (response.isSuccessful) success(response)
                else {
                    response.errorBody()?.close()
                    when (response.code()) {
                        401 -> expire(token)
                        400 -> fail(R.string.settings_invalid_block)
                        404 -> fail(R.string.settings_not_found)
                        else -> fail(R.string.settings_error)
                    }
                }
            }
            override fun onFailure(call: Call<T>, error: Throwable) {
                if (request !== call) return
                request = null
                if (tokens.getToken() != token) { this@SettingsViewModel.token(); return }
                if (!call.isCanceled) fail(R.string.settings_error)
            }
        })
    }

    private fun cancel() { val call = request; request = null; call?.cancel() }
    override fun onCleared() { cancel(); super.onCleared() }
}
