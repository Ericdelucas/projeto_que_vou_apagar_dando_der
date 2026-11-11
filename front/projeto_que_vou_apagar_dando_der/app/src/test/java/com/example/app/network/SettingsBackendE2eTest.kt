package com.example.app.network

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Integração opt-in: usa apenas a API e o banco descartáveis definidos pelo executor E2E. */
class SettingsBackendE2eTest {
    data class Registration(val name: String, val email: String, val password: String, val role: String)
    interface RegisterApi {
        @POST("auth/register") fun register(@Body request: Registration): Call<Void>
    }

    @Test fun retrofitCompletesTheProductJourneyAgainstRealBackend() {
        val url = System.getenv("SKIPPECAS_E2E_URL")
        assumeTrue("Defina SKIPPECAS_E2E_URL para a API de testes descartável.", !url.isNullOrBlank())
        val retrofit = Retrofit.Builder().baseUrl(requireNotNull(url))
            .addConverterFactory(GsonConverterFactory.create()).build()
        val register = retrofit.create(RegisterApi::class.java)
        val auth = retrofit.create(AuthApi::class.java)
        val settings = retrofit.create(SettingsApi::class.java)
        val users = retrofit.create(UserApi::class.java)
        val favorites = retrofit.create(FavoritesApi::class.java)
        val sales = retrofit.create(SellerApi::class.java)
        val market = retrofit.create(MarketplaceApi::class.java)
        val feed = retrofit.create(FeedApi::class.java)
        val suffix = UUID.randomUUID().toString()
        val password = "senha-e2e-12345"
        var sellerToken: String? = null
        var buyerToken: String? = null
        try {
            fun create(name: String, role: String): LoginResponse {
                val email = "$name-$suffix@example.com"
                val registration = register.register(Registration(name, email, password, role)).execute()
                assertEquals(201, registration.code())
                return requireNotNull(auth.login(LoginRequest(email, password)).execute().body())
            }
            val seller = create("VendedorE2e", "vendedor")
            sellerToken = "Bearer ${requireNotNull(seller.token)}"
            val buyer = create("CompradorE2e", "comprador")
            buyerToken = "Bearer ${requireNotNull(buyer.token)}"
            val sellerAuth = requireNotNull(sellerToken)
            val buyerAuth = requireNotNull(buyerToken)
            val sellerId = requireNotNull(seller.user).id
            val photo = requireNotNull(javaClass.getResourceAsStream("/avatar.png")).use { it.readBytes() }
            val avatarResult = users.uploadAvatar(buyerAuth, photo.toRequestBody("image/png".toMediaType())).execute()
            assertEquals(200, avatarResult.code())
            val avatarUrl = requireNotNull(avatarResult.body()?.avatarUrl)
            assertEquals(avatarUrl, users.me(buyerAuth).execute().body()?.avatarUrl)
            assertNotEquals(avatarUrl, users.me(sellerAuth).execute().body()?.avatarUrl)
            val uploadedFile = java.net.URI(avatarUrl).path.substringAfterLast('/')
            System.getenv("SKIPPECAS_E2E_UPLOAD_DIRECTORY")?.let { directory ->
                require(uploadedFile.matches(Regex("[a-f0-9-]+\\.png")))
                java.io.File(directory, uploadedFile).delete()
            }
            val imageUrl = "https://images.pexels.com/photos/10040636/pexels-photo-10040636.jpeg?auto=compress&cs=tinysrgb&w=960"
            val product = requireNotNull(sales.createProduct(sellerAuth, NewSellerProduct("Volante E2E", "199.90",
                "Interior", "Mustang", imageUrl, null, "Palmas")).execute().body())
            assertTrue(market.getProducts(model = "Mustang", minPrice = "100", maxPrice = "250")
                .execute().body().orEmpty().any { it.id == product.id })
            val details = requireNotNull(feed.getProduct(product.id).execute().body())
            assertEquals(product.id, details.id)
            assertEquals(imageUrl, details.imageUrl)
            assertTrue(favorites.setFavorite(buyerAuth, FavoriteRequest(product.id, true)).execute().body()!!.isFavorite)
            assertEquals(product.id, favorites.getFavorites(buyerAuth).execute().body()!!.single().productId)
            assertEquals(sellerId, users.search(buyerAuth, "VendedorE2e").execute().body()!!.single().id)
            assertFalse(users.profile(buyerAuth, sellerId).execute().body()!!.isOwner)
            val block = requireNotNull(settings.block(buyerAuth, BlockUserRequest(sellerId)).execute().body())
            assertEquals(block.id, settings.blocked(buyerAuth).execute().body()!!.single().id)
            assertEquals(204, settings.unblock(buyerAuth, block.id).execute().code())
            assertTrue(settings.blocked(buyerAuth).execute().body()!!.isEmpty())
            assertEquals(204, settings.deleteAccount(sellerAuth).execute().code())
            val invalidated = users.me(sellerAuth).execute()
            assertEquals(401, invalidated.code()); invalidated.errorBody()?.close()
            assertTrue(favorites.getFavorites(buyerAuth).execute().body()!!.isEmpty())
            val removedProduct = feed.getProduct(product.id).execute()
            assertEquals(404, removedProduct.code()); removedProduct.errorBody()?.close()
        } finally {
            // O executor remove todo o banco descartável mesmo em caso de falha.
            sellerToken?.let { settings.deleteAccount(it).execute().errorBody()?.close() }
            buyerToken?.let { settings.deleteAccount(it).execute().errorBody()?.close() }
        }
    }
}
