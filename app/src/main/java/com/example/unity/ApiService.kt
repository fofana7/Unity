package com.example.unity

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTH ---
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body body: ChangePasswordRequest
    ): Response<Void>

    // --- USERS ---
    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body body: UserResponse
    ): Response<UserResponse>

    @DELETE("users/me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Void>

    @GET("users/{id}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<UserResponse>

    @GET("users/{id}/friends")
    suspend fun getUserFriends(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<List<UserResponse>>

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

    @POST("ami/reject")
    suspend fun rejectFriend(
        @Header("Authorization") token: String,
        @Body body: FriendActionRequest
    ): Response<Void>

    @POST("ami/remove")
    suspend fun removeFriend(
        @Header("Authorization") token: String,
        @Body body: FriendActionRequest
    ): Response<Void>

    @GET("ami/check/{friendId}")
    suspend fun checkFriendship(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: Int
    ): Response<Map<String, Boolean>>
}
