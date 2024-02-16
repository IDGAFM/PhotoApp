package com.example.instaapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.example.instaapp.modal.User
import com.example.instaapp.preferences.LanguageAppPreference
import com.example.instaapp.preferences.ThemeAppPreference
import com.example.instaapp.preferences.UserProfilePreference
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale


@Suppress("DEPRECATION")
class SettingsActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var languageAppPreference: LanguageAppPreference
    }

    private lateinit var themeAppPreference: ThemeAppPreference
    private lateinit var darkModePreference: SwitchPreferenceCompat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        themeAppPreference = ThemeAppPreference(this)
        languageAppPreference = LanguageAppPreference(this)
        val languageCode = languageAppPreference.getAppLanguage()
        setAppLocale(languageCode)

        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)



        updateHomeAsUpIndicator()

        if (!themeAppPreference.isThemeApplied()) {
            applyThemePreference()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, SettingsFragment()).commit()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateHomeAsUpIndicator() {
        if (isDarkTheme()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_white_back24)
        } else {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_back24)
        }
    }

    private fun isDarkTheme(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyThemePreference() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        themeAppPreference.setThemeApplied(true)
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "dark_mode" -> {
                val isDarkMode = sharedPreferences?.getBoolean(key, false) ?: false
                AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            }
            "language_mode" -> {
                val languageCode = sharedPreferences?.getString(key, LanguageAppPreference.DEFAULT_LANGUAGE)
                LanguageAppPreference.selectedLanguage = languageCode ?: LanguageAppPreference.DEFAULT_LANGUAGE
                languageAppPreference.setAppLanguage(languageCode ?: LanguageAppPreference.DEFAULT_LANGUAGE)

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

        }

    }



    override fun onResume() {
        super.onResume()
        updateTexts()
        updateHomeAsUpIndicator()
    }

    private fun updateTexts() {
        supportActionBar?.title = getString(R.string.settings)
    }



    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }



    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var userProfilePreference: UserProfilePreference

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser
            val userInfoPreference = findPreference<PreferenceCategory>("user_info")

            // Создаем новый Preference для профиля пользователя
            userProfilePreference = UserProfilePreference(requireContext(), null)
            userInfoPreference?.addPreference(userProfilePreference)

            loadUserProfileData()


            val userEmailPreference = findPreference<Preference>("user_email")
            userEmailPreference?.summary = currentUser?.email ?: "user@example.com"

            val logoutPreference = findPreference<Preference>("logout")
            logoutPreference?.setOnPreferenceClickListener {
                Exit()
                true
            }

            val darkModePreference = findPreference<SwitchPreferenceCompat>("dark_mode")
            darkModePreference?.setOnPreferenceChangeListener { _, newValue ->
                val isDarkMode = newValue as Boolean
                AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                true
            }

            val deleteAccountPreference = findPreference<Preference>("delete_account")
            deleteAccountPreference?.setOnPreferenceClickListener {
                showDeleteAccountConfirmationDialog()
                true
            }





        }
        private fun loadUserProfileData() {
            val firestore = FirebaseFirestore.getInstance()
            val firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser
            val userId = currentUser?.uid

            userId?.let {
                firestore.collection("Users").document(it).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val userName = document.getString("username")
                            val userAvatarUrl = document.getString("imageUrl")

                            // Заполняем данные о профиле пользователя
                            userProfilePreference.setUserName(userName)
                            userProfilePreference.setUserAvatarUrl(userAvatarUrl)
                        }
                    }
            }
        }


        private fun showDeleteAccountConfirmationDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                .setPositiveButton("Да") { _, _ ->
                    deleteUserAccount()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }



        private fun deleteUserAccount() {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            val userEmail = firebaseUser?.email ?: return
            var firestore: FirebaseFirestore


            val dialog = AlertDialog.Builder(requireContext())
            dialog.setTitle("Confirm Delete Account")
            dialog.setMessage("Please enter your password to confirm deletion.")

            firestore = FirebaseFirestore.getInstance()

            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            dialog.setView(input)

            dialog.setPositiveButton("Confirm") { _, _ ->
                val password = input.text.toString()
                val credential = EmailAuthProvider.getCredential(userEmail, password)

                firebaseUser.reauthenticate(credential).addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {

                        firebaseUser.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(requireContext(), getString(R.string.delacc), Toast.LENGTH_SHORT).show()
                                val intent = Intent(requireContext(), StartActivity::class.java)
                                startActivity(intent)
                            } else {
                                Log.e("Error", "Failed to delete account: ${deleteTask.exception}")
                                Toast.makeText(requireContext(), "Failed to delete account. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        firestore = FirebaseFirestore.getInstance()
                        val postsCollection = firestore.collection("Posts")
                        postsCollection.whereEqualTo("userid", firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val deleteTasks = mutableListOf<Task<Void>>()
                                for (document in querySnapshot.documents) {
                                    val deleteTask = postsCollection.document(document.id).delete()
                                    deleteTasks.add(deleteTask)
                                }
                                val userDocRef = firestore.collection("Users").document(firebaseUser.uid)
                                userDocRef.delete()


                                val currentUserUid = Utils.getUiLoggedIn()

                                firestore.collection("Follower").whereArrayContains("following_id", currentUserUid).get()
                                    .addOnSuccessListener { followerQuerySnapshot ->
                                        val userBatch = firestore.batch()

                                        followerQuerySnapshot.documents.forEach { followerDocument ->
                                            val followerUserId = followerDocument.id
                                            val userDocReff = firestore.collection("Users").document(followerUserId)
                                            userBatch.update(userDocReff, "followers", FieldValue.increment(-1))
                                        }

                                        userBatch.commit().addOnCompleteListener { followerUpdateTask ->
                                            if (followerUpdateTask.isSuccessful) {
                                                firestore.collection("Follower").document(currentUserUid).get().addOnSuccessListener { currentUserSnapshot ->
                                                    val followingIds = currentUserSnapshot.get("following_id") as? List<String> ?: emptyList()

                                                    firestore.collection("Users").whereIn("userid", followingIds).get().addOnSuccessListener { followingQuerySnapshot ->
                                                        val followingBatch = firestore.batch()

                                                        followingQuerySnapshot.documents.forEach { followingDocument ->
                                                            val followingUserId =
                                                                followingDocument.id
                                                            val userDocReff =
                                                                firestore.collection("Users")
                                                                    .document(followingUserId)
                                                            followingBatch.update(
                                                                userDocReff,
                                                                "following",
                                                                FieldValue.increment(-1)
                                                            )
                                                        }

                                                        followingBatch.commit()
                                                    }
                                                }
                                            } else {
                                                Log.e("DeleteUserAccount", "Failed to update follower count: ${followerUpdateTask.exception}")
                                            }
                                        }
                                    }



                                val followDocRef = firestore.collection("Follower").document(firebaseUser.uid)
                                followDocRef.delete().addOnCompleteListener { deleteTask ->
                                    if (deleteTask.isSuccessful) {
                                        Log.d("DeleteUserAccount", "User data deleted from 'Follower' collection")
                                    } else {
                                        Log.e("DeleteUserAccount", "Failed to delete user data from 'Follower' collection: ${deleteTask.exception}")
                                    }

                                }

                            }

                    } else {
                        Log.e("Error", "Reauthentication failed: ${authResult.exception}")
                        Toast.makeText(requireContext(), "Reauthentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.setNegativeButton("Cancel") { it, _ ->
                it.cancel()
            }

            dialog.show()
        }


        private fun Exit() {
            firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }



}
