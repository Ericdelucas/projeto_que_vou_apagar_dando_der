package com.example.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class BlockedUserSummary(
    val id: Int, val name: String?, val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
)
data class BlockedUser(
    val id: Int,
    @SerializedName("blocked_user_id") val blockedUserId: Int,
    @SerializedName("created_at") val createdAt: String,
    val user: BlockedUserSummary,
)
data class BlockUserRequest(@SerializedName("blocked_user_id") val blockedUserId: Int)

interface SettingsApi {
    @GET("settings/blocked")
    fun blocked(@Header("Authorization") auth: String): Call<List<BlockedUser>>

    @POST("settings/block")
    fun block(@Header("Authorization") auth: String, @Body body: BlockUserRequest): Call<BlockedUser>

    // id é o identificador do bloqueio, não o ID do usuário bloqueado.
    @DELETE("settings/block/{id}")
    fun unblock(@Header("Authorization") auth: String, @Path("id") id: Int): Call<Void>

    @DELETE("settings/account")
    fun deleteAccount(@Header("Authorization") auth: String): Call<Void>
}
