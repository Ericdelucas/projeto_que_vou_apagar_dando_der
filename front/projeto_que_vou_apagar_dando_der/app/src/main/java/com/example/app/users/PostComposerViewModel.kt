package com.example.app.users

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.CreatedPost
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

data class ComposerState(val photo: Uri? = null, val loading: Boolean = false,
    val error: Int? = null, val published: Boolean = false)

class PostComposerViewModel(app: Application) : AndroidViewModel(app) {
    val state = MutableLiveData(ComposerState())
    private val worker = Executors.newSingleThreadExecutor()
    private var pending: Call<CreatedPost>? = null
    @Volatile private var cleared = false
    fun select(uri: Uri) { if (state.value?.loading != true) state.value = ComposerState(photo = uri) }
    fun publish(caption: String) {
        val current = state.value ?: return
        if (current.loading || current.published) return
        val photo = current.photo
        if (photo == null) { state.value = current.copy(error = R.string.post_choose_first); return }
        val token = TokenStore(getApplication()).getToken()
        if (token.isNullOrBlank()) { state.value = current.copy(error = R.string.favorites_session_expired); return }
        state.value = current.copy(loading = true, error = null)
        worker.execute {
            try {
                val bytes = PhotoEncoder.encode(getApplication(), photo)
                if (cleared) return@execute
                val call = ApiClient.feed.publishPhoto("Bearer $token", caption, RequestBody.create(MediaType.parse("image/jpeg"), bytes))
                pending = call
                if (cleared) { call.cancel(); return@execute }
                call.enqueue(object : Callback<CreatedPost> {
                    override fun onResponse(call: Call<CreatedPost>, response: Response<CreatedPost>) {
                        pending = null
                        if (cleared) return
                        if (response.isSuccessful && response.body() != null)
                            state.value = current.copy(published = true)
                        else {
                            response.errorBody()?.close()
                            state.value = current.copy(error = when (response.code()) {
                                401 -> R.string.favorites_session_expired
                                413 -> R.string.post_photo_too_large
                                else -> R.string.new_post_error
                            })
                        }
                    }
                    override fun onFailure(call: Call<CreatedPost>, error: Throwable) {
                        pending = null
                        if (!cleared && !call.isCanceled) state.value = current.copy(error = R.string.new_post_error)
                    }
                })
            } catch (_: Exception) {
                if (!cleared) state.postValue(current.copy(error = R.string.post_photo_read_error))
            }
        }
    }
    override fun onCleared() {
        cleared = true; pending?.cancel(); worker.shutdownNow()
        super.onCleared()
    }
}
