package com.example.instaapp.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.example.instaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class UserProfilePreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUser = firebaseAuth.currentUser
    private val userId = currentUser?.uid
    private lateinit var userProfileImage: CircleImageView
    private lateinit var userProfileName: TextView
    private var dataLoadedListener: (() -> Unit)? = null

    interface UserProfileDataListener {
        fun onDataLoaded()
    }

    fun setDataLoadedListener(listener: () -> Unit) {
        dataLoadedListener = listener
    }

    private fun notifyDataLoaded() {
        dataLoadedListener?.invoke()
    }

    init {
        layoutResource = R.layout.preference_user_info
    }

    fun setUserName(userName: String?) {
        if (::userProfileName.isInitialized) {
            userProfileName.text = userName ?: context.getString(R.string.user_name)
        }
    }

    fun setUserAvatarUrl(avatarUrl: String?) {
        if (::userProfileImage.isInitialized) {
            Glide.with(context)
                .load(avatarUrl)
                .into(userProfileImage)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        userProfileImage = holder.findViewById(R.id.profile_image) as CircleImageView
        userProfileName = holder.findViewById(R.id.profile_name) as TextView
        notifyDataLoaded()
        userId?.let {
            firestore.collection("Users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("username")
                        val userAvatarUrl = document.getString("imageUrl")

                        // Находим элементы пользовательского макета

                        setUserName(userName)
                        setUserAvatarUrl(userAvatarUrl)

                        // Уведомляем слушателя о загрузке данных
                        notifyDataLoaded()

                        }
                    }
                }
        }
    }

