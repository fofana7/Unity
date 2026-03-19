package com.example.unity

import com.google.gson.annotations.SerializedName

// --- AUTH MODELS ---

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserResponse
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String,
    @SerializedName("firstname") val firstName: String,
    @SerializedName("lastname") val lastName: String,
    @SerializedName("role") val role: String,
    @SerializedName("classe") val classe: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

// --- USER MODELS ---

data class UserResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("firstname") val firstName: String? = null,
    @SerializedName("lastname") val lastName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("classe") val classe: String? = null
)

// --- FRIEND MODELS ---

data class FriendActionRequest(
    @SerializedName("friendId") val friendId: Int? = null,
    @SerializedName("requesterId") val requesterId: Int? = null,
    @SerializedName("status") val status: String? = null
)

// --- POST MODELS ---

data class PostResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("author") val author: String,
    @SerializedName("content") val content: String,
    @SerializedName("uri") val uri: String?,
    @SerializedName("is_image") val isImage: Boolean,
    @SerializedName("time") val time: String
)
