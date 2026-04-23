package com.example.unity

import com.google.gson.annotations.SerializedName

data class NotificationItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName(value = "from_user_id", alternate = ["sender_id"]) val fromUserId: Int? = null,
    @SerializedName(value = "username", alternate = ["sender_name"]) val username: String? = null,
    @SerializedName(value = "first_name", alternate = ["sender_first_name"]) val firstName: String? = null,
    @SerializedName(value = "last_name", alternate = ["sender_last_name"]) val lastName: String? = null,
    @SerializedName("avatarurl") val avatarUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("preview") val preview: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("post_id") val postId: Int? = null,
    @SerializedName("announcement_id") val announcementId: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("is_read") var isRead: Boolean = false,
    @SerializedName("content") val content: String? = null,
    @SerializedName("event_id") val eventId: Int? = null
) {
    fun displayName(): String {
        val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
        return full.ifEmpty { username ?: "Utilisateur" }
    }
}
