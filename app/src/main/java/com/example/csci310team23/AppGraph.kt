package com.example.csci310team23

import android.content.Context
import com.example.csci310team23.data.local.AppDatabase
import com.example.csci310team23.data.local.PreferencesManager
import com.example.csci310team23.data.repository.AppRepository
import com.example.csci310team23.data.repository.RoomAppRepository

object AppGraph {
    lateinit var repository: AppRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    fun provide(context: Context) {
        if (::repository.isInitialized) return

        val database = AppDatabase.get(context)
        repository = RoomAppRepository(
            userDao = database.userDao(),
            postDao = database.postDao(),
            commentDao = database.commentDao(),
            promptDao = database.promptDao(),
            postVoteDao = database.postVoteDao(),
            commentVoteDao = database.commentVoteDao(),
            tagWatchHistoryDao = database.tagWatchHistoryDao(), // Add this
            userWatchHistoryDao = database.userWatchHistoryDao(), // Add this
            postBookmarkDao = database.postBookmarkDao(),
            promptBookmarkDao = database.promptBookmarkDao(),
            postVersionDao = database.postVersionDao(),
            promptVersionDao = database.promptVersionDao()
        )

        preferencesManager = PreferencesManager(context.applicationContext)
    }
}
