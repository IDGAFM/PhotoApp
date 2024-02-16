package com.example.instaapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instaapp.R
import com.example.instaapp.modal.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date
import java.util.Locale


class CommentsAdapter(private val commentTimeTextView: TextView, private var postId: String, private val listener: CommentInteractionListener, private val context: Context) : RecyclerView.Adapter<CommentsAdapter.CommentHolder>() {
    private var commentsList = listOf<Comment>()
    private val userAvatarUrls = mutableMapOf<String, String?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentHolder(view)
    }


    fun setPostId(postId: String) {
        this.postId = postId
    }


    fun setUserAvatar(userId: String, imageUrl: String?) {
        userAvatarUrls[userId] = imageUrl
    }

    override fun onBindViewHolder(holder: CommentHolder, position: Int) {
        val comment = commentsList[position]
        holder.commentUserName.text = comment.username
        holder.commentText.text = comment.commentText
        holder.commentTime.text = formatCommentTime(comment)
        loadProfileImageComm(comment.userId, holder.userAvatarImageView)

    }

    private fun loadProfileImageComm(userId: String?, circleImageView: CircleImageView) {
        if (!userId.isNullOrEmpty()) {
            val firestore = FirebaseFirestore.getInstance()

            val userDocRef = firestore.collection("Users").document(userId)
            userDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userImageUrl = documentSnapshot.getString("imageUrl")

                    if (!userImageUrl.isNullOrEmpty()) {
                        Glide.with(circleImageView.context)
                            .load(userImageUrl)
                            .into(circleImageView)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }
    fun removeCommentById(commentId: String) {
        commentsList = commentsList.filterNot { it.commentId == commentId }
        notifyDataSetChanged()
    }

    fun setCommentsList(list: List<Comment>) {
        this.commentsList = list.sortedByDescending { it.time }
        notifyDataSetChanged()
    }

    fun updateComment(commentId: String, updatedText: String) {
        commentsList.find { it.commentId == commentId }?.commentText = updatedText
        notifyDataSetChanged()
    }

    fun addComment(comment: Comment) {
        val updatedList = mutableListOf(comment)
        updatedList.addAll(commentsList)
        commentsList = updatedList.sortedByDescending { it.time }
        notifyDataSetChanged()
    }

    fun getCommentById(commentId: String): Comment? {
        for (comment in commentsList) {
            if (comment.commentId == commentId) {
                return comment
            }
        }
        return null
    }

    private fun formatCommentTime(comment: Comment): String {
        val date = Date(comment.time!!.toLong())
        val now = System.currentTimeMillis()

        val timeDifference = now - date.time

        return when {
            timeDifference < DateUtils.MINUTE_IN_MILLIS -> context.getString(R.string.now)
            timeDifference < DateUtils.HOUR_IN_MILLIS -> {
                val minutes = (timeDifference / DateUtils.MINUTE_IN_MILLIS).toInt()
                "$minutes ${context.getString(R.string.min)}"
            }
            timeDifference < DateUtils.DAY_IN_MILLIS -> {
                val hours = (timeDifference / DateUtils.HOUR_IN_MILLIS).toInt()
                "$hours ${context.getString(R.string.hour)}"
            }
            else -> {
                val dateFormat = java.text.SimpleDateFormat("d MMM", Locale("ru"))
                dateFormat.format(date)
            }
        }
    }

    interface CommentInteractionListener {
        fun onCommentDeleted(commentId: String)
        fun onCommentUpdated(comment: Comment)
        fun onCommentAdded(postId: String)
    }



    inner class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        val userAvatarImageView: CircleImageView = itemView.findViewById(R.id.commentUserAvatar)
        val commentUserName: TextView = itemView.findViewById(R.id.commentUserName)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val commentTime: TextView = itemView.findViewById(R.id.commentTime)

        init {
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(view: View): Boolean {
            val position = adapterPosition
            val firestore = FirebaseFirestore.getInstance()
            if (position != RecyclerView.NO_POSITION) {
                val comment = commentsList[position]

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser?.uid == comment.userId) {
                    val options = arrayOf(context.getString(R.string.delete), context.getString(R.string.edit))
                    AlertDialog.Builder(view.context)
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> deleteComment(comment)
                                1 -> editComment(comment)
                            }
                        }
                        .show()
                } else {
                    val postDocRef = firestore.collection("Posts").document(postId)
                    postDocRef.get().addOnSuccessListener { postDocument ->
                        if (postDocument.exists()) {
                            val postAuthorId = postDocument.getString("userid")
                            if (currentUser?.uid == postAuthorId) {
                                val options = arrayOf(context.getString(R.string.delete))
                                AlertDialog.Builder(view.context)
                                    .setItems(options) { _, _ ->
                                        deleteComment(comment)
                                    }
                                    .show()
                            } else {

                            }
                        }
                    }
                }
            }
            return true
        }



        private fun deleteComment(comment: Comment) {
            listener.onCommentDeleted(comment.commentId!!)
        }



        private fun editComment(comment: Comment) {
            listener.onCommentUpdated(comment)
        }


    }
}

