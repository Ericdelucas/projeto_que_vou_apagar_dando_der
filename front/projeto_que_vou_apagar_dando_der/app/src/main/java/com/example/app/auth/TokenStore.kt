package com.example.app.auth

import android.content.Context

class TokenStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("auth_session", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        preferences.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? = preferences.getString("jwt_token", null)

    fun clear() {
        preferences.edit().clear().apply()
    }
}
