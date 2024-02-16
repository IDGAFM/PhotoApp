package com.example.instaapp.modal

data class Comment(
    var commentId: String? = "",
    val userId: String? = "",
    val username: String? = null,
    val imageUrl: String? = null,
    var time: Long? = null,
    var commentText: String = "",

    )
