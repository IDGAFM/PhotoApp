package com.example.instaapp

import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instaapp.adapters.FollowerAdapter
import com.example.instaapp.modal.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FollowingActivity : AppCompatActivity() {


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

        supportActionBar?.title = getString(R.string.following)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        updateHomeAsUpIndicator()

        getUsersFollowedByCurrentUser()
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


    private fun Resources.Theme.getThemeResId(): Int {
        val outValue = TypedValue()
        resolveAttribute(android.R.attr.theme, outValue, true)
        return outValue.resourceId
    }


    private fun getUsersFollowedByCurrentUser() {
        val currentUserUid = auth.currentUser?.uid ?: return

        firestore.collection("Follower")
            .document(currentUserUid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val followingIds = documentSnapshot.get("following_id") as? List<String>
                    if (!followingIds.isNullOrEmpty()) {
                        loadFollowingUsers(followingIds)
                    }
                }
            }
            .addOnFailureListener { exception ->
            }
    }

    private fun loadFollowingUsers(followingIds: List<String>) {
        val followingUsers = mutableListOf<User>()

        for (userId in followingIds) {
            firestore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val user = documentSnapshot.toObject(User::class.java)
                        if (user != null) {
                            followingUsers.add(user)
                            followerAdapter.setFollowerList(followingUsers)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                }
        }
    }
}

