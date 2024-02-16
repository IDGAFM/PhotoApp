package com.example.instaapp

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.instaapp.modal.Posts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date
import java.util.Locale

class PostViewActivity : AppCompatActivity() {


    private lateinit var postImageView: ImageView
    private lateinit var postUsernameCaption: TextView
    private lateinit var postTime: TextView
    private lateinit var likeCount: TextView
    private lateinit var feedtopusername: TextView
    private lateinit var menuButton: ImageView
    private lateinit var userimage: CircleImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chat_message: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_view)

        val postId = intent.getStringExtra("postId") ?: ""

        postImageView = findViewById(R.id.feedImage)
        postUsernameCaption = findViewById(R.id.feedusernamecaption)
        postTime = findViewById(R.id.feedtime)
        likeCount = findViewById(R.id.likecount)
        feedtopusername = findViewById(R.id.feedtopusername)
        menuButton = findViewById(R.id.menuButton)
        userimage = findViewById(R.id.userimage)
        firestore = FirebaseFirestore.getInstance()
        chat_message = findViewById(R.id.chat_message)

        supportActionBar?.title = getString(R.string.posts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        updateHomeAsUpIndicator()
        setMenuButtonImage(theme.getThemeResId(), menuButton)

        menuButton.setOnClickListener(){
            showPopUpMenu(menuButton, postId)
        }



        chat_message.setOnClickListener(){
                val intent = Intent(this, CommentsActivity::class.java)
                intent.putExtra("postId", postId)
                startActivity(intent)

        }


        loadPostById(postId)

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

    override fun onResume() {
        super.onResume()
        updateHomeAsUpIndicator()

    }

    private fun showPopUpMenu(view: View, postId: String) {
        val popUpMenu = PopupMenu(this, view)
        val inflater: MenuInflater = popUpMenu.menuInflater
        inflater.inflate(R.menu.delete_edit_menu, popUpMenu.menu)

        popUpMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete -> {
                    deletePost(postId)
                    return@setOnMenuItemClickListener true
                }
                R.id.edit -> {
                    showEditPostDescriptionDialog(postId)
                    return@setOnMenuItemClickListener true
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
        }
        popUpMenu.show()
    }

    private fun setMenuButtonImage(themeResId: Int, imageView: ImageView) {
        val imageResId = if (themeResId == R.style.Theme_InstaApp) {
            if (isDarkTheme()) {
                R.drawable.baseline_more_vert_white_24
            } else {
                R.drawable.round_more_vert_24
            }
        } else {
            if (isDarkTheme()) {
                R.drawable.baseline_more_vert_white_24
            } else {
                R.drawable.round_more_vert_24
            }
        }
        imageView.setImageResource(imageResId)
    }


    private fun Resources.Theme.getThemeResId(): Int {
        val outValue = TypedValue()
        resolveAttribute(android.R.attr.theme, outValue, true)
        return outValue.resourceId
    }

    private fun showEditPostDescriptionDialog(postId: String) {
        val db = FirebaseFirestore.getInstance()
        val postDocRef = db.collection("Posts").document(postId)

        postDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val post = document.toObject(Posts::class.java)
                post?.let {
                    val currentDescription = it.caption

                    val editPostDialog = AlertDialog.Builder(this)
                    val editPostEditText = EditText(this)
                    editPostEditText.setText(currentDescription)

                    editPostDialog.setTitle(getString(R.string.editdescriptions))
                        .setView(editPostEditText)
                        .setPositiveButton(getString(R.string.save)) { dialog, which ->
                            val newDescription = editPostEditText.text.toString()
                            savePostDescription(postId, newDescription, post)
                        }
                        .setNegativeButton(R.string.discrad, null)
                        .show()
                }
            }
        }
    }

    private fun savePostDescription(postId: String, newDescription: String, post:Posts) {
        val db = FirebaseFirestore.getInstance()
        val postDocRef = db.collection("Posts").document(postId)

        postDocRef.update("caption", newDescription)
            .addOnSuccessListener {
                val boldText = "<b>${post.username}</b>"
                val combinedText = boldText + ": $newDescription"
                val formattedText: Spanned = Html.fromHtml(combinedText)
                postUsernameCaption.text = formattedText

            }
            .addOnFailureListener { e ->
                Log.e("PostViewActivity", "Ошибка при обновлении описания поста", e)
            }
    }




    private fun deletePost(postId: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("Photos")

        deletePostFromFirestore(postId)

        storageRef.listAll()
            .addOnSuccessListener { result ->
                val fileNames = result.items.map { it.name }
                deletePostAndFiles(fileNames)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseStorage", "Ошибка при получении списка файлов", e)
            }

        openProfileFragment()

    }

    private fun deletePostAndFiles(fileNames: List<String>) {

        fileNames.forEach { fileName ->
            val filePath = "Photos/$fileName"
            deleteFileFromStorage(filePath)
        }
    }

    private fun deletePostFromFirestore(postId: String) {
        val db = FirebaseFirestore.getInstance()
        val postsCollection = db.collection("Posts")

        postsCollection.document(postId).delete().addOnSuccessListener {
            val commentsCollection = db.collection("Posts").document(postId).collection("Comments")
            deleteCommentsCollection(commentsCollection)
        }.addOnFailureListener { exception ->
        }
    }

    private fun deleteCommentsCollection(commentsCollection: CollectionReference) {
        commentsCollection.get().addOnSuccessListener { querySnapshot ->
            val batch = commentsCollection.firestore.batch()

            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().addOnSuccessListener {
            }.addOnFailureListener { exception ->
            }
        }.addOnFailureListener { exception ->
        }
    }



    private fun deleteFileFromStorage(filePath: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child(filePath)

        storageRef.delete()
            .addOnSuccessListener {
                Log.d("FirebaseStorage", "Файл успешно удален из Storage")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseStorage", "Ошибка при удалении из Storage", e)
            }
    }

    private fun openProfileFragment(){
        onBackPressed()

    }


    private fun loadPostById(postId: String?) {
         firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        val postDocRef = firestore.collection("Posts").document(postId!!)
        postDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val post = documentSnapshot.toObject(Posts::class.java)

                if (post?.userid == firebaseAuth.currentUser?.uid) {
                    displayPostDetails(post)
                }
            }
        }
    }

    private fun displayPostDetails(post: Posts?) {
        if (post != null) {
            Glide.with(this)
                .load(post.image)
                .override(700, 700)
                .centerCrop()
                .into(postImageView)

            feedtopusername.text = post.username

            loadProfileImage(post.userid)

            FormatpostTime(post)
            val caption = post.caption ?: ""
            val boldText = if (caption.isNotBlank()) "<b>${post.username}</b>" else ""
            val additionalText = if (caption.isNotBlank()) ": $caption" else ""
            val combinedText = boldText + additionalText
            val formattedText: Spanned = Html.fromHtml(combinedText)

            postUsernameCaption.text = formattedText
            feedtopusername.text = post.username

            likeCount.text = "${post.likes} ${getString(R.string._0_likes)}"



        }
    }

    private fun loadProfileImage(userId: String?) {
        if (userId != null) {

            val userDocRef = firestore.collection("Users").document(userId)
            userDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userImageUrl = documentSnapshot.getString("imageUrl")

                    if (!userImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(userImageUrl)
                            .into(userimage)
                    }
                }
            }
        }
    }

    private fun FormatpostTime(post: Posts) {
        val boldText = "<b>${post.username}</b>"
        val additionalText = "${post.caption}"
        val combinedText = boldText + additionalText
        val formattedText: Spanned = Html.fromHtml(combinedText)

        feedtopusername.text = formattedText

        val date = Date(post.time!!.toLong() * 1000)

        val now = System.currentTimeMillis()
        val timeDifference = now - date.time

        val instagramTimeFormat = when {
            timeDifference < DateUtils.MINUTE_IN_MILLIS -> getString(R.string.now)
            timeDifference < DateUtils.HOUR_IN_MILLIS -> {
                val minutes = (timeDifference / DateUtils.MINUTE_IN_MILLIS).toInt()
                "$minutes ${getString(R.string.min)}"
            }

            timeDifference < DateUtils.DAY_IN_MILLIS -> {
                val hours = (timeDifference / DateUtils.HOUR_IN_MILLIS).toInt()
                "$hours ${getString(R.string.hour)}"
            }

            else -> {
                val dateFormat = java.text.SimpleDateFormat("d MMM", Locale("ru"))
                dateFormat.format(date)
            }
        }

        postTime.text = instagramTimeFormat
    }
}
