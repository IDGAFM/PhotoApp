@file:Suppress("DEPRECATION")

package com.example.instaapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.instaapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {


    lateinit var firebaseAuth: FirebaseAuth
    lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val sign_in: Button = findViewById(R.id.sign_in)
        val sign_up_txt: TextView = findViewById(R.id.sing_up_txt)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val forgot_password : TextView = findViewById(R.id.forgot_password)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)

        sign_up_txt.setOnClickListener(){

            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgot_password.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }


        sign_in.setOnClickListener(){

            val semail = email.text.toString()
            val spassword = password.text.toString()


            if(semail.isEmpty()){

                Toast.makeText(this, R.string.entere, Toast.LENGTH_SHORT).show()
            }

            else if (spassword.isEmpty()){

                Toast.makeText(this, R.string.enterp, Toast.LENGTH_SHORT).show()
            }

            else if (semail.isNotEmpty() && spassword.isNotEmpty()){

                signIn(spassword, semail)

            }

        }

    }

    private fun signIn(spassword: String, semail: String){

        progressDialog.show()
        progressDialog.setMessage(getString(R.string.sss))

        firebaseAuth.signInWithEmailAndPassword(semail, spassword).addOnCompleteListener(){

            if (it.isSuccessful){
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
            }

            else{
                progressDialog.dismiss()
                Toast.makeText(this, R.string.Incorrect, Toast.LENGTH_SHORT).show()
            }


        }.addOnFailureListener{ exception ->

            when(exception){

                is FirebaseAuthInvalidCredentialsException->{
                    Toast.makeText(this, R.string.Incorrect, Toast.LENGTH_SHORT).show()
                }

                else->{
                    Toast.makeText(applicationContext, R.string.af, Toast.LENGTH_SHORT).show()

                }
            }

        }
    }

 }
