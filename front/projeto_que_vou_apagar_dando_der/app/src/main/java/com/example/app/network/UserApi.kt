package com.example.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

data class UserSearchItem(
    val id: Int, val name: String?, val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("last_active_at") val lastActiveAt: String?,
    @SerializedName("activity_status") val activityStatus: String,
    val activity: List<Int>? = null,
    @SerializedName("activity_count") val activityCount: Int = 0,
)
data class ProfileMedia(
    @SerializedName("media_id") val mediaId: String,
    val kind: String,
    @SerializedName("post_id") val postId: Int?,
    @SerializedName("product_id") val productId: Int?,
    val title: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("video_url") val videoUrl: String?,
)
data class UserProfile(
    val id: Int, val name: String?, val email: String?, val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?, val bio: String?,
    @SerializedName("is_owner") val isOwner: Boolean,
    @SerializedName("posts_count") val postsCount: Int,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    val media: List<ProfileMedia>,
)
data class UpdateProfileRequest(val name: String, val email: String, val avatar_url: String, val bio: String)

interface UserApi {
    @POST("users/avatar")
    fun uploadAvatar(@Header("Authorization") auth: String, @Body photo: okhttp3.RequestBody): Call<UserProfile>

    @GET("users/search")
    fun search(@Header("Authorization") auth: String, @Query("q") query: String,
               @Query("filter") filter: String? = null): Call<List<UserSearchItem>>
    @GET("users/me")
    fun me(@Header("Authorization") auth: String): Call<UserProfile>
    @GET("users/profile/{userId}")
    fun profile(@Header("Authorization") auth: String, @Path("userId") userId: Int): Call<UserProfile>
    @PUT("users/profile")
    fun update(@Header("Authorization") auth: String, @Body body: UpdateProfileRequest): Call<UserProfile>
}
