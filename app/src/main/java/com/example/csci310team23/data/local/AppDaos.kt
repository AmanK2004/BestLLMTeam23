package com.example.csci310team23.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<UserEntity>

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, passwordHash: String)
}

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity): Long

    @Update
    suspend fun update(post: PostEntity)

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getById(postId: Long): PostEntity?

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>
}

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity): Long

    @Update
    suspend fun update(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE id = :commentId LIMIT 1")
    suspend fun getById(commentId: Long): CommentEntity?

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeForPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments")
    fun observeAll(): Flow<List<CommentEntity>>
}

@Dao
interface PromptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: PromptEntity): Long

    @Update
    suspend fun update(prompt: PromptEntity)

    @Query("DELETE FROM prompts WHERE id = :promptId")
    suspend fun delete(promptId: Long)

    @Query("SELECT * FROM prompts WHERE id = :promptId LIMIT 1")
    suspend fun getById(promptId: Long): PromptEntity?

    @Query("SELECT * FROM prompts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PromptEntity>>
}

@Dao
interface PostVoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vote: PostVoteEntity): Long

    @Query("DELETE FROM post_votes WHERE postId = :postId AND userId = :userId")
    suspend fun delete(postId: Long, userId: Long)

    @Query("SELECT * FROM post_votes WHERE postId = :postId AND userId = :userId LIMIT 1")
    suspend fun getVote(postId: Long, userId: Long): PostVoteEntity?

    @Query("SELECT * FROM post_votes")
    fun observeAll(): Flow<List<PostVoteEntity>>
}

@Dao
interface CommentVoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vote: CommentVoteEntity): Long

    @Query("DELETE FROM comment_votes WHERE commentId = :commentId AND userId = :userId")
    suspend fun delete(commentId: Long, userId: Long)

    @Query("SELECT * FROM comment_votes WHERE commentId = :commentId AND userId = :userId LIMIT 1")
    suspend fun getVote(commentId: Long, userId: Long): CommentVoteEntity?

    @Query("SELECT * FROM comment_votes")
    fun observeAll(): Flow<List<CommentVoteEntity>>
}
