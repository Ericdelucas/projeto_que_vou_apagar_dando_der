package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserApiTest {
    private fun api(server: MockWebServer): UserApi = Retrofit.Builder().baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build().create(UserApi::class.java)
    private val profile = """{"id":7,"name":"Ana","email":"ana@example.com","role":"comprador","avatar_url":null,"bio":"Meu perfil","is_owner":true,"posts_count":2,"followers_count":3,"following_count":4,"media":[{"media_id":"post:1","kind":"post","post_id":1,"product_id":42,"title":"Volante","image_url":null,"video_url":"https://example.com/p.mp4"}]}"""
    @Test fun searchEncodesTextAndCategoryAndReadsActivity() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{"id":7,"name":"Ana","role":"vendedor","avatar_url":null,"last_active_at":null,"activity_status":"recente","activity":[0,1,3,0,2,0,1,0,2,0,1,0,1,4],"activity_count":15}]"""))
            val user = api(server).search("Bearer usuario", "Ana São", "vendedor").execute().body()!!.single()
            assertEquals("recente", user.activityStatus)
            assertEquals(15, user.activityCount)
            assertEquals(listOf(0,1,3,0,2,0,1,0,2,0,1,0,1,4), user.activity)
            assertEquals("vendedor", user.role)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/users/search", request.requestUrl!!.encodedPath)
            assertEquals("Ana São", request.requestUrl!!.queryParameter("q"))
            assertEquals("vendedor", request.requestUrl!!.queryParameter("filter"))
            assertEquals("Bearer usuario", request.getHeader("Authorization"))
        } finally { server.shutdown() }
    }
    @Test fun profileAndUpdateReadMetricsAndMedia() {
        val server = MockWebServer(); server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody(profile))
            val own = api.me("Bearer usuario").execute().body()!!
            assertTrue(own.isOwner); assertEquals(2, own.postsCount)
            assertEquals(3, own.followersCount); assertEquals(4, own.followingCount)
            assertEquals("post:1", own.media.single().mediaId)
            assertNotNull(own.media.single().videoUrl)
            assertEquals("/api/users/me", server.takeRequest().path)
            server.enqueue(MockResponse().setBody(profile))
            api.update("Bearer usuario", UpdateProfileRequest("Ana", "ana@example.com", "", "Meu perfil")).execute()
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/users/profile", request.path)
            assertEquals("Bearer usuario", request.getHeader("Authorization"))
            assertEquals("""{"name":"Ana","email":"ana@example.com","avatar_url":"","bio":"Meu perfil"}""", request.body.readUtf8())
            server.enqueue(MockResponse().setBody(profile.replace("\"email\":\"ana@example.com\",", "").replace("\"is_owner\":true", "\"is_owner\":false")))
            val other = api.profile("Bearer usuario", 8).execute().body()!!
            assertFalse(other.isOwner); assertNull(other.email)
            assertEquals("/api/users/profile/8", server.takeRequest().path)
        } finally { server.shutdown() }
    }
    @Test fun emptySearchAndConflictingEmailStayDistinct() {
        val server = MockWebServer(); server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.search("Bearer usuario", "").execute().body()!!.isEmpty())
            server.enqueue(MockResponse().setResponseCode(409).setBody("{}"))
            val conflict = api.update("Bearer usuario", UpdateProfileRequest("Ana", "ana@example.com", "", "")).execute()
            assertEquals(409, conflict.code()); assertNull(conflict.body()); conflict.errorBody()?.close()
        } finally { server.shutdown() }
    }
}
