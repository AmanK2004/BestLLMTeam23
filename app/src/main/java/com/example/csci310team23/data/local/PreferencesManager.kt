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

    fun setPostSearchType(typeOrdinal: Int) {
        prefs.edit().putInt("post_search_type", typeOrdinal).apply()
    }

    fun getPostSearchType(): Int {
        return prefs.getInt("post_search_type", 0)
    }

    fun setPostSearchKeyword(keyword: String) {
        prefs.edit().putString("post_search_keyword", keyword).apply()
    }

    fun getPostSearchKeyword(): String {
        return prefs.getString("post_search_keyword", "") ?: ""
    }

    fun setPromptSearchTag(tag: String) {
        prefs.edit().putString("prompt_search_tag", tag).apply()
    }

    fun getPromptSearchTag(): String {
        return prefs.getString("prompt_search_tag", "") ?: ""
    }

    fun setUserSearchQuery(query: String) {
        prefs.edit().putString("user_search_query", query).apply()
    }

    fun getUserSearchQuery(): String {
        return prefs.getString("user_search_query", "") ?: ""
    }

    fun setSearchTab(tab: Int) {
        prefs.edit().putInt("search_tab", tab).apply()
    }

    fun getSearchTab(): Int {
        return prefs.getInt("search_tab", 0)
    }

    companion object {
        private const val KEY_HAS_SEEN_LANDING = "has_seen_landing"
        private const val KEY_SHOW_COMMENT_TITLES = "show_comment_titles"
    }
}
