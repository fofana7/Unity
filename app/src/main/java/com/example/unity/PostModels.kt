package com.example.unity

import com.google.gson.annotations.SerializedName

data class PostResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("author_id") val authorId: Int,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("likes_count") var likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("is_liked") var isLiked: Boolean = false,
    @SerializedName("user") val user: UserResponse? = null
)

data class CreatePostRequest(
    @SerializedName("content") val content: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class PostListWrapper(
    @SerializedName("posts") val posts: List<PostResponse>
)
