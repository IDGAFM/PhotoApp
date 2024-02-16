package com.example.instaapp.modal

data class Posts (

    val id: String? = "",
    val username: String? = "",
    val image:String?="",
    val time: Long?=null,
    val caption: String?="",
    val likes: Int?= 0,
    val userid: String?="",
    val postid: String?="",
    val profileImage: String?= "",
    val comment: String?=""
    )

{
}