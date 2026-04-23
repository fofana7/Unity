package com.example.unity

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTH ---
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserWrapper>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("auth/change-password")
    suspend fun changePassword(@Header("Authorization") token: String, @Body body: ChangePasswordRequest): Response<Void>

    // --- USERS ---
    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserWrapper>

    @GET("users/{id}")
    suspend fun getUserById(@Header("Authorization") token: String, @Path("id") id: Int): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateMe(@Header("Authorization") token: String, @Body body: UserResponse): Response<UserWrapper>

    @DELETE("users/me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Void>

    @Multipart
    @PUT("users/me/avatar")
    suspend fun uploadAvatar(@Header("Authorization") token: String, @Part avatar: okhttp3.MultipartBody.Part): Response<Map<String, String>>

    @GET("users/{id}")
    suspend fun getUser(@Header("Authorization") token: String, @Path("id") id: Int): Response<UserWrapper>

    @GET("users/search")
    suspend fun searchUsers(@Header("Authorization") token: String, @Query("q") query: String = ""): Response<List<UserResponse>>

    @GET("ami")
    suspend fun getAllUsers(@Header("Authorization") token: String): Response<List<UserResponse>>

    // --- AMIS ---
    @GET("ami/friends")
    suspend fun getMyFriends(@Header("Authorization") token: String): Response<List<UserResponse>>

    @GET("ami/friends/{id}")
    suspend fun getFriendsById(@Header("Authorization") token: String, @Path("id") id: Int): Response<List<UserResponse>>

    @POST("ami/add")
    suspend fun addFriend(@Header("Authorization") token: String, @Body body: FriendActionRequest): Response<ResponseWrapper>

    @GET("ami/requests")
    suspend fun getFriendRequests(@Header("Authorization") token: String): Response<List<UserResponse>>

    @GET("ami/requests/sent")
    suspend fun getSentRequests(@Header("Authorization") token: String): Response<List<UserResponse>>

    @GET("ami/status/{friendId}")
    suspend fun getFriendshipStatus(@Header("Authorization") token: String, @Path("friendId") friendId: Int): Response<FriendshipStatusResponse>

    @POST("ami/accept")
    suspend fun acceptFriend(@Header("Authorization") token: String, @Body body: FriendActionRequest): Response<ResponseWrapper>

    @POST("ami/decline")
    suspend fun declineFriend(@Header("Authorization") token: String, @Body body: FriendActionRequest): Response<ResponseWrapper>

    // --- POSTS ---
    @GET("posts/timeline")
    suspend fun getPosts(@Header("Authorization") token: String): Response<List<PostResponse>>

    @POST("posts")
    suspend fun createPost(@Header("Authorization") token: String, @Body body: CreatePostRequest): Response<PostResponse>

    @PUT("posts/{id}")
    suspend fun updatePost(@Header("Authorization") token: String, @Path("id") id: Int, @Body body: CreatePostRequest): Response<PostResponse>

    @DELETE("posts/{id}")
    suspend fun deletePost(@Header("Authorization") token: String, @Path("id") id: Int): Response<Void>

    @POST("posts/{id}/like")
    suspend fun likePost(@Header("Authorization") token: String, @Path("id") id: Int): Response<Void>

    // --- COMMENTS ---
    @GET("posts/{id}/comments")
    suspend fun getComments(@Header("Authorization") token: String, @Path("id") postId: Int): Response<List<CommentResponse>>

    @POST("posts/{id}/comments")
    suspend fun addComment(@Header("Authorization") token: String, @Path("id") postId: Int, @Body body: Map<String, String>): Response<CommentResponse>

    // --- MESSAGES ---
    @GET("messages/{userId}")
    suspend fun getPrivateMessages(@Header("Authorization") token: String, @Path("userId") otherUserId: Int): Response<List<Message>>

    @POST("messages/send")
    suspend fun sendPrivateMessage(@Header("Authorization") token: String, @Body body: SendMessageRequest): Response<Message>

    @GET("messages/group/{groupId}/members")
    suspend fun getGroupMembers(@Header("Authorization") token: String, @Path("groupId") groupId: Int): Response<List<UserResponse>>

    @Multipart
    @POST("messages/upload")
    suspend fun uploadChatFile(
        @Header("Authorization") token: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<ChatUploadResponse>

    @GET("messages/group/{groupId}")
    suspend fun getGroupMessages(@Header("Authorization") token: String, @Path("groupId") groupId: Int): Response<List<GroupMessageResponse>>

    @POST("messages/group/send")
    suspend fun sendGroupMessage(@Header("Authorization") token: String, @Body body: SendMessageRequest): Response<GroupMessageResponse>

    @GET("messages/conversations")
    suspend fun getConversations(@Header("Authorization") token: String): Response<List<ConversationResponse>>

    // --- GROUPES ---
    @POST("ami/groups/create")
    suspend fun createGroup(@Header("Authorization") token: String, @Body request: CreateGroupRequest): Response<Void>

    @GET("ami/groups/{id}")
    suspend fun getGroupDetails(@Header("Authorization") token: String, @Path("id") groupId: Int): Response<GroupDetailsResponse>

    @POST("ami/groups/{id}/add-members")
    suspend fun addMembersToGroup(@Header("Authorization") token: String, @Path("id") groupId: Int, @Body body: AddMembersRequest): Response<ResponseWrapper>

    @DELETE("messages/group/{id}/leave")
    suspend fun leaveGroup(@Header("Authorization") token: String, @Path("id") groupId: Int): Response<Void>

    // --- NOTIFICATIONS ---
    @GET("notifications")
    suspend fun getNotifications(@Header("Authorization") token: String): Response<List<NotificationItem>>

    // --- EVENEMENTS (Teacher Feature) ---
    @POST("content/events")
    suspend fun createEvent(@Header("Authorization") token: String, @Body body: CreateEventRequest): Response<EventResponse>

    @GET("content/events/classe/{classe}")
    suspend fun getEventsByClasse(@Header("Authorization") token: String, @Path("classe") classe: String): Response<List<EventResponse>>

    @GET("content/events/teacher/me")
    suspend fun getMyEvents(@Header("Authorization") token: String): Response<List<EventResponse>>

    @GET("content/events/{id}")
    suspend fun getEventById(@Header("Authorization") token: String, @Path("id") id: Int): Response<EventResponse>

    @PUT("content/events/{id}")
    suspend fun updateEvent(@Header("Authorization") token: String, @Path("id") id: Int, @Body body: CreateEventRequest): Response<ResponseWrapper>

    @DELETE("content/events/{id}")
    suspend fun deleteEvent(@Header("Authorization") token: String, @Path("id") id: Int): Response<ResponseWrapper>
}

data class ResponseWrapper(
    val success: Boolean,
    val message: String? = null
)
