package com.example.unity

data class Post(
    val author: String,
    val content: String,
    val uri: String?,
    val isImage: Boolean,
    val time: String
)