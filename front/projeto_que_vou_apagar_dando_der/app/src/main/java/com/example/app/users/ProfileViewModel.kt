package com.example.app.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.UpdateProfileRequest
import com.example.app.network.UserProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ProfileState(
    val profile: UserProfile? = null, val loading: Boolean = false, val saving: Boolean = false,
    val error: Int? = null, val saveError: Int? = null, val savedVersion: Int = 0,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val photoWorker = java.util.concurrent.Executors.newSingleThreadExecutor()
    @Volatile private var cleared = false
    private val tokens = TokenStore(application)
    private var request: Call<UserProfile>? = null
    private var update: Call<UserProfile>? = null
    private var session: String? = null
    private val mutableState = MutableLiveData(ProfileState())
    val state: LiveData<ProfileState> = mutableState
    private val current: ProfileState get() = mutableState.value ?: ProfileState()

    fun load(userId: Int?) {
        val token = tokens.getToken()
        if (token != session) {
            cancelRequests()
            session = token
            mutableState.value = ProfileState()
        }
        if (token.isNullOrBlank()) { expire(); return }
        if (current.saving) return
        val previous = request
        request = null
        previous?.cancel()
        mutableState.value = current.copy(loading = true, error = null)
        val call = if (userId == null) ApiClient.users.me("Bearer $token")
            else ApiClient.users.profile("Bearer $token", userId)
        request = call
        call.enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (request !== call) return
                request = null
                val profile = response.body()
                if (response.isSuccessful && profile != null) {
                    mutableState.value = current.copy(profile = profile, loading = false, error = null)
                } else {
                    response.errorBody()?.close()
                    if (response.code() == 401) expire()
                    else mutableState.value = current.copy(loading = false,
                        error = if (response.code() == 404) R.string.profile_not_found else R.string.profile_error)
                }
            }
            override fun onFailure(call: Call<UserProfile>, error: Throwable) {
                if (request !== call) return
                request = null
                if (!call.isCanceled) mutableState.value = current.copy(loading = false, error = R.string.profile_error)
            }
        })
    }

    fun clearSaveError() { mutableState.value = current.copy(saveError = null) }

    fun save(body: UpdateProfileRequest, photo: android.net.Uri? = null) {
        if (photo != null) { uploadPhoto(body, photo); return }
        if (current.saving || current.profile?.isOwner != true) return
        val token = tokens.getToken()
        if (token.isNullOrBlank() || token != session) { expire(); return }
        val previous = request
        request = null
        previous?.cancel()
        mutableState.value = current.copy(saving = true, loading = false, saveError = null)
        val call = ApiClient.users.update("Bearer $token", body)
        update = call
        call.enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (update !== call) return
                update = null
                val profile = response.body()
                if (response.isSuccessful && profile != null) {
                    mutableState.value = current.copy(profile = profile, saving = false, error = null,
                        saveError = null, savedVersion = current.savedVersion + 1)
                } else {
                    response.errorBody()?.close()
                    if (response.code() == 401) expire()
                    else mutableState.value = current.copy(saving = false, saveError = when (response.code()) {
                        409 -> R.string.profile_email_used
                        400 -> R.string.profile_invalid
                        else -> R.string.profile_save_error
                    })
                }
            }
            override fun onFailure(call: Call<UserProfile>, error: Throwable) {
                if (update !== call) return
                update = null
                if (!call.isCanceled) mutableState.value = current.copy(saving = false, saveError = R.string.profile_save_error)
            }
        })
    }

    private fun uploadPhoto(body: UpdateProfileRequest, photo: android.net.Uri) {
        if (current.saving || current.profile?.isOwner != true) return
        val token = tokens.getToken()
        if (token.isNullOrBlank() || token != session) { expire(); return }
        val loading = request; request = null; loading?.cancel()
        mutableState.value = current.copy(saving = true, loading = false, saveError = null)
        photoWorker.execute {
            try {
                val bytes = PhotoEncoder.encode(getApplication(), photo)
                if (cleared) return@execute
                val call = ApiClient.users.uploadAvatar("Bearer $token", okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), bytes))
                update = call
                if (cleared) { call.cancel(); return@execute }
                call.enqueue(object : Callback<UserProfile> {
                    override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                        if (update !== call || cleared) return
                        update = null
                        val user = response.body()
                        if (response.isSuccessful && user != null) {
                            mutableState.value = current.copy(profile = user, saving = false)
                            save(body.copy(avatar_url = user.avatarUrl.orEmpty()))
                        } else {
                            response.errorBody()?.close()
                            if (response.code() == 401) expire()
                            else mutableState.value = current.copy(saving = false, saveError = R.string.profile_save_error)
                        }
                    }
                    override fun onFailure(call: Call<UserProfile>, error: Throwable) {
                        if (update !== call || cleared) return
                        update = null
                        if (!call.isCanceled) mutableState.value = current.copy(saving = false, saveError = R.string.profile_save_error)
                    }
                })
            } catch (_: Exception) {
                if (!cleared) mutableState.postValue(current.copy(saving = false, saveError = R.string.post_photo_read_error))
            }
        }
    }

    private fun expire() {
        if (tokens.getToken() == session) tokens.clear()
        cancelRequests()
        mutableState.value = ProfileState(error = R.string.favorites_session_expired,
            saveError = R.string.favorites_session_expired)
    }
    private fun cancelRequests() {
        val first = request; val second = update
        request = null; update = null
        first?.cancel(); second?.cancel()
    }
    override fun onCleared() { cleared = true; photoWorker.shutdownNow(); cancelRequests(); super.onCleared() }
}
