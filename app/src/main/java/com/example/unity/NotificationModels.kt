package com.example.unity

import com.google.gson.annotations.SerializedName

data class NotificationItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("from_user_id") val fromUserId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("avatarurl") val avatarUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("preview") val preview: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("post_id") val postId: Int? = null,
    @SerializedName("announcement_id") val announcementId: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("class_name") val className: String? = null
) {
    fun displayName(): String {
        val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
        return full.ifEmpty { username ?: "Utilisateur" }
    }
}
