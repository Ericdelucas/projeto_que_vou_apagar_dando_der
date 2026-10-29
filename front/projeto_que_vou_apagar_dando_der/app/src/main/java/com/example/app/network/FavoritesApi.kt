package com.example.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class FavoriteAuthor(val id: Int, val name: String?)
data class FavoriteItem(
    val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("created_at") val createdAt: String,
    val product: ProductTag,
    val author: FavoriteAuthor,
)
data class FavoriteRequest(
    @SerializedName("product_id") val productId: Int,
    // Estado explícito torna uma repetição da mesma requisição segura.
    @SerializedName("is_favorite") val isFavorite: Boolean,
)
data class FavoriteResult(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("is_favorite") val isFavorite: Boolean,
)

interface FavoritesApi {
    @GET("favorites")
    fun getFavorites(@Header("Authorization") authorization: String): Call<List<FavoriteItem>>

    @POST("favorites")
    fun setFavorite(
        @Header("Authorization") authorization: String,
        @Body request: FavoriteRequest,
    ): Call<FavoriteResult>
}
