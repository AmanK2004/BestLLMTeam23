package com.example.csci310team23.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        PromptEntity::class,
        PostVoteEntity::class,
        CommentVoteEntity::class,
        TagWatchHistoryEntity::class,
        UserWatchHistoryEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun promptDao(): PromptDao
    abstract fun postVoteDao(): PostVoteDao
    abstract fun commentVoteDao(): CommentVoteDao
    abstract fun tagWatchHistoryDao(): TagWatchHistoryDao
    abstract fun userWatchHistoryDao(): UserWatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}
