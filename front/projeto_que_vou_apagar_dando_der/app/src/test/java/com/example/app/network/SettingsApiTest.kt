package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SettingsApiTest {
    private fun api(server: MockWebServer): SettingsApi = Retrofit.Builder().baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build().create(SettingsApi::class.java)

    private val block = """{"id":31,"blocked_user_id":8,"created_at":"2026-10-25T12:00:00Z","user":{"id":8,"name":"Ana","role":"vendedor","avatar_url":null}}"""

    @Test fun loadsBlockedUsersAndKeepsBlockIdDistinctFromUserId() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("[$block]"))
            val item = api(server).blocked("Bearer teste").execute().body()!!.single()
            assertEquals(31, item.id); assertEquals(8, item.blockedUserId)
            assertEquals("Ana", item.user.name); assertNull(item.user.avatarUrl)
            val request = server.takeRequest()
            assertEquals("GET", request.method); assertEquals("/api/settings/blocked", request.path)
            assertEquals("Bearer teste", request.getHeader("Authorization"))
        } finally { server.shutdown() }
    }

    @Test fun blocksWithOnlyTheTargetAndUnblocksUsingRelationshipId() {
        val server = MockWebServer(); server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody(block))
            val saved = api.block("Bearer teste", BlockUserRequest(8)).execute().body()!!
            val post = server.takeRequest()
            assertEquals("POST", post.method); assertEquals("/api/settings/block", post.path)
            assertEquals("Bearer teste", post.getHeader("Authorization"))
            assertEquals("""{"blocked_user_id":8}""", post.body.readUtf8())
            server.enqueue(MockResponse().setResponseCode(204))
            val removed = api.unblock("Bearer teste", saved.id).execute()
            assertTrue(removed.isSuccessful); assertNull(removed.body())
            val delete = server.takeRequest()
            assertEquals("DELETE", delete.method); assertEquals("/api/settings/block/31", delete.path)
            assertEquals("Bearer teste", delete.getHeader("Authorization"))
        } finally { server.shutdown() }
    }

    @Test fun accountDeletionUsesAuthenticatedIdentityWithoutBody() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(204))
            val response = api(server).deleteAccount("Bearer teste").execute()
            assertTrue(response.isSuccessful); assertNull(response.body())
            val request = server.takeRequest()
            assertEquals("DELETE", request.method); assertEquals("/api/settings/account", request.path)
            assertEquals("Bearer teste", request.getHeader("Authorization")); assertEquals(0L, request.bodySize)
        } finally { server.shutdown() }
    }

    @Test fun emptyListExpiredSessionAndServerFailureRemainDistinct() {
        val server = MockWebServer(); server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.blocked("Bearer teste").execute().body()!!.isEmpty())
            for (code in listOf(401, 404, 500)) {
                server.enqueue(MockResponse().setResponseCode(code).setBody("{}"))
                val response = api.unblock("Bearer teste", 31).execute()
                assertEquals(code, response.code()); assertNull(response.body()); response.errorBody()?.close()
            }
        } finally { server.shutdown() }
    }
}
