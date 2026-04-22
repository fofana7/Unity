package com.example.unity

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTH ---
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserWrapper>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body body: ChangePasswordRequest
    ): Response<Void>

    // --- USERS ---
    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserWrapper>

    @PUT("users/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body body: UserResponse
    ): Response<UserWrapper>

    @Multipart
    @PUT("users/me/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: okhttp3.MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("users/me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Void>

    @GET("users/{id}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<UserWrapper>

    // --- AMIS ---
    @GET("ami")
    suspend fun getAllUsers(@Header("Authorization") token: String): Response<List<UserResponse>>

    @GET("ami/friends")
    suspend fun getMyFriends(@Header("Authorization") token: String): Response<List<UserResponse>>

    @GET("ami/requests")
    suspend fun getFriendRequests(@Header("Authorization") token: String): Response<List<UserResponse>>

    // Utilisation de FriendActionRequest pour la cohérence
    @POST("ami/add")
    suspend fun addFriend(
        @Header("Authorization") token: String,
        @Body body: FriendActionRequest
    ): Response<Void>

    @POST("ami/accept")
    suspend fun acceptFriend(
        @Header("Authorization") token: String,
        @Body body: FriendActionRequest
    ): Response<Void>

    @POST("ami/reject")
    suspend fun declineFriend(
        @Header("Authorization") token: String,
        @Body body: FriendActionRequest
    ): Response<Void>

    // --- MESSAGES ---
    @GET("messages/{userId}")
    suspend fun getPrivateMessages(
        @Header("Authorization") token: String,
        @Path("userId") otherUserId: Int
    ): Response<List<Message>>

    @POST("messages/send")
    suspend fun sendPrivateMessage(
        @Header("Authorization") token: String,
        @Body body: SendMessageRequest
    ): Response<Message>

    @GET("messages/group/{groupId}")
    suspend fun getGroupMessages(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Response<List<GroupMessageResponse>>

    @POST("messages/group/send")
    suspend fun sendGroupMessage(
        @Header("Authorization") token: String,
        @Body body: SendMessageRequest
    ): Response<GroupMessageResponse>

    // --- POSTS ---
    @GET("posts/timeline")
    suspend fun getPosts(@Header("Authorization") token: String): Response<List<PostResponse>>

    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Body body: CreatePostRequest
    ): Response<PostResponse>

    @POST("posts/{id}/like")
    suspend fun likePost(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<Void>

    @PUT("posts/{id}")
    suspend fun updatePost(
        @Header("Authorization") token: String,
        @Path("id") postId: Int,
        @Body body: CreatePostRequest
    ): Response<PostResponse>

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<Void>

    @POST("posts/{id}/comments")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Path("id") postId: Int,
        @Body body: Map<String, String>
    ): Response<Void>

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<List<CommentResponse>>

    // --- CONVERSATIONS ---
    @GET("messages/conversations")
    suspend fun getConversations(@Header("Authorization") token: String): Response<List<ConversationResponse>>

    // --- NOTIFICATIONS ---
    @GET("notifications")
    suspend fun getNotifications(@Header("Authorization") token: String): Response<List<NotificationItem>>

    // --- GROUPES ---
    @POST("ami/groups/create")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: CreateGroupRequest
    ): Response<Void>
}
