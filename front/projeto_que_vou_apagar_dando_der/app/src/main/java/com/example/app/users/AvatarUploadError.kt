package com.example.app.users
import com.example.app.R
object AvatarUploadError {
    fun forStatus(status: Int): Int = when (status) {
        400, 415 -> R.string.avatar_invalid_image
        401 -> R.string.favorites_session_expired
        404, 405 -> R.string.avatar_service_outdated
        413 -> R.string.post_photo_too_large
        else -> R.string.profile_save_error
    }
}
