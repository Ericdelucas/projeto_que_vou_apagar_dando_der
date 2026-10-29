package com.example.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class SellerProduct(
    val id: Int,
    val title: String,
    val price: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("video_url") val videoUrl: String?,
    val category: String?,
    val model: String?,
    val location: String?,
    val status: String,
    @SerializedName("requests_count") val requestsCount: Int,
    @SerializedName("pending_requests_count") val pendingRequestsCount: Int,
    @SerializedName("accepted_requests_count") val acceptedRequestsCount: Int,
    @SerializedName("rejected_requests_count") val rejectedRequestsCount: Int,
)

data class NewSellerProduct(
    val title: String,
    val price: String,
    val category: String,
    val model: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("video_url") val videoUrl: String?,
    val location: String?,
)

interface SellerApi {
    @GET("seller/products")
    fun getProducts(@Header("Authorization") authorization: String): Call<List<SellerProduct>>

    @POST("seller/products")
    fun createProduct(
        @Header("Authorization") authorization: String,
        @Body product: NewSellerProduct,
    ): Call<SellerProduct>
}
