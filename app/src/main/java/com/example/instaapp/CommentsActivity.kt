package com.example.instaapp

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.util.Log
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instaapp.adapters.CommentsAdapter
import com.example.instaapp.adapters.MyFeedAdapter
import com.example.instaapp.modal.Comment
import com.example.instaapp.modal.Feed
import com.example.instaapp.modal.Posts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

class CommentsActivity : AppCompatActivity(), CommentsAdapter.CommentInteractionListener {

    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var postId: String
    private lateinit var postImageView: ImageView
    private lateinit var postUserAvatarImageView: ImageView
    private lateinit var postUserNameTextView: TextView
    private lateinit var postTimeTextView: TextView
    private lateinit var postLikesTextView: TextView
    private lateinit var postDescriptionTextView: TextView
    private lateinit var commentsRecyclerView: RecyclerView

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var commentTime: TextView
    private lateinit var rootView: RelativeLayout
    private var keyboardVisible = false
    private lateinit var addCommentButton:Button
    private lateinit var commentEditText:EditText
    private lateinit var progressDialog: ProgressDialog
    private lateinit var beforelike: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        supportActionBar?.title = getString(R.string.comm)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        updateHomeAsUpIndicator()

        postId = intent.getStringExtra("postId") ?: ""

        postImageView = findViewById(R.id.feedImage)
        postUserAvatarImageView = findViewById(R.id.userimage)
        postUserNameTextView = findViewById(R.id.feedtopusername)
        postTimeTextView = findViewById(R.id.feedtime)
        postLikesTextView = findViewById(R.id.likecount)
        postDescriptionTextView = findViewById(R.id.feedusernamecaption)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        beforelike = findViewById(R.id.beforelike)


        commentTime = findViewById(R.id.commentTime) ?: TextView(this)
        commentsAdapter = CommentsAdapter(commentTime,  postId,  this, this)
        commentsAdapter.setPostId(postId)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter

        addCommentButton = findViewById(R.id.buttonAddComment)
        commentEditText = findViewById(R.id.editTextComment)
        rootView = findViewById(R.id.rootView)

