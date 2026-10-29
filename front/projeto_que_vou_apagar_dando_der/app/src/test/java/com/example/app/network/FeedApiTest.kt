package com.example.app.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedApiTest {
    private fun api(server: MockWebServer): FeedApi = Retrofit.Builder()
        .baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build()
        .create(FeedApi::class.java)

    @Test
    fun photoUploadSendsBinaryImageAndEncodedCaption() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":124}"""))
            val bytes = byteArrayOf(-1, -40, -1, 1, 2, 3)
            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            val response = api(server).publishPhoto("Bearer token", "Minha foto & peças", body).execute()
            assertEquals(124, response.body()?.id)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/posts/photo", request.requestUrl!!.encodedPath)
            assertEquals("Minha foto & peças", request.requestUrl!!.queryParameter("description"))
            assertEquals("Bearer token", request.getHeader("Authorization"))
            assertEquals("image/jpeg", request.getHeader("Content-Type"))
            assertArrayEquals(bytes, request.body.readByteArray())
        } finally { server.shutdown() }
    }

    @Test
    fun publishSendsAuthenticatedContentAndReadsPostId() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":123}"""))
            val response = api(server).publish("Bearer token", NewPostRequest("Meu post", "https://example.com/p.jpg", null)).execute()
            assertEquals(201, response.code())
            assertEquals(123, response.body()?.id)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/posts", request.path)
            assertEquals("Bearer token", request.getHeader("Authorization"))
            val json = com.google.gson.JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("Meu post", json.get("description").asString)
            assertEquals("https://example.com/p.jpg", json.get("image_url").asString)
            assertFalse(json.has("user_id"))
        } finally { server.shutdown() }
    }

    @Test
    fun feedReadsProductTagAndUsesProductIdForDetails() {
        val server = MockWebServer()
        server.start()
        try {
            val product = """{"id":42,"title":"Volante","price":"350.90","image_url":"https://example.com/p.jpg","user_id":7,"category":"Interior","model":"Mustang","location":"Palmas"}"""
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""[
                {"id":91,"user_id":7,"user":{"id":7,"name":"Ana"},"product_id":42,"video_url":null,"description":"Peça em ótimo estado","likes_count":12,"comments_count":3,"product":$product},
                {"id":90,"user_id":7,"user":{"id":7,"name":"Ana"},"product_id":null,"video_url":"https://example.com/video.mp4","description":null,"likes_count":0,"comments_count":0,"product":null}
            ]"""))
            val api = api(server)
            val response = api.getFeed().execute()
            assertTrue(response.isSuccessful)
            val posts = response.body()!!
            assertEquals(2, posts.size)
            assertEquals("Ana", posts[0].user.name)
            assertEquals("Peça em ótimo estado", posts[0].description)
            assertEquals(12, posts[0].likesCount)
            assertEquals(3, posts[0].commentsCount)
            assertEquals("350.90", posts[0].product?.price)
            assertNull(posts[1].product)
            assertNotNull(posts[1].videoUrl)
            val feedRequest = server.takeRequest()
            assertEquals("GET", feedRequest.method)
            assertEquals("/api/feed", feedRequest.path)

            server.enqueue(MockResponse().setBody(product))
            val selectedProductId = posts[0].product!!.id
            assertNotEquals(posts[0].id, selectedProductId)
            assertEquals(42, api.getProduct(selectedProductId).execute().body()?.id)
            assertEquals("/api/products/42", server.takeRequest().path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun feedCanBeEmptyAndProductCanBeRemoved() {
        val server = MockWebServer()
        server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.getFeed().execute().body()!!.isEmpty())
            server.enqueue(MockResponse().setResponseCode(404)
                .setBody("""{"message":"Produto não encontrado."}"""))
            val response = api.getProduct(42).execute()
            assertEquals(404, response.code())
            assertNull(response.body())
            response.errorBody()?.close()
        } finally {
            server.shutdown()
        }
    }
}
