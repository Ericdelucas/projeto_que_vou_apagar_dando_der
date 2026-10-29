package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SellerApiTest {
    private fun api(server: MockWebServer): SellerApi = Retrofit.Builder().baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build().create(SellerApi::class.java)
    private val product = """{"id":42,"title":"Volante","price":"350.90","user_id":7,"image_url":"https://example.com/p.jpg","video_url":null,"category":"Interior","model":"Mustang","location":"Palmas","status":"ativo","requests_count":3,"pending_requests_count":2,"accepted_requests_count":1,"rejected_requests_count":0}"""

    @Test
    fun listIncludesStatusAndRequestCountsWithBearer() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("[$product]"))
            val item = api(server).getProducts("Bearer vendedor").execute().body()!!.single()
            assertEquals("ativo", item.status)
            assertEquals(3, item.requestsCount)
            assertEquals(2, item.pendingRequestsCount)
            assertEquals(1, item.acceptedRequestsCount)
            assertEquals("350.90", item.price)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/seller/products", request.path)
            assertEquals("Bearer vendedor", request.getHeader("Authorization"))
        } finally { server.shutdown() }
    }

    @Test
    fun createUsesExpectedFieldsAndReadsCreatedProduct() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201).setBody(product))
            val response = api(server).createProduct("Bearer vendedor",
                NewSellerProduct("Volante", "350.90", "Interior", "Mustang", "https://example.com/p.jpg", null, "Palmas")).execute()
            assertEquals(201, response.code())
            assertEquals(42, response.body()?.id)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/seller/products", request.path)
            assertEquals("Bearer vendedor", request.getHeader("Authorization"))
            assertEquals("""{"title":"Volante","price":"350.90","category":"Interior","model":"Mustang","image_url":"https://example.com/p.jpg","location":"Palmas"}""", request.body.readUtf8())
        } finally { server.shutdown() }
    }

    @Test
    fun emptyListAndForbiddenAccountDoNotProduceFakeProducts() {
        val server = MockWebServer()
        server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.getProducts("Bearer vendedor").execute().body()!!.isEmpty())
            server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
            val forbidden = api.getProducts("Bearer comprador").execute()
            assertEquals(403, forbidden.code())
            assertNull(forbidden.body())
            forbidden.errorBody()?.close()
        } finally { server.shutdown() }
    }
}
