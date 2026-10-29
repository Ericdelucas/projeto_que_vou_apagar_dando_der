package com.example.app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class AuthUser(val id: Int, val name: String, val email: String, val role: String)
data class LoginResponse(val token: String?, val user: AuthUser?)

data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class RegisterResponse(val user: AuthUser?)

interface AuthApi {
    @POST("auth/register")
    fun register(@Body account: RegisterRequest): Call<RegisterResponse>

    @POST("auth/login")
    fun login(@Body credentials: LoginRequest): Call<LoginResponse>
}
