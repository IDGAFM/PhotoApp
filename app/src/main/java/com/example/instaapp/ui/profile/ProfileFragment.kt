package com.example.instaapp.ui.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.instaapp.FollowersActivity
import com.example.instaapp.FollowingActivity
import com.example.instaapp.PostViewActivity
import com.example.instaapp.R
import com.example.instaapp.SettingsActivity
import com.example.instaapp.Utils
import com.example.instaapp.adapters.MyPostAdapter
import com.example.instaapp.adapters.OnPostClickListener
import com.example.instaapp.modal.Posts
import com.example.instaapp.mvvm.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {

private lateinit var viewModel: ViewModel
private lateinit var myPostAdapter: MyPostAdapter

private lateinit var profileImage: CircleImageView
private lateinit var postsCountText: TextView
private lateinit var postsText: TextView
private lateinit var followersCountText: TextView
private lateinit var followersText: TextView
private lateinit var followingCountText: TextView
private lateinit var followingText: TextView
private lateinit var editProfileBtn: Button
private lateinit var imagesRecycler: RecyclerView


private lateinit var firebaseAuth: FirebaseAuth
private lateinit var fireStore: FirebaseFirestore
private lateinit var editText : EditText


private lateinit var storageRef: StorageReference
private lateinit var storage: FirebaseStorage

private lateinit var profileImagecard: CircleImageView
private lateinit var profileBitmap: Bitmap
private lateinit var swipeRefreshLayout: SwipeRefreshLayout


override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_profile, container, false)
    swipeRefreshLayout = view.findViewById(R.id.SwipeRefreshLayout)
    profileImage = view.findViewById(R.id.profile_image)
    postsCountText = view.findViewById(R.id.posts_count_text)
    postsText = view.findViewById(R.id.posts_text)
    followersCountText = view.findViewById(R.id.followers_count_text)
    followersText = view.findViewById(R.id.followers_text)
    followingCountText = view.findViewById(R.id.following_count_text)
    followingText = view.findViewById(R.id.following_text)
    editProfileBtn = view.findViewById(R.id.edit_profile_btn)
    imagesRecycler = view.findViewById(R.id.images_recycler)


    fireStore = FirebaseFirestore.getInstance()
    firebaseAuth = FirebaseAuth.getInstance()
    storage = FirebaseStorage.getInstance()
    storageRef = storage.reference


    swipeRefreshLayout.setOnRefreshListener {
        refreshProfileData()
    }


    viewModel = ViewModelProvider(this)[ViewModel::class.java]

    loadProfileImage()

    viewModel.name.observe(viewLifecycleOwner) { name ->
        (activity as AppCompatActivity).supportActionBar?.title = name
    }


    viewModel.followers.observe(viewLifecycleOwner) { followers ->
        followersCountText.text = followers
    }

    viewModel.following.observe(viewLifecycleOwner) { following ->
        followingCountText.text = following
    }


    profileImage.setOnClickListener(){
        alertDialogProfile()

    }



    followersText.setOnClickListener {
        val intent = Intent(requireContext(), FollowersActivity::class.java)
        startActivity(intent)
    }

    followersCountText.setOnClickListener {
        val intent = Intent(requireContext(), FollowersActivity::class.java)
        startActivity(intent)
    }

    followingText.setOnClickListener {
        val intent = Intent(requireContext(), FollowingActivity::class.java)
        startActivity(intent)
    }

    followingCountText.setOnClickListener {
        val intent = Intent(requireContext(), FollowingActivity::class.java)
        startActivity(intent)
    }

    editProfileBtn.setOnClickListener {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dialog_layout, null)
        profileImagecard = customView.findViewById(R.id.userProfileImage)
        editText = customView.findViewById(R.id.edit_username)


        viewModel.image.observe(viewLifecycleOwner) {
            Glide.with(requireContext()).load(it).into(profileImagecard)
        }

        viewModel.name.observe(viewLifecycleOwner) {
            editText.setText(it)
        }


        profileImagecard.setOnClickListener {
            alertDialogProfile()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_profile_card))
            .setView(customView)
            .setPositiveButton(getString(R.string.done)) { dialog, which ->
                val inputText = editText.text.toString()

                fireStore.collection("Users").document(Utils.getUiLoggedIn()).update("username", inputText.toString())


                val collectionref = fireStore.collection("Posts")
                val query = collectionref.whereEqualTo("userid", Utils.getUiLoggedIn())

                query.get().addOnSuccessListener { documents->

                    for (document in documents){

                        collectionref.document(document.id).update("username" , inputText)
                    }
                }
            }
            .setNegativeButton(getString(R.string.consel), null)
            .create()

        dialog.show()




    }



    return view
}

    private fun alertDialogProfile(){

        val options = arrayOf<CharSequence>(getString(R.string.take_Photo), getString(R.string.choosefromgal), getString(R.string.delete_avatar), getString(R.string.consel))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.profile_pick))
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == getString(R.string.take_Photo) -> {

                    profilePhotoWithCamera()


                }
                options[item] == getString(R.string.choosefromgal) -> {
                    profileImageFromGallery()
                }

                options[item] == getString(R.string.delete_avatar) -> {
                    deleteProfileImage()
                }
                options[item] == getString(R.string.consel) -> dialog.dismiss()


            }
        }
         builder.show()

    }

    private fun deleteProfileImage() {
        val storagePath = storageRef.child("avatars/${Utils.getUiLoggedIn()}.jpg")
        storagePath.delete().addOnSuccessListener {

            fireStore.collection("Users").document(Utils.getUiLoggedIn())
                .update("imageUrl", null)
                .addOnSuccessListener {

                    profileImage.setImageResource(R.drawable.person)
                    Toast.makeText(context, "Аватарка успешно удалена", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка при удалении аватарки", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(context, "Ошибка при удалении аватарки", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun profileImageFromGallery() {
        val pickPictureIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (pickPictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(pickPictureIntent, Utils.PROFILE_IMAGE_PICK)
        }



    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun profilePhotoWithCamera() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, Utils.PROFILE_IMAGE_CAPTURE)


    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Utils.PROFILE_IMAGE_CAPTURE -> {
                    val profilebitmap = data?.extras?.get("data") as Bitmap

                    uploadProfile(profilebitmap)
                }
                Utils.PROFILE_IMAGE_PICK -> {
                    val profileUri = data?.data
                    val profilebitmap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, profileUri)
                    uploadProfile(profilebitmap)

                }
            }
        }

    }

    private fun uploadProfile(imageBitmap: Bitmap?) {
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        profileBitmap = imageBitmap!!

        profileImage.setImageBitmap(imageBitmap)

        val storagePath = storageRef.child("avatars/${Utils.getUiLoggedIn()}.jpg")
        val uploadTask = storagePath.putBytes(data)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            val uriProfile = taskSnapshot.storage.downloadUrl
            uriProfile.addOnSuccessListener { taskSnapshot  ->
                updateProfileImage()
                refreshProfileData()

                fireStore.collection("Users").document(Utils.getUiLoggedIn()).update("imageUrl", taskSnapshot .toString())
                fireStore.collection("Users").document(Utils.getUiLoggedIn()).update("image", taskSnapshot .toString())
                val collectionre = fireStore.collection("Posts")
                val query = collectionre.whereEqualTo("userid", Utils.getUiLoggedIn())
                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        collectionre.document(document.id).update("imageposter", taskSnapshot .toString())
                    }
                }
                viewModel.image.value = taskSnapshot .toString()



            }

            Toast.makeText(context, getString(R.string.image_uploaded), Toast.LENGTH_SHORT).show()

        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateProfileImage() {
        firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            fireStore.collection("Users").document(userId).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val imageUrl = documentSnapshot.getString("imageUrl")

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(profileImage)
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val currentThemeId = requireContext().theme.getThemeResId()
        val menuResId = if (currentThemeId == R.style.Theme_InstaApp) {
            R.menu.top_nav_menu
        } else {
            R.menu.top_nav_menu
        }
        inflater.inflate(menuResId, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }


    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_setting -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun Resources.Theme.getThemeResId(): Int {
        val outValue = TypedValue()
        resolveAttribute(android.R.attr.theme, outValue, true)
        return outValue.resourceId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        viewModel.getMyPosts().observe(viewLifecycleOwner) { postsList ->
            myPostAdapter.setPostList(postsList)

            postsCountText.text = postsList.size.toString()
        }
        loadProfileImage()


    }

    private fun refreshProfileData() {
        updateProfileImage()
        myPostAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
    }


    private fun loadProfileImage() {
        firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            fireStore.collection("Users").document(userId).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val imageUrl = documentSnapshot.getString("imageUrl")

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .into(profileImage)

                        myPostAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }


    private fun setupRecyclerView() {
        myPostAdapter = MyPostAdapter(object : OnPostClickListener {
            override fun onPostClick(post: Posts) {
                val intent = Intent(requireContext(), PostViewActivity::class.java)
                intent.putExtra("postId", post.postid ?: "")
                intent.putExtra("postImage", post.image ?: "")
                intent.putExtra("postUsername", viewModel.name.value ?: "")
                intent.putExtra("postTime", post.time ?: 0L)
                intent.putExtra("postLikes", post.likes ?: 0)
                intent.putExtra("postCaption", post.caption ?: "")
                startActivity(intent)
            }
        })
        imagesRecycler.layoutManager = GridLayoutManager(context, 3)
        imagesRecycler.adapter = myPostAdapter
        }
    }
