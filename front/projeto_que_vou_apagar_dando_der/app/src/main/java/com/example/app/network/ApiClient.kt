package com.example.app.network

import android.content.Context
import com.example.app.BuildConfig
import com.example.app.auth.TokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Celular/emulador por ADB: execute adb reverse tcp:3000 tcp:3000.
    const val BASE_URL = BuildConfig.API_BASE_URL

    var activeBaseUrl: String = BASE_URL
        private set
    private var configuredRetrofit: Retrofit? = null

    fun initialize(context: Context) {
        if (!BuildConfig.DEBUG) return
        val saved = context.getSharedPreferences("api_connection", Context.MODE_PRIVATE).getString("url", null)
        activeBaseUrl = saved?.let { runCatching { ApiEndpoint.normalize(it, true) }.getOrNull() } ?: BASE_URL
        configuredRetrofit = null
    }

    fun configure(context: Context, url: String) {
        check(BuildConfig.DEBUG)
        val normalized = ApiEndpoint.normalize(url, true)
        if (normalized != activeBaseUrl) TokenStore(context).clear()
        activeBaseUrl = normalized
        configuredRetrofit = null
        context.getSharedPreferences("api_connection", Context.MODE_PRIVATE).edit().putString("url", normalized).apply()
    }

    private val retrofit: Retrofit
        get() = configuredRetrofit ?: createRetrofit().also { configuredRetrofit = it }

    private fun createRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(activeBaseUrl)
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
