package com.example.instaapp

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instaapp.adapters.FollowerAdapter
import com.example.instaapp.modal.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FollowersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var followerAdapter: FollowerAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        recyclerView = findViewById(R.id.followers_list)
        followerAdapter = FollowerAdapter(this)
        recyclerView.adapter = followerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        supportActionBar?.title =getString(R.string.followers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        updateHomeAsUpIndicator()

        getUsersFollowingCurrentUser()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun updateHomeAsUpIndicator() {
        if (isDarkTheme()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_white_back24)
        } else {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        }
    }

    private fun isDarkTheme(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getUsersFollowingCurrentUser() {
        val currentUserUid = auth.currentUser?.uid ?: return

        firestore.collection("Follower")
            .whereArrayContains("following_id", currentUserUid)
            .get()
            .addOnSuccessListener { documents ->
                val followers = mutableListOf<User>()
                for (document in documents) {
                    val followerUid = document.id
                    val followerUserRef = firestore.collection("Users").document(followerUid)
                    followerUserRef.get().addOnSuccessListener { followerDocument ->
                        if (followerDocument.exists()) {
                            val follower = followerDocument.toObject(User::class.java)
                            follower?.let { followers.add(it) }
                            followerAdapter.setFollowerList(followers)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
            }
    }
}
