package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MarketplaceApiTest {
    private fun api(server: MockWebServer): MarketplaceApi = Retrofit.Builder()
        .baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build()
        .create(MarketplaceApi::class.java)

    @Test
    fun combinesAndEncodesFiltersAndReadsSellerAndPrice() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""[
                {"id":42,"title":"Volante","price":"350.90","image_url":"https://example.com/p.jpg","user_id":7,"category":"Interior","model":"Mustang","location":"São Paulo"}
            ]"""))
            val result = api(server).getProducts("Mustang", "Interior", "São Paulo", "0", "500.00").execute()
            assertTrue(result.isSuccessful)
            val product = result.body()!!.single()
            assertEquals(42, product.id)
            assertEquals(7, product.userId)
            assertEquals("350.90", product.price)
            assertEquals("https://example.com/p.jpg", product.imageUrl)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/marketplace", request.requestUrl!!.encodedPath)
            assertEquals("Mustang", request.requestUrl!!.queryParameter("model"))
            assertEquals("Interior", request.requestUrl!!.queryParameter("category"))
            assertEquals("São Paulo", request.requestUrl!!.queryParameter("location"))
            assertEquals("0", request.requestUrl!!.queryParameter("min_price"))
            assertEquals("500.00", request.requestUrl!!.queryParameter("max_price"))
        } finally { server.shutdown() }
    }

    @Test
    fun unselectedFiltersAreOmittedAndOptionsComeFromServer() {
        val server = MockWebServer()
        server.start()
        try {
            val api = api(server)
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.getProducts().execute().body()!!.isEmpty())
            assertEquals("/api/marketplace", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("""{
                "models":["Fiesta","Mustang"],"categories":["Interior"],"locations":["Palmas - TO"]
            }"""))
            val options = api.getFilters().execute().body()!!
            assertEquals(listOf("Fiesta", "Mustang"), options.models)
            assertEquals(listOf("Interior"), options.categories)
            assertEquals(listOf("Palmas - TO"), options.locations)
            assertEquals("/api/marketplace/filters", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("[]"))
            api.getProducts(category = "Interior").execute()
            assertEquals("/api/marketplace?category=Interior", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test
    fun failureDoesNotReturnProducts() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(500)
                .setBody("""{"message":"Não foi possível carregar os anúncios."}"""))
            val result = api(server).getProducts().execute()
            assertEquals(500, result.code())
            assertNull(result.body())
            result.errorBody()?.close()
        } finally { server.shutdown() }
    }
}
