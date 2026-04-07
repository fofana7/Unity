package com.example.unity

import com.google.gson.annotations.SerializedName

// --- AUTH MODELS ---

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("message") val message: String,
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

// --- WRAPPERS (Structure API Express) ---

data class UserWrapper(
    @SerializedName("user") val user: UserResponse
)

data class UsersListWrapper(
    @SerializedName("users") val users: List<UserResponse>? = null,
    @SerializedName("friends") val friends: List<UserResponse>? = null
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
    @SerializedName("avatarurl") val avatarUrl: String? = null,
    @SerializedName("classe") val classe: String? = null
)

// --- CHAT & FRIENDS ---

data class Message(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int? = null,
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: String? = null,
    var isMe: Boolean = false
)

data class FriendActionRequest(
    @SerializedName("toId") val friendId: Int? = null,
    @SerializedName("fromId") val requesterId: Int? = null,
    @SerializedName("status") val status: String? = null
)