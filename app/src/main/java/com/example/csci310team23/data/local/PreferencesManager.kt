package com.example.csci310team23.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    var hasSeenLanding: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEN_LANDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SEEN_LANDING, value).apply()

    fun setShowCommentTitles(userId: Long, show: Boolean) {
        prefs.edit().putBoolean("${KEY_SHOW_COMMENT_TITLES}_$userId", show).apply()
    }

    fun getShowCommentTitles(userId: Long): Boolean {
        return prefs.getBoolean("${KEY_SHOW_COMMENT_TITLES}_$userId", true) // Default to true
    }

    companion object {
        private const val KEY_HAS_SEEN_LANDING = "has_seen_landing"
        private const val KEY_SHOW_COMMENT_TITLES = "show_comment_titles"
    }
}
