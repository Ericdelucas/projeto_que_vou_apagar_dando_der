package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FavoritesApiTest {
    private fun api(server: MockWebServer): FavoritesApi = Retrofit.Builder()
        .baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create()).build()
        .create(FavoritesApi::class.java)

    @Test
    fun saveAndRemoveSendBearerAndExplicitDesiredState() {
        val server = MockWebServer()
        server.start()
        try {
            val api = api(server)
            for (saved in listOf(true, false)) {
                server.enqueue(MockResponse().setBody("""{"product_id":42,"is_favorite":$saved}"""))
                val response = api.setFavorite("Bearer token-usuario", FavoriteRequest(42, saved)).execute()
                assertTrue(response.isSuccessful)
                assertEquals(saved, response.body()?.isFavorite)
                assertEquals(42, response.body()?.productId)
                val request = server.takeRequest()
                assertEquals("POST", request.method)
                assertEquals("/api/favorites", request.path)
                assertEquals("Bearer token-usuario", request.getHeader("Authorization"))
                assertEquals("""{"product_id":42,"is_favorite":$saved}""", request.body.readUtf8())
            }
        } finally { server.shutdown() }
    }

    @Test
    fun savedListReadsProductPriceAndAuthor() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{
                "id":3,"product_id":42,"created_at":"2026-10-25T12:00:00Z",
                "product":{"id":42,"title":"Volante","price":"350.90","image_url":"https://example.com/p.jpg","user_id":9,"category":"Interior","model":"Mustang","location":"Palmas"},
                "author":{"id":9,"name":"Vendedor"}
            }]"""))
            val api = api(server)
            val item = api.getFavorites("Bearer token-usuario").execute().body()!!.single()
            assertEquals(42, item.productId)
            assertEquals("350.90", item.product.price)
            assertEquals("Vendedor", item.author.name)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/favorites", request.path)
            assertEquals("Bearer token-usuario", request.getHeader("Authorization"))
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.getFavorites("Bearer token-usuario").execute().body()!!.isEmpty())
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            val expired = api.getFavorites("Bearer expirado").execute()
            assertEquals(401, expired.code())
            assertNull(expired.body())
            expired.errorBody()?.close()
        } finally { server.shutdown() }
    }

}
