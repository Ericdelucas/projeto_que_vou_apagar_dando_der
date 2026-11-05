package com.example.app.network

import android.content.Context
import com.example.app.BuildConfig
import com.example.app.auth.TokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = BuildConfig.API_BASE_URL

    // Migra instalações antigas sem reaproveitar sessões de outro servidor.
    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences("api_connection", Context.MODE_PRIVATE)
        if (preferences.getString("bound_api", null) != BASE_URL) {
            TokenStore(context).clear()
            preferences.edit().clear().putString("bound_api", BASE_URL).apply()
        }
    }

    private val retrofit: Retrofit by lazy { createRetrofit() }

    private fun createRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val auth: AuthApi get() = retrofit.create(AuthApi::class.java)
    val feed: FeedApi get() = retrofit.create(FeedApi::class.java)
    val marketplace: MarketplaceApi get() = retrofit.create(MarketplaceApi::class.java)
    val favorites: FavoritesApi get() = retrofit.create(FavoritesApi::class.java)
    val seller: SellerApi get() = retrofit.create(SellerApi::class.java)
    val settings: SettingsApi get() = retrofit.create(SettingsApi::class.java)
    val users: UserApi get() = retrofit.create(UserApi::class.java)
}
