package com.example.instaapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.instaapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask

class RegisterActivity : AppCompatActivity() {


    lateinit var firebaseAuth: FirebaseAuth
    lateinit var progressDialog: ProgressDialog
    lateinit var fireStore : FirebaseFirestore
    private lateinit var emailVerificationTimer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        val sign_up: Button = findViewById(R.id.sign_up)
        val sign_in_txt: TextView = findViewById(R.id.sing_in_txt)
        val username : EditText = findViewById(R.id.username)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)


        firebaseAuth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)

        sign_in_txt.setOnClickListener(){
            startActivity(Intent(this, LoginActivity::class.java))
        }

        sign_up.setOnClickListener(){

            val name = username.text.toString()
            val semail = email.text.toString()
            val spassword = password.text.toString()

            if (name.isEmpty()){
                Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show()
            }


            else if (semail.isEmpty()){
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show()
            }


            else if (spassword.isEmpty()){
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
            }


            else if (name.isNotEmpty() && semail.isNotEmpty() && spassword.isNotEmpty()){

                signUp(name, semail, spassword)
            }

        }

    }

    private fun signUp(name: String, semail: String, spassword: String) {
        progressDialog.show()
        progressDialog.setMessage("Signing up")

        if (!isEmailValid(semail)) {
            progressDialog.dismiss()
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (spassword.length < 8 || !spassword.any()) {
            progressDialog.dismiss()
            Toast.makeText(this, "Password must be at least 8 characters long ", Toast.LENGTH_SHORT).show()
            return
        }

        fireStore.collection("Users").whereEqualTo("username", name).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result?.isEmpty == false) {
                    progressDialog.dismiss()
                    Toast.makeText(this, R.string.alredytaken, Toast.LENGTH_SHORT).show()
                } else {
                    firebaseAuth.createUserWithEmailAndPassword(semail, spassword).addOnCompleteListener { createUserTask ->
                        if (createUserTask.isSuccessful) {
                            val user = firebaseAuth.currentUser

                            user?.sendEmailVerification()?.addOnSuccessListener {
                                startEmailVerificationCheck(user, name, semail)
                                Toast.makeText(this, R.string.checkem, Toast.LENGTH_SHORT).show()
                            }?.addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Toast.makeText(this, "Failed to send email verification: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Something incorrect", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to check username availability: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startEmailVerificationCheck(user: FirebaseUser?, name: String, semail: String) {
        emailVerificationTimer = Timer()
        emailVerificationTimer.schedule(object : TimerTask() {
            override fun run() {
                user?.reload()
                if (user?.isEmailVerified == true) {
                    progressDialog.dismiss()
                    // Здесь проверяем подтверждение email
                    writeUserDataToFirestore(user, name, semail)
                    emailVerificationTimer.cancel()
                }
            }
        }, 0, 1000)
    }

    private fun writeUserDataToFirestore(user: FirebaseUser, name: String, semail: String) {
        val dataHashMap = hashMapOf(
            "userid" to user.uid,
            "username" to name,
            "useremail" to semail,
            "status" to "default",
            "followers" to 0,
            "following" to 0,
            "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/instaapp-8a2c5.appspot.com/o/avatars%2Fperson.png?alt=media&token=3f6a5649-546e-42bb-9277-cb61bcd637d0"
        )

        fireStore.collection("Users").document(user.uid).set(dataHashMap)
        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
    }

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }



    override fun onDestroy() {
        super.onDestroy()
        emailVerificationTimer.cancel()
    }

}