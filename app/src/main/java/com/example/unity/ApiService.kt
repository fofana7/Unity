package com.example.unity

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
    suspend fun getFriendRequests(@Header("Authorization") token: String): Response<List<FriendActionRequest>>

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

    // --- MESSAGES ---
    @GET("messages/private/{userId}")
    suspend fun getPrivateMessages(
        @Header("Authorization") token: String,
        @Path("userId") otherUserId: Int
    ): Response<List<Message>>

    @POST("messages/private")
    suspend fun sendPrivateMessage(
        @Header("Authorization") token: String,
        @Body body: Message
    ): Response<Message>

    @GET("messages/group/{groupId}")
    suspend fun getGroupMessages(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Response<List<Message>>

    @POST("messages/group")
    suspend fun sendGroupMessage(
        @Header("Authorization") token: String,
        @Body body: Message
    ): Response<Message>

    // --- POSTS ---
    @GET("posts")
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
}
