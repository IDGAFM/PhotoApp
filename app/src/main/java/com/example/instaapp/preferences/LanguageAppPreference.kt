package com.example.instaapp.preferences


import android.content.Context
import android.content.SharedPreferences

import android.content.res.Configuration
import java.util.*

class LanguageAppPreference(private val context: Context) {
    companion object {
        private const val LANGUAGE_KEY = "language_mode"
        const val DEFAULT_LANGUAGE = "en"

        var selectedLanguage: String = DEFAULT_LANGUAGE
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getAppLanguage(): String {
        val defaultLanguageKey = "$LANGUAGE_KEY$DEFAULT_LANGUAGE"
        return sharedPreferences.getString(defaultLanguageKey, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setAppLanguage(languageCode: String) {
        selectedLanguage = languageCode
        val languageKey = "$LANGUAGE_KEY$languageCode"
        sharedPreferences.edit().putString(languageKey, languageCode).apply()
        updateAppLanguage(languageCode)
    }


    private fun updateAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
