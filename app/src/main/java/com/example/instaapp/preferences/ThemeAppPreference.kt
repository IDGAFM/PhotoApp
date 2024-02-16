package com.example.instaapp.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class ThemeAppPreference(context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun getThemeMode(): Boolean {
        val darkModeKey = "dark_mode"
        return sharedPreferences.getBoolean(darkModeKey, false)
    }

    fun setThemeMode(isDarkMode: Boolean) {
        val darkModeKey = "dark_mode"
        sharedPreferences.edit().putBoolean(darkModeKey, isDarkMode).apply()
    }

    fun isThemeApplied(): Boolean {
        val themeAppliedKey = "theme_applied"
        return sharedPreferences.getBoolean(themeAppliedKey, false)
    }

    fun setThemeApplied(applied: Boolean) {
        val themeAppliedKey = "theme_applied"
        sharedPreferences.edit().putBoolean(themeAppliedKey, applied).apply()
    }



}