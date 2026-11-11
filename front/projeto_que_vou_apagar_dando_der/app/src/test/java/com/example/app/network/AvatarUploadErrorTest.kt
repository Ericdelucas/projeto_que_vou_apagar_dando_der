package com.example.app.network
import com.example.app.R
import com.example.app.users.AvatarUploadError
import org.junit.Assert.*
import org.junit.Test
class AvatarUploadErrorTest {
    @Test fun distinguishesMissingDeploymentFromInvalidImageAndOversizedUpload() {
        assertEquals(R.string.avatar_service_outdated, AvatarUploadError.forStatus(404))
        assertEquals(R.string.avatar_service_outdated, AvatarUploadError.forStatus(405))
        assertEquals(R.string.avatar_invalid_image, AvatarUploadError.forStatus(400))
        assertEquals(R.string.post_photo_too_large, AvatarUploadError.forStatus(413))
        assertEquals(R.string.favorites_session_expired, AvatarUploadError.forStatus(401))
        assertEquals(R.string.profile_save_error, AvatarUploadError.forStatus(500))
    }
}
