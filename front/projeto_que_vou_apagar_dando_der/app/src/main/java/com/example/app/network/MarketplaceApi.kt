package com.example.app.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class MarketplaceFilters(
    val models: List<String>,
    val categories: List<String>,
    val locations: List<String>,
)

interface MarketplaceApi {
    @GET("marketplace")
    fun getProducts(
        @Query("model") model: String? = null,
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
        @Query("min_price") minPrice: String? = null,
        @Query("max_price") maxPrice: String? = null,
    ): Call<List<ProductTag>>

    @GET("marketplace/filters")
    fun getFilters(): Call<MarketplaceFilters>
}