        loadPostById(postId)
        loadComments()

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.publishing))
        progressDialog.setCancelable(false)


        checkIfPostLiked(postId)


        beforelike.setOnClickListener {
            toggleLikeState(postId)
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - r.bottom
                keyboardVisible = keypadHeight > screenHeight * 0.15

                if (keyboardVisible) {
                    moveViewsForKeyboard(true)
                } else {
                    moveViewsForKeyboard(false)
                }
            }
        })


        addCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
                commentEditText.text.clear()
            } else {
            }
        }

    }

    private fun checkIfPostLiked(postId: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val postRef = firestore.collection("Posts").document(postId)

            postRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val likes = document.getLong("likes") ?: 0

                        val likers = document.get("likers") as? List<String> ?: mutableListOf()

                        val isLiked = likers.contains(currentUser.uid)

                        if (isLiked) {
                            beforelike.setImageResource(R.drawable.baseline_favorite_24)
                        } else {
                            beforelike.setImageResource(R.drawable.baseline_favorite_border_24)
                        }
                    }
                } else {
                    Log.d(TAG, "Error getting document: ", task.exception)
                }
            }
        }
    }



    private fun toggleLikeState(postId: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val postRef = firestore.collection("Posts").document(postId)

            postRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val likes = document.getLong("likes") ?: 0

                        val likers = document.get("likers") as? List<String> ?: mutableListOf()

                        val isLiked = likers.contains(currentUser.uid)

                        val updatedLikes = if (isLiked) likes - 1 else likes + 1
                        val updatedLikers = if (isLiked) likers - currentUser.uid else likers + currentUser.uid

                        postRef.update(
                            "likes", updatedLikes,
                            "likers", updatedLikers
                        ).addOnSuccessListener {
                            updateLikeButtonState(isLiked)
                            updateLikeCount(updatedLikes)
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error updating document", e)
                        }
                    }
                } else {
                    Log.d(TAG, "Error getting document: ", task.exception)
                }
            }
        }
    }

    private fun updateLikeButtonState(isLiked: Boolean) {
        val likeImageResource = if (isLiked) R.drawable.baseline_favorite_border_24 else R.drawable.baseline_favorite_24
        beforelike.setImageResource(likeImageResource)

    }

    private fun updateLikeCount(likes: Long) {
        postLikesTextView.text = "$likes ${getString(R.string._0_likes)}"
    }


    override fun onCommentAdded(postId: String) {
        this.postId = postId

    }



    private fun showProgressDialog() {
        progressDialog.show()
    }

    private fun dismissProgressDialog() {
        progressDialog.dismiss()
    }



    private fun moveViewsForKeyboard(up: Boolean) {
        val layoutParams = commentsRecyclerView.layoutParams as ConstraintLayout.LayoutParams
        val distanceToMove = resources.getDimensionPixelSize(R.dimen.keyboard_offset)

        if (up) {
            layoutParams.bottomMargin = distanceToMove
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } else {
            layoutParams.bottomMargin = 0
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        }

        commentsRecyclerView.layoutParams = layoutParams

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

    private fun loadPostById(postId: String?) {
        firestore = FirebaseFirestore.getInstance()
        val postDocRef = firestore.collection("Posts").document(postId!!)
        postDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val post = documentSnapshot.toObject(Posts::class.java)


                displayPostDetails(post)

                checkIfPostLiked(postId)
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

            postUserNameTextView.text = post.username
            loadProfileImage(post.userid, commentTime)
            FormatpostTime(post)
            val caption = post.caption ?: ""
            val boldText = if (caption.isNotBlank()) "<b>${post.username}</b>" else ""
            val additionalText = if (caption.isNotBlank()) ": $caption" else ""
            val combinedText = boldText + additionalText
            val formattedText: Spanned = Html.fromHtml(combinedText)

            postDescriptionTextView.text = formattedText
            postLikesTextView.text = "${post.likes} ${getString(R.string._0_likes)}"
        }
    }

    private fun loadProfileImage(userId: String?, commentTimeTextView: TextView) {
        if (userId != null) {

            val userDocRef = firestore.collection("Users").document(userId)
            userDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userImageUrl = documentSnapshot.getString("imageUrl")

                    if (!userImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(userImageUrl)
                            .into(postUserAvatarImageView)
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

        postTimeTextView.text = formattedText

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

        postTimeTextView.text = instagramTimeFormat
    }


    private fun loadComments() {
        firestore.collection("Posts").document(postId).collection("Comments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val commentsList = mutableListOf<Comment>()
                for (document in querySnapshot) {
                    val commentId = document.id
                    val comment = document.toObject(Comment::class.java)
                    comment.commentId = commentId
                    commentsList.add(comment)
                }

                loadUsersProfileImagesComm(commentsList)

                for (comment in commentsList) {
                    formatCommentTime(comment, commentTime)
                }

                commentsAdapter.setCommentsList(commentsList)
            }
            .addOnFailureListener { error ->
            }
    }

    private fun loadUsersProfileImagesComm(commentsList: List<Comment>) {

        for (comment in commentsList) {
            val userId = comment.userId
            if (!userId.isNullOrEmpty()) {
                firestore.collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument.exists()) {
                            val userImageUrl = userDocument.getString("imageUrl")
                            commentsAdapter.setUserAvatar(userId, userImageUrl)
                            commentsAdapter.notifyDataSetChanged()
                        }
                    }
            }
        }
    }

    private fun addComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        showProgressDialog()

        if (currentUser != null) {
            val userId = currentUser.uid

            val usersCollection = FirebaseFirestore.getInstance().collection("Users")
            usersCollection.document(userId).get()
                .addOnSuccessListener { userDocument ->
                    val username = userDocument.getString("username")
                    val image = userDocument.getString("imageUrl")

                    val newComment = Comment(
                        username = username,
                        time = System.currentTimeMillis(),
                        commentText = commentText,
                    )

                    addCommentToFirestore(newComment){
                        dismissProgressDialog()
                    }

                }
                .addOnFailureListener {

                }
            postId = intent.getStringExtra("postId") ?: ""
        }
    }

    private fun formatCommentTime(comment: Comment, commentTimeTextView: TextView) {
        val date = Date(comment.time!!.toLong() * 1000)
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

        commentTime.text = instagramTimeFormat
    }


    private fun addCommentToFirestore(newComment: Comment, onSuccess: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            val userDocRef = firestore.collection("Users").document(userId)
            userDocRef.get().addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val username = userDocument.getString("username")
                    val currentTime = System.currentTimeMillis()

                    val commentToAdd = newComment.copy(
                        userId = userId,
                        username = username  ,
                        time = currentTime
                    )

                    firestore.collection("Posts").document(postId).collection("Comments")
                        .add(commentToAdd)
                        .addOnSuccessListener { documentReference ->
                            val commentId = documentReference.id
                            documentReference.update("commentId", commentId)
                                .addOnSuccessListener {
                                    hideKeyboard()


                                    commentsAdapter.addComment(commentToAdd)
                                    commentToAdd.time = currentTime
                                    commentsAdapter.notifyDataSetChanged()

                                    loadComments()

                                    commentsRecyclerView.scrollToPosition(commentsAdapter.itemCount)
                                    onCommentAdded(postId)

                                    onSuccess()
                                }
                                .addOnFailureListener {
                                }
                        }
                        .addOnFailureListener {
                        }
                }
            }
        }
    }


    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onCommentDeleted(commentId: String) {
        val postId = intent.getStringExtra("postId") ?: ""
        val commentToDelete = commentsAdapter.getCommentById(commentId)
        if (commentToDelete != null) {
            deleteComment(postId, commentToDelete)
        }
    }


    override fun onCommentUpdated(comment: Comment) {
        val postId = intent.getStringExtra("postId") ?: ""
        loadComments()
        updateComment(postId, comment)
    }

    private fun deleteComment(postId: String, comment: Comment) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val postId = intent.getStringExtra("postId") ?: ""

        if (currentUser != null) {
            val userId = currentUser.uid

            if (userId == comment.userId) {
                val commentId = comment.commentId
                firestore.collection("Posts").document(postId).collection("Comments")
                    .document(commentId!!)
                    .delete()
                    .addOnSuccessListener {
                        loadComments()
                        commentsAdapter.removeCommentById(commentId)
                        Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "${getString(R.string.error)} ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val postDocRef = firestore.collection("Posts").document(postId)
                postDocRef.get().addOnSuccessListener { postDocument ->
                    if (postDocument.exists()) {
                        val postAuthorId = postDocument.getString("userid")
                        if (userId == postAuthorId) {
                            val commentId = comment.commentId
                            firestore.collection("Posts").document(postId).collection("Comments")
                                .document(commentId!!)
                                .delete()
                                .addOnSuccessListener {
                                    loadComments()
                                    commentsAdapter.removeCommentById(commentId)
                                    Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "${getString(R.string.error)} ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Вы не можете удалить этот комментарий", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }



    private fun updateComment(postId: String, comment: Comment) {
        val editText = EditText(this)
        editText.setText(comment.commentText)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_comment))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val updatedText = editText.text.toString().trim()
                if (updatedText.isNotEmpty()) {
                    firestore = FirebaseFirestore.getInstance()
                    firestore.collection("Posts").document(postId).collection("Comments")
                        .document(comment.commentId!!)
                        .update("commentText", updatedText)
                        .addOnSuccessListener {
                            loadComments()
                            commentsAdapter.updateComment(comment.commentId!!, updatedText)
                            Toast.makeText(this, getString(R.string.sucсessfull), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "${getString(R.string.error_edit)} ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, getString(R.string.can_not_be_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.discrad)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}