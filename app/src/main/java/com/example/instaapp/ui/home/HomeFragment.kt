package com.example.instaapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.instaapp.CommentsActivity
import com.example.instaapp.R
import com.example.instaapp.Utils
import com.example.instaapp.adapters.MyFeedAdapter
import com.example.instaapp.modal.Feed
import com.example.instaapp.mvvm.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var vm: ViewModel
    private lateinit var adapter: MyFeedAdapter
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ViewModel::class.java)
        adapter = MyFeedAdapter(vm, requireContext())


        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            updateFeedList()
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.feed_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        vm.loadMyFeed().observe(viewLifecycleOwner, Observer { feedList ->
            adapter.setFeedList(feedList)
            adapter.notifyDataSetChanged()
        })

        adapter.setDoubleTapClickListener(object : MyFeedAdapter.OnDoubleTapClickListener {
            override fun onDoubleTap(feed: Feed) {
                handleDoubleTap(feed)
            }
        })
        adapter.setUserImageClickListener(object : MyFeedAdapter.OnUserImageClickListener {
            override fun onUserImageClick(feed: Feed) {

            }
        })

        adapter.setChatMessageClickListener(object : MyFeedAdapter.OnChatMessageClickListener {
            override fun onChatMessageClick(feed: Feed) {
                val intent = Intent(requireContext(), CommentsActivity::class.java)
                intent.putExtra("postId", feed.postid)
                startActivity(intent)
            }
        })
    }

    private fun updateFeedList() {

        vm.loadMyFeed().observe(viewLifecycleOwner, Observer { newFeedList ->
            adapter.setFeedList(newFeedList)
            adapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        })
    }




    private fun handleDoubleTap(feed: Feed) {
        val currentUserId = Utils.getUiLoggedIn()
        val postId = feed.postid

        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("Posts").document(postId!!)

        postRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val likes = document.getLong("likes")?.toInt() ?: 0
                val likers = document.get("likers") as? List<String>

                if (!likers.isNullOrEmpty() && likers.contains(currentUserId)) {
                    postRef.update(
                        "likes", likes - 1,
                        "likers", FieldValue.arrayRemove(currentUserId)
                    )
                        .addOnSuccessListener {
                            feed.likes = likes - 1
                            feed.likers.remove(currentUserId)
                            adapter.updateFeed(feed)
                        }
                } else {
                    postRef.update(
                        "likes", likes + 1,
                        "likers", FieldValue.arrayUnion(currentUserId)
                    )
                        .addOnSuccessListener {
                            feed.likes = likes + 1
                            feed.likers.add(currentUserId)
                            adapter.updateFeed(feed)
                        }
                }
            }
        }
    }

}
