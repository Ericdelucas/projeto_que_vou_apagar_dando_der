package com.example.app.network

import com.example.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Celular/emulador por ADB: execute adb reverse tcp:3000 tcp:3000.
    const val BASE_URL = BuildConfig.API_BASE_URL

    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val feed: FeedApi by lazy { retrofit.create(FeedApi::class.java) }
    val marketplace: MarketplaceApi by lazy { retrofit.create(MarketplaceApi::class.java) }
    val favorites: FavoritesApi by lazy { retrofit.create(FavoritesApi::class.java) }
    val seller: SellerApi by lazy { retrofit.create(SellerApi::class.java) }
    val settings: SettingsApi by lazy { retrofit.create(SettingsApi::class.java) }
    val users: UserApi by lazy { retrofit.create(UserApi::class.java) }
}
