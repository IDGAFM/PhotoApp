package com.example.instaapp.mvvm

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.instaapp.Utils
import com.example.instaapp.modal.Feed
import com.example.instaapp.modal.Posts
import com.example.instaapp.modal.User
import com.example.instaapp.preferences.AppPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ViewModel(application: Application) : AndroidViewModel(application){


    val name = MutableLiveData<String>()
    val image = MutableLiveData<String>()
    val followers = MutableLiveData<String>()
    val following = MutableLiveData<String>()

    private val context: Context = application.applicationContext

    private val firestore = FirebaseFirestore.getInstance()
    private var lastSearchQuery: String? = null
    private val searchResults: MutableLiveData<List<User>> = MutableLiveData()
    private val searchQuery = MutableLiveData<String>()

    val appPreferences: AppPreferences = AppPreferences()

    private val likedPosts = mutableMapOf<String, Boolean>()


    init {

        getCurrentUser()

    }

    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Users").document(Utils.getUiLoggedIn()).addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (value != null && value.exists()) {
                val users = value.toObject(User::class.java)
                if (users != null) {
                    name.value = users.username ?: ""
                    image.value = users.imageUrl ?: ""
                    followers.value = users.followers?.toString() ?: ""
                    following.value = users.following?.toString() ?: ""
                }
            }
        }

    }


    fun getMyPosts(): LiveData<List<Posts>> {
        val posts = MutableLiveData<List<Posts>>()
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Posts")
                    .whereEqualTo("userid", Utils.getUiLoggedIn())
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            // Handle the exception here
                            return@addSnapshotListener
                        }

                        val postList = snapshot?.documents?.mapNotNull {
                            it.toObject(Posts::class.java)
                        }
                            ?.sortedByDescending { it.time
                            }

                        posts.postValue(postList!!)
                    }
            } catch (e: Exception) {
                //  exceptions
            }
        }


        return posts
    }

    fun getAllUsers(): LiveData<List<User>> {
        val users = MutableLiveData<List<User>>()
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Users").addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        return@addSnapshotListener
                    }

                    val usersList = mutableListOf<User>()
                    snapshot?.documents?.forEach { document ->
                        val user = document.toObject(User::class.java)
                        if (user != null && user.userid != Utils.getUiLoggedIn()) {
                            usersList.add(user)
                        }
                    }

                    val filteredUsers = usersList.filter { user ->
                        user.username?.contains(searchQuery.value.orEmpty(), ignoreCase = true) == true
                    }

                    users.postValue(filteredUsers)
                }
            } catch (e: Exception) {
                // exceptions
            }
        }

        return users
    }


    fun performSearch(query: String) {
        lastSearchQuery = query.toLowerCase()
        firestore.collection("Users")
            .get()
            .addOnSuccessListener { result ->
                val usersList = mutableListOf<User>()
                for (document in result) {
                    val user = document.toObject(User::class.java)
                    val username = user.username?.toLowerCase() ?: ""
                    if (user != null && user.userid != Utils.getUiLoggedIn() && username.contains(lastSearchQuery.orEmpty())) {
                        usersList.add(user)
                    }
                }
                searchResults.postValue(usersList)
            }
            .addOnFailureListener { exception ->
            }
    }


    fun loadMyFeed(): LiveData<List<Feed>> {
        val firestore = FirebaseFirestore.getInstance()

        val feeds = MutableLiveData<List<Feed>>()

        viewModelScope.launch(Dispatchers.IO) {

            getThePeopleIFollow { list ->
                try {
                    firestore.collection("Posts").whereIn("userid", list)
                        .addSnapshotListener { value, error ->
                            if (error != null) {
                                return@addSnapshotListener
                            }

                            val feed = mutableListOf<Feed>()
                            value?.documents?.forEach { documentSnapshot ->
                                val pModal = documentSnapshot.toObject(Feed::class.java)
                                pModal?.let {
                                    // Добавляем информацию о лайках
                                    viewModelScope.async {
                                        it.isLiked = checkIfPostIsLiked(documentSnapshot.id)
                                    }
                                    feed.add(it)
                                }
                            }

                            val sortedFeed = feed.sortedByDescending { it.time }

                            feeds.postValue(sortedFeed)
                        }

                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }
        return feeds
    }

    fun getLikedPostsMap(): Map<String, Boolean> {
        return likedPosts
    }

    fun updateLikedPost(postId: String, isLiked: Boolean) {
        likedPosts[postId] = isLiked
    }

    private suspend fun checkIfPostIsLiked(postId: String): Boolean = coroutineScope {
        val currentUserId = Utils.getUiLoggedIn()
        val firestore = FirebaseFirestore.getInstance()

        try {
            val document = firestore.collection("Posts").document(postId).get().await()
            val likers = document.get("likers") as? List<String>

            likers?.contains(currentUserId) == true
        } catch (e: Exception) {
            false
        }
    }


    fun getSearchResults(): LiveData<List<User>> {
        return searchResults
    }

    fun getThePeopleIFollow(callback: (List<String>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        val ifollowlist = mutableListOf<String>()
        ifollowlist.add(Utils.getUiLoggedIn())

        firestore.collection("Follower").document(Utils.getUiLoggedIn())
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val followingIds = documentSnapshot.get("following_id") as? List<String>
                    val updatedList = followingIds?.toMutableList() ?: mutableListOf()

                    ifollowlist.addAll(updatedList)

                    Log.e("ListOfFeed", ifollowlist.toString())
                    callback(ifollowlist)
                } else {
                    callback(ifollowlist)
                }
            }
    }

    fun loadUserProfile(userId: String, callback: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Users").document(userId).get().addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    callback(user)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching user profile: ", exception)
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile: ", e)
                callback(null)
            }
        }
    }
}