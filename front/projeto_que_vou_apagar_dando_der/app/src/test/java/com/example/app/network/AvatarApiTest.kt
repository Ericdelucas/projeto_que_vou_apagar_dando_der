package com.example.app.network
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class AvatarApiTest {
    @Test fun uploadsRawPhotoToAuthenticatedAvatarRoute() {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setBody("""{"id":1,"name":"Ana","role":"comprador","is_owner":true,"avatar_url":"https://example.com/a.jpg","media":[]}"""))
            server.start()
            val api = Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create()).build().create(UserApi::class.java)
            val photo = byteArrayOf(1,2,3)
            val response = api.uploadAvatar("Bearer test-token", photo.toRequestBody("image/jpeg".toMediaType())).execute()
            assertTrue(response.isSuccessful)
            assertEquals("https://example.com/a.jpg", response.body()?.avatarUrl)
            val req = server.takeRequest()
            assertEquals("/api/users/avatar", req.path)
            assertEquals("Bearer test-token", req.getHeader("Authorization"))
            assertEquals("image/jpeg", req.getHeader("Content-Type"))
            assertArrayEquals(photo, req.body.readByteArray())
        } finally { server.shutdown() }
    }
}
