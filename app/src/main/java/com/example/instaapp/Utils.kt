package com.example.instaapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Utils {

    companion object {

        private val auth = FirebaseAuth.getInstance()
        private var userid: String = ""

        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val PROFILE_IMAGE_CAPTURE = 3
        const val PROFILE_IMAGE_PICK = 4


        fun getUiLoggedIn(): String {

            if (auth.currentUser != null) {

                userid = auth.currentUser!!.uid

            }

            return userid

        }

        fun getTime(): Long {


            val unixTimestamp: Long = System.currentTimeMillis() / 1000


            return unixTimestamp

        }

        fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
            try {
                val filesDir = context.filesDir
                val imageFile = File(filesDir, "image.jpg")

                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                return Uri.fromFile(imageFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

    }
}