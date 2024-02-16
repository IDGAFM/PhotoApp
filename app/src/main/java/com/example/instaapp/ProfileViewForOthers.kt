package com.example.instaapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.instaapp.mvvm.ViewModel

class ProfileViewForOthers : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view_for_others)


        viewModel = ViewModel(application) // Инициализируем ViewModel

        val userId = intent.getStringExtra("userId")
        if (userId != null) {
            loadUserProfile(userId)
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModel.loadUserProfile(userId) { user ->
            if (user != null) {

                val nameTextView = findViewById<TextView>(R.id.username)
                val followersTextView = findViewById<TextView>(R.id.following_text)
                val followingTextView = findViewById<TextView>(R.id.followers_text)

                nameTextView.text = user.username
                followersTextView.text = user.followers.toString()
                followingTextView.text = user.following.toString()

                // Дополнительный функционал, если требуется
            } else {
                Log.e("ProfileViewForOthers", "Failed to load user profile")
                // Дополнительная обработка ошибки, если требуется
            }
        }
    }

}