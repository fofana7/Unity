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
    @SerializedName("prenom") val firstName: String,
    @SerializedName("nom") val lastName: String,
    @SerializedName("role") val role: String,
    @SerializedName("classe") val classe: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

// --- USER MODELS ---

data class UserWrapper(
    @SerializedName("user") val user: UserResponse
)

data class UserResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("firstname") val firstName: String? = null,
    @SerializedName("lastname") val lastName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarurl") val avatarUrl: String? = null,
    @SerializedName("classe") val classe: String? = null,
    @SerializedName("friendsCount") val friendsCount: Int = 0,
    @SerializedName("postsCount") val postsCount: Int = 0
)

// --- CHAT & MESSAGES ---

data class SendMessageRequest(
    @SerializedName("receiverId") val receiverId: Int? = null,
    @SerializedName("groupId") val groupId: Int? = null,
    @SerializedName("content") val content: String
)

data class CreateGroupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null, // Champ rajouté
    @SerializedName("memberIds") val memberIds: List<Int>
)

data class Message(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int? = null,
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val timestamp: String? = null,
    var isMe: Boolean = false
)

data class GroupMessageResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("sender_id") val senderId: Int = 0,
    @SerializedName("content") val content: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("username") val username: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null
) {
    fun displayName(): String {
        val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
        return full.ifEmpty { username ?: "Utilisateur" }
    }
}

data class ConversationResponse(
    @SerializedName("other_user_id") val otherUserId: Int? = null,
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("other_username") val otherUsername: String? = null,
    @SerializedName("other_first_name") val otherFirstName: String? = null,
    @SerializedName("other_last_name") val otherLastName: String? = null,
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_time") val lastMessageTime: String? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0
) {
    fun displayName(): String {
        if (name != null) return name
        val full = listOfNotNull(otherFirstName, otherLastName).joinToString(" ").trim()
        return full.ifEmpty { otherUsername ?: "Utilisateur" }
    }

    fun toConversation() = Conversation(
        id = groupId ?: otherUserId ?: 0,
        userName = displayName(),
        lastMessage = lastMessage ?: "Nouvelle conversation",
        time = formatTime(lastMessageTime ?: ""),
        type = if (groupId != null) ConversationType.GROUP else ConversationType.PRIVATE
    )

    private fun formatTime(dateStr: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr) ?: return ""
            val diff = (System.currentTimeMillis() - date.time) / 1000
            when {
                diff < 3600 -> "${diff / 60}m"
                diff < 86400 -> "${diff / 3600}h"
                else -> "${diff / 86400}j"
            }
        } catch (e: Exception) { "" }
    }
}

// --- AMIS ---

data class FriendActionRequest(
    @SerializedName("friendId") val friendId: Int? = null,
    @SerializedName("requesterId") val requesterId: Int? = null,
    @SerializedName("status") val status: String? = null
)

// --- POSTS ---

data class PostResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("content") val content: String = "",
    @SerializedName("user_id") val authorId: Int = 0,
    @SerializedName("username") val authorName: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_data") val imageData: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("likes_count") var likesCount: Int = 0,
    @SerializedName("comments_count") var commentsCount: Int = 0,
    @SerializedName("is_liked") var isLiked: Boolean = false
) {
    fun displayName(): String {
        val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
        return full.ifEmpty { authorName ?: "Utilisateur" }
    }
    fun displayImage(): String? = imageData?.takeIf { it.isNotEmpty() } ?: imageUrl?.takeIf { it.isNotEmpty() }
}

data class CreatePostRequest(
    @SerializedName("content") val content: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class CommentResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("content") val content: String = "",
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("username") val username: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("avatarurl") val avatarUrl: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
) {
    fun displayName(): String {
        val full = listOfNotNull(firstName, lastName).joinToString(" ").trim()
        return full.ifEmpty { username ?: "Utilisateur" }
    }
}
