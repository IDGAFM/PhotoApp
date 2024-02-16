package com.example.instaapp.adapters

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.GestureDetectorCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instaapp.R
import com.example.instaapp.Utils
import com.example.instaapp.modal.Feed
import com.example.instaapp.mvvm.ViewModel
import com.example.instaapp.preferences.AppPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*


class MyFeedAdapter(private val vm: ViewModel, private val context: Context) : RecyclerView.Adapter<MyFeedAdapter.FeedHolder>() {

    private var feedList = listOf<Feed>()
    private var doubleTapListener: OnDoubleTapClickListener? = null
    private var userImageClickListener: OnUserImageClickListener? = null
    private var chatMessageClickListener: OnChatMessageClickListener? = null
    private val appPreferences = AppPreferences()
    private val likeValueEventListeners = mutableMapOf<String, ValueEventListener>()



    private fun checkIfPostLiked(feed: Feed, holder: FeedHolder) {
        appPreferences.isPostLiked(Utils.getUiLoggedIn(), feed.postid ?: "") { isLiked ->

            val likeImageResource = if (isLiked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
            holder.beforelike.setImageResource(likeImageResource)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feeditem, parent, false)
        return FeedHolder(view)
    }

    override fun getItemCount(): Int {
        return feedList.size

    }
    fun setChatMessageClickListener(listener: OnChatMessageClickListener) {
        this.chatMessageClickListener = listener
    }

    override fun onBindViewHolder(holder: FeedHolder, position: Int) {
        val feed = feedList[position]


        val avatarUrl = feed.imageposter
        Glide.with(holder.itemView.context).load(avatarUrl).into(holder.userPosterImage)




        val likeValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLiked = snapshot.getValue(Boolean::class.java) ?: false
                val likeImageResource = if (isLiked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
                holder.beforelike.setImageResource(likeImageResource)
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработайте отмену, если это необходимо
            }
        }

        holder.userPosterImage.setOnClickListener {
            val userId = feed.userId
            val bundle = bundleOf("userId" to userId)
            it.findNavController().navigate(R.id.navigation_profile, bundle)
        }

        checkIfPostLiked(feed, holder)

        likeValueEventListeners[feed.postid ?: ""] = likeValueEventListener
        vm.appPreferences.addLikeChangeListener(feed.postid ?: "", likeValueEventListener)

        val caption = feed.caption ?: ""
        val boldText = if (caption.isNotBlank()) "<b>${feed.username}</b>" else ""
        val additionalText = if (caption.isNotBlank()) ": $caption" else ""
        val combinedText = boldText + additionalText
        val formattedText: Spanned = Html.fromHtml(combinedText)

        holder.userNameCaption.text = formattedText

        val date = Date(feed.time!!.toLong() * 1000)

        val now = System.currentTimeMillis()
        val timeDifference = now - date.time

        val instagramTimeFormat = when {
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
                val dateFormat = SimpleDateFormat("d MMM", Locale("ru"))
                dateFormat.format(date)
            }
        }

        holder.chatMessage.setOnClickListener {
            chatMessageClickListener?.onChatMessageClick(feed)
        }

        holder.time.text = instagramTimeFormat
        holder.userNamePoster.text = feed.username

        Glide.with(holder.itemView.context).load(feed.image).override(700, 700).centerCrop().into(holder.feedImage)

        Glide.with(holder.itemView.context).load(feed.imageposter).into(holder.userPosterImage)

        holder.likecount.text = "${feed.likes} ${context.getString(R.string._0_likes)}"

