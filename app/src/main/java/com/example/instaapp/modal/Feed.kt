package com.example.instaapp.modal

import android.widget.ImageView

data class Feed(
    var isLiked: Boolean = false,
    var likeAnimationView: ImageView? = null,
    val username: String? ="",
    var likes: Int?=0,
    val postid: String? = "",
    val image: String?= "",
    val imageposter: String?= "",
    val time: Long?= null,
    val caption: String? ="",
    val userId: String? = null,
    val likers: ArrayList<String> = ArrayList(),
    val comments: List<Comment>? = emptyList(),


    ){

}
