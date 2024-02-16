package com.example.instaapp.applecations

import android.app.Application
import com.google.firebase.FirebaseApp
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.example.instaapp.preferences.LanguageAppPreference
import com.example.instaapp.preferences.ThemeAppPreference
import java.util.Locale

@Suppress("DEPRECATION")
class MyApplication : Application() {

    private lateinit var languageAppPreference: LanguageAppPreference

    override fun onCreate() {
        super.onCreate()
        languageAppPreference = LanguageAppPreference(this)
        applyTheme()
        setAppLocale(getAppLanguage())
        FirebaseApp.initializeApp(this)
        applyLanguage()
    }


    private fun getAppLanguage(): String {
        val languageAppPreference = LanguageAppPreference(this)
        return languageAppPreference.getAppLanguage()
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    private fun applyLanguage() {
        val languageCode = getAppLanguage()
        LanguageAppPreference.selectedLanguage = languageCode
        setAppLocale(languageCode)
    }

    private fun applyTheme() {
        val themeAppPreference = ThemeAppPreference(this)
        val isDarkMode = themeAppPreference.getThemeMode()

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }


}
