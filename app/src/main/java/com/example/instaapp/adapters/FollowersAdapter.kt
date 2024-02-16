package com.example.instaapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instaapp.R
import com.example.instaapp.Utils
import com.example.instaapp.modal.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView

class FollowerAdapter(private val context: Context) : RecyclerView.Adapter<FollowerAdapter.FollowerHolder>() {

    var listOfFollowers = listOf<User>()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.userlist, parent, false)
        firestore = FirebaseFirestore.getInstance()
        return FollowerHolder(view)
    }

    override fun getItemCount(): Int {
        return listOfFollowers.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FollowerHolder, position: Int) {
        val follower = listOfFollowers[position]
        holder.userName.text = follower.username

        val followerDocRef = firestore.collection("Users").document(follower.userid ?: "")
        followerDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val imageUrl = documentSnapshot.getString("imageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .into(holder.userImage)
                }
            }
        }

        firestore.collection("Follower").document(Utils.getUiLoggedIn()).addSnapshotListener{value, error ->
            if (error != null){
                return@addSnapshotListener
            }
            if (value != null && value.exists()){
                val followingId = value.get("following_id") as? List<String>
                if (followingId != null) {
                    if (followingId.contains(follower.userid)) {
                        holder.toggle.text = context.getString(R.string.unfollow)
                    } else {
                        holder.toggle.text = context.getString(R.string.follow)
                    }
                }
            }

            holder.toggle.setOnClickListener{
                val  buttontxt = holder.toggle.text.toString()
                if (buttontxt == context.getString(R.string.follow)){
                    followUser(follower)
                } else {
                    unfollowUser(follower)
                }
            }

        }

    }

    fun followUser(user: User) {
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())
        val userToFollowDocRef = firestore.collection("Users").document(user.userid!!)
        val followDocRef = firestore.collection("Follower").document(Utils.getUiLoggedIn())

        val batch = firestore.batch()

        batch.set(followDocRef, hashMapOf<String, Any>(), SetOptions.merge())

        batch.update(userDocRef, "following", FieldValue.increment(1))
        batch.update(userToFollowDocRef, "followers", FieldValue.increment(1))
        batch.update(followDocRef, "following_id", FieldValue.arrayUnion(user.userid))

        batch.commit()
            .addOnFailureListener { exception ->
                Log.e("UserAdapter", "Error following user: $exception")
            }
    }

    fun unfollowUser(user: User) {
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())
        val userToUnFollowDocRef = firestore.collection("Users").document(user.userid!!)
        val followDocRef = firestore.collection("Follower").document(Utils.getUiLoggedIn())

        val batch = firestore.batch()

        batch.update(userToUnFollowDocRef, "followers", FieldValue.increment(-1))
        batch.update(userDocRef, "following", FieldValue.increment(-1))
        batch.update(followDocRef, "following_id", FieldValue.arrayRemove(user.userid))

        batch.commit()
            .addOnFailureListener { exception ->
                Log.e("UserAdapter", "Error unfollowing user: $exception")
            }
    }



    fun setFollowerList(list: List<User>) {
        this.listOfFollowers = list
        notifyDataSetChanged()
    }

    inner class FollowerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: CircleImageView = itemView.findViewById(R.id.userImage_follow)
        val userName: TextView = itemView.findViewById(R.id.userName_follow)
        val toggle: Button = itemView.findViewById(R.id.toggle)
    }
}
