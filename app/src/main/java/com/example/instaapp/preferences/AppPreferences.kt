package com.example.instaapp.preferences

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AppPreferences {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val likesReference: DatabaseReference = database.getReference("likes")

    private val likeChangeListeners = mutableMapOf<String, ValueEventListener>()

    fun saveLikedPost(userId: String, postId: String, isLiked: String) {
        likesReference.child(userId).child(postId).setValue(isLiked)
    }


    fun isPostLiked(userId: String, postId: String, callback: (Boolean) -> Unit) {
        likesReference.child(userId).child(postId).get().addOnSuccessListener { dataSnapshot ->
            val isLikedString = dataSnapshot.getValue(String::class.java) // Получение значения как строки
            val isLiked = isLikedString?.toBoolean() ?: false // Преобразование строки в булево значение
            callback(isLiked)
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun addLikeChangeListener(postId: String, listener: ValueEventListener) {
        val likeReference = likesReference.child(postId)
        likeReference.addValueEventListener(listener)
        likeChangeListeners[postId] = listener
    }

    fun removeLikeChangeListener(postId: String, listener: ValueEventListener) {
        val likeReference = likesReference.child(postId)
        likeReference.removeEventListener(listener)
        likeChangeListeners.remove(postId)
    }
}