        val doubleClickGestureDetector = GestureDetectorCompat(
            holder.itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isEventOnView(e, holder.feedImage)) {
                        doubleTapListener?.onDoubleTap(feed)

                        val updatedIsLiked = !feed.isLiked
                        updateLikeImage(feed, updatedIsLiked)

                        (vm as? ViewModel)?.updateLikedPost(feed.postid ?: "", updatedIsLiked)

                        val likeImageResourceAfterClick = if (updatedIsLiked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
                        holder.beforelike.setImageResource(likeImageResourceAfterClick)

                        holder.baselineFavoriteAnimation.setImageResource(likeImageResourceAfterClick)
                        holder.baselineFavoriteAnimation.visibility = View.VISIBLE

                        val layoutParams = holder.baselineFavoriteAnimation.layoutParams as ViewGroup.MarginLayoutParams
                        layoutParams.topMargin = holder.feedImage.height / 2 - holder.baselineFavoriteAnimation.height / 2
                        layoutParams.leftMargin = holder.feedImage.width / 2 - holder.baselineFavoriteAnimation.width / 2
                        holder.baselineFavoriteAnimation.layoutParams = layoutParams

                        holder.baselineFavoriteAnimation.animate()
                            .alpha(0f)
                            .setDuration(1000)
                            .withEndAction {
                                holder.baselineFavoriteAnimation.visibility = View.INVISIBLE
                            }
                            .start()
                    }
                    return true
                }
            })

        holder.itemView.setOnTouchListener { _, event ->
            doubleClickGestureDetector.onTouchEvent(event)
            true
        }



        holder.beforelike.setOnClickListener {
            doubleTapListener?.onDoubleTap(feed)
            val updatedIsLiked = !feed.isLiked
            updateLikeImage(feed, updatedIsLiked)

            appPreferences.saveLikedPost(Utils.getUiLoggedIn(), feed.postid ?: "", updatedIsLiked.toString())

            val likeImageResourceAfterClick = if (updatedIsLiked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
            holder.beforelike.setImageResource(likeImageResourceAfterClick)

            holder.beforelike.setImageResource(likeImageResourceAfterClick)

            holder.baselineFavoriteAnimation.setImageResource(likeImageResourceAfterClick)
            holder.baselineFavoriteAnimation.visibility = View.VISIBLE

            val layoutParams = holder.baselineFavoriteAnimation.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = holder.feedImage.height / 2 - holder.baselineFavoriteAnimation.height / 2
            layoutParams.leftMargin = holder.feedImage.width / 2 - holder.baselineFavoriteAnimation.width / 2
            holder.baselineFavoriteAnimation.layoutParams = layoutParams

            holder.baselineFavoriteAnimation.animate()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction {
                    holder.baselineFavoriteAnimation.visibility = View.INVISIBLE
                }
                .start()
        }



    }



    interface OnDoubleTapClickListener {
        fun onDoubleTap(feed: Feed)
    }

    interface OnUserImageClickListener {
        fun onUserImageClick(feed: Feed)
    }

    interface OnPostClickList{
        fun onPostClick(feed: Feed)
    }

    interface OnChatMessageClickListener {
        fun onChatMessageClick(feed: Feed)
    }


    private fun updateLikeImage(feed: Feed, isLiked: Boolean) {
        feed.isLiked = isLiked

        if (isLiked) {
            feed.likes = feed.likes?.plus(1)
            feed.likers.add(Utils.getUiLoggedIn())
        } else {
            feed.likes = feed.likes?.minus(1)
            feed.likers.remove(Utils.getUiLoggedIn())
        }

        notifyItemChanged(feedList.indexOf(feed))
    }

    private fun isEventOnView(event: MotionEvent, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        return x >= location[0] && x <= location[0] + view.width && y >= location[1] && y <= location[1] + view.height
    }



    fun updateFeed(updatedFeed: Feed) {
        val index = feedList.indexOfFirst { it.postid == updatedFeed.postid }
        if (index != -1) {
            feedList = feedList.toMutableList().apply {
                this[index] = updatedFeed
            }
            notifyItemChanged(index)
        }
    }

    fun updateFeedList(newFeedList: List<Feed>) {
        feedList = newFeedList
        notifyDataSetChanged()
    }

    fun releaseListeners() {
        likeValueEventListeners.forEach { (postId, listener) ->
            vm.appPreferences.removeLikeChangeListener(postId, listener)
        }
        likeValueEventListeners.clear()
    }



    fun setFeedList(list: List<Feed>) {
        this.feedList = list
    }

    fun setDoubleTapClickListener(listener: OnDoubleTapClickListener) {
        this.doubleTapListener = listener
    }

    fun setUserImageClickListener(listener: OnUserImageClickListener) {
        this.userImageClickListener = listener
    }
    fun getFeedAtPosition(position: Int): Feed {
        return feedList[position]
    }

    inner class FeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNamePoster: TextView = itemView.findViewById(R.id.feedtopusername)
        val userNameCaption: TextView = itemView.findViewById(R.id.feedusernamecaption)
        val userPosterImage: CircleImageView = itemView.findViewById(R.id.userimage)
        val feedImage: ImageView = itemView.findViewById(R.id.feedImage)
        val time: TextView = itemView.findViewById(R.id.feedtime)
        val likecount: TextView = itemView.findViewById(R.id.likecount)
        val beforelike: ImageView = itemView.findViewById(R.id.beforelike)
        val baselineFavoriteAnimation: ImageView = itemView.findViewById(R.id.baselineFavoriteAnimation)
        val chatMessage: ImageView = itemView.findViewById(R.id.chat_message)



    }
}