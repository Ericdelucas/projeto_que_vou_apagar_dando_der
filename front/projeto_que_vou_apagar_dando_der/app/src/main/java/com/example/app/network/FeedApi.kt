package com.example.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

data class PostAuthor(val id: Int, val name: String?)

data class ProductTag(
    val id: Int,
    val title: String,
    // MySQL DECIMAL é enviado como string para preservar as casas decimais.
    val price: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("user_id") val userId: Int,
    val category: String?,
    val model: String?,
    val location: String?,
    @SerializedName("video_url") val videoUrl: String? = null,
)

data class PostResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val user: PostAuthor,
    @SerializedName("product_id") val productId: Int?,
    @SerializedName("video_url") val videoUrl: String?,
    val description: String?,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("comments_count") val commentsCount: Int,
    val product: ProductTag?,
    @SerializedName("image_url") val imageUrl: String? = null,
)

data class NewPostRequest(val description: String, val image_url: String?, val video_url: String?)
data class CreatedPost(val id: Int)

interface FeedApi {
    @POST("posts/photo")
    fun publishPhoto(@Header("Authorization") auth: String,
        @retrofit2.http.Query("description") caption: String,
        @Body photo: okhttp3.RequestBody): Call<CreatedPost>

    @POST("posts")
    fun publish(@Header("Authorization") auth: String, @Body post: NewPostRequest): Call<CreatedPost>

    @GET("feed")
    fun getFeed(): Call<List<PostResponse>>

    @GET("products/{id}")
    fun getProduct(@Path("id") id: Int): Call<ProductTag>
}
