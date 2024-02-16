package com.example.instaapp.ui.create

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.instaapp.R
import com.example.instaapp.Utils
import com.example.instaapp.mvvm.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.ArrayList
import java.util.UUID

class CreateFragment : Fragment() {

    private lateinit var pd: ProgressDialog
    private lateinit var vm: ViewModel
    private lateinit var storageRef: StorageReference
    private lateinit var storage: FirebaseStorage
    private var uri: Uri? = null
    private lateinit var firestore: FirebaseFirestore

    private lateinit var bitmap: Bitmap
    private var postid: String = ""
    private var nameUserPoster: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ViewModel::class.java)

        postid = UUID.randomUUID().toString()

        pd = ProgressDialog(requireContext())

        firestore = FirebaseFirestore.getInstance()

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        vm.name.observe(viewLifecycleOwner, Observer { it ->
            nameUserPoster = it!!
        })

        view.findViewById<View>(R.id.imageToPost).setOnClickListener {
            addPostDialog()
        }

        view.findViewById<View>(R.id.postBtn).setOnClickListener {
            val caption = view.findViewById<EditText>(R.id.addCaption).text.toString()

            if (uri != null) {
                postImage(uri, caption)
                updateImagePosterAndPost(uri.toString())
            } else {
                Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addPostDialog() {
        val options = arrayOf<CharSequence>(getString(R.string.take_Photo), getString(R.string.choosefromgal),getString(R.string.consel))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.choesePhotoToPost))
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == getString(R.string.take_Photo) -> {
                    takePhotoWithCamera()
                }
                options[item] == getString(R.string.choosefromgal) -> {
                    pickImageFromGallery()
                }
                options[item] == getString(R.string.consel) -> dialog.dismiss()
            }
        }
        builder.show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun pickImageFromGallery() {
        val pickPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (pickPictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(pickPictureIntent, Utils.REQUEST_IMAGE_PICK)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhotoWithCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, Utils.REQUEST_IMAGE_CAPTURE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Utils.REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    uploadImageToFirebaseStorageAndPost(imageBitmap)
                }
                Utils.REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
                    uploadImageToFirebaseStorageAndPost(imageBitmap)
                }
            }
        }
    }

    private fun uploadImageToFirebaseStorageAndPost(imageBitmap: Bitmap) {
        val resizedBitmap = resizeBitmap(imageBitmap, 600, 600)

        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        bitmap = resizedBitmap

        bitmap = imageBitmap

        val storagePath = storageRef.child("Photos/${UUID.randomUUID()}.jpg")
        val uploadTask = storagePath.putBytes(data)

        uploadTask.addOnSuccessListener {
            val task = it.metadata?.reference?.downloadUrl

            task?.addOnSuccessListener { uri ->
                this.uri = uri
                view?.findViewById<ImageView>(R.id.imageToPost)?.setImageBitmap(bitmap)

                // Обновите изображение пользователя в базе данных и добавьте пост
            }
            Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun updateImagePosterAndPost(imageUrl: String) {
        firestore.collection("Users").document(Utils.getUiLoggedIn()).update("imageposter", imageUrl)
            .addOnSuccessListener {
                postImage(uri, view?.findViewById<EditText>(R.id.addCaption)?.text.toString())
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update image poster!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun postImage(uri: Uri?, caption: String) {
        val likers = ArrayList<String>()

        firestore.collection("Users").document(Utils.getUiLoggedIn()).get().addOnSuccessListener { userSnapshot ->
            val imageUrl = userSnapshot.getString("imageUrl")


            val hashMap = hashMapOf<Any, Any>(
                "image" to uri.toString(),
                "postid" to postid,
                "userid" to Utils.getUiLoggedIn(),
                "likers" to likers,
                "time" to Utils.getTime(),
                "caption" to caption,
                "likes" to 0,
                "username" to nameUserPoster,
                "imageposter" to imageUrl.toString()
            )

            firestore.collection("Posts").document(postid).set(hashMap)
                .addOnSuccessListener {
                    Toast.makeText(context, "Post added successfully!", Toast.LENGTH_SHORT).show()


                    findNavController().navigate(R.id.action_navigation_notifications_to_navigation_profile)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to add post!", Toast.LENGTH_SHORT).show()
                }

        }
    }
}
