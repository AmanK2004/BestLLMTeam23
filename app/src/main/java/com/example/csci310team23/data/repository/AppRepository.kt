package com.example.csci310team23.data.repository

import com.example.csci310team23.data.local.CommentDao
import com.example.csci310team23.data.local.CommentEntity
import com.example.csci310team23.data.local.CommentVoteDao
import com.example.csci310team23.data.local.CommentVoteEntity
import com.example.csci310team23.data.local.PostDao
import com.example.csci310team23.data.local.PostEntity
import com.example.csci310team23.data.local.PostVoteDao
import com.example.csci310team23.data.local.PostVoteEntity
import com.example.csci310team23.data.local.PromptDao
import com.example.csci310team23.data.local.PromptEntity
import com.example.csci310team23.data.local.UserDao
import com.example.csci310team23.data.local.UserEntity
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.toProfile
import com.example.csci310team23.data.model.toEpochDayOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.LocalDate

data class AppDataSnapshot(
    val users: List<UserEntity>,
    val posts: List<PostEntity>,
    val comments: List<CommentEntity>,
    val prompts: List<PromptEntity>,
    val postVotes: List<PostVoteEntity>,
    val commentVotes: List<CommentVoteEntity>
)

private data class PartialSnapshot(
    val users: List<UserEntity>,
    val posts: List<PostEntity>,
    val comments: List<CommentEntity>,
    val prompts: List<PromptEntity>,
    val postVotes: List<PostVoteEntity>
)

interface AppRepository {
    suspend fun registerUser(
        name: String,
        email: String,
        studentId: String,
        password: String
    ): UserProfile

    suspend fun authenticate(email: String, password: String): UserProfile?

    suspend fun completeProfile(
        userId: Long,
        department: String,
        school: String,
        birthDate: LocalDate?,
        bio: String
    ): UserProfile

    suspend fun updateProfile(
        userId: Long,
        birthDate: LocalDate?,
        bio: String
    ): UserProfile

    suspend fun resetPassword(userId: Long, newPassword: String)

    fun observeUser(userId: Long): Flow<UserProfile?>

    fun observeData(): Flow<AppDataSnapshot>

    suspend fun createPost(
        authorId: Long,
        title: String,
        body: String,
        tag: String
    ): Long

    suspend fun updatePost(
        postId: Long,
        title: String,
        body: String,
        tag: String
    )

    suspend fun createComment(
        postId: Long,
        authorId: Long,
        title: String?,
        body: String
    ): Long

    suspend fun updateComment(
        commentId: Long,
        title: String?,
        body: String
    )

    suspend fun voteOnPost(
        postId: Long,
        userId: Long,
        value: Int
    )

    suspend fun voteOnComment(
        commentId: Long,
        userId: Long,
        value: Int
    )

    suspend fun createPrompt(
        authorId: Long,
        title: String,
        description: String,
        content: String,
        tag: String
    ): Long

    suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        content: String,
        tag: String
    )

    suspend fun deletePrompt(promptId: Long)
}

class RoomAppRepository(
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val promptDao: PromptDao,
    private val postVoteDao: PostVoteDao,
    private val commentVoteDao: CommentVoteDao
) : AppRepository {

    override suspend fun registerUser(
        name: String,
        email: String,
        studentId: String,
        password: String
    ): UserProfile {
        val normalizedEmail = email.lowercase()
        if (!normalizedEmail.endsWith("@usc.edu")) {
            throw IllegalArgumentException("Email must end with @usc.edu")
        }
        if (!studentId.matches(Regex("^\\d{10}\$"))) {
            throw IllegalArgumentException("Student ID must be a 10-digit number")
        }
        val existing = userDao.getByEmail(normalizedEmail)
        if (existing != null) {
            throw IllegalStateException("An account already exists for $normalizedEmail")
        }

        val entity = UserEntity(
            name = name.trim(),
            email = normalizedEmail,
            studentId = studentId,
            passwordHash = password.toHash(),
            department = "",
            school = "",
            birthDateEpochDay = null,
            bio = ""
        )

        val id = userDao.insert(entity)
        return userDao.getById(id)?.toProfile()
            ?: error("User not found after registration")
    }

    override suspend fun completeProfile(
        userId: Long,
        department: String,
        school: String,
        birthDate: LocalDate?,
        bio: String
    ): UserProfile {
        val existing = userDao.getById(userId) ?: throw IllegalArgumentException("User not found")
        if (department.isBlank() || school.isBlank()) {
            throw IllegalArgumentException("Affiliation fields cannot be blank")
        }
        if (existing.department.isNotBlank() || existing.school.isNotBlank()) {
            if (existing.department != department.trim() || existing.school != school.trim()) {
                throw IllegalStateException("Affiliation cannot be changed after profile creation")
            }
        }

        val updated = existing.copy(
            department = department.trim(),
            school = school.trim(),
            birthDateEpochDay = birthDate.toEpochDayOrNull(),
            bio = bio.trim()
        )

        userDao.update(updated)
        return updated.toProfile()
    }

    override suspend fun authenticate(email: String, password: String): UserProfile? {
        val normalizedEmail = email.lowercase()
        val entity = userDao.getByEmail(normalizedEmail) ?: return null
        return if (entity.passwordHash == password.toHash()) {
            entity.toProfile()
        } else {
            null
        }
    }

    override suspend fun updateProfile(userId: Long, birthDate: LocalDate?, bio: String): UserProfile {
        val existing = userDao.getById(userId) ?: throw IllegalArgumentException("User not found")
        val updated = existing.copy(
            birthDateEpochDay = birthDate.toEpochDayOrNull(),
            bio = bio.trim()
        )
        userDao.update(updated)
        return updated.toProfile()
    }

    override suspend fun resetPassword(userId: Long, newPassword: String) {
        userDao.updatePassword(userId, newPassword.toHash())
    }

    override fun observeUser(userId: Long): Flow<UserProfile?> =
        userDao.observeById(userId).map { it?.toProfile() }

    override fun observeData(): Flow<AppDataSnapshot> {
        val usersFlow = userDao.observeAll()
        val postsFlow = postDao.observeAll()
        val commentsFlow = commentDao.observeAll()
        val promptsFlow = promptDao.observeAll()
        val postVotesFlow = postVoteDao.observeAll()
        val commentVotesFlow = commentVoteDao.observeAll()

        val partialFlow = combine(
            usersFlow,
            postsFlow,
            commentsFlow,
            promptsFlow,
            postVotesFlow
        ) { users, posts, comments, prompts, postVotes ->
            PartialSnapshot(
                users = users,
                posts = posts,
                comments = comments,
                prompts = prompts,
                postVotes = postVotes
            )
        }

        return combine(partialFlow, commentVotesFlow) { partial, commentVotes ->
            AppDataSnapshot(
                users = partial.users,
                posts = partial.posts,
                comments = partial.comments,
                prompts = partial.prompts,
                postVotes = partial.postVotes,
                commentVotes = commentVotes
            )
        }
    }

    override suspend fun createPost(authorId: Long, title: String, body: String, tag: String): Long {
        val now = System.currentTimeMillis()
        val entity = PostEntity(
            authorId = authorId,
            title = title.trim(),
            body = body.trim(),
            tag = tag.trim(),
            createdAt = now,
            updatedAt = now
        )
        return postDao.insert(entity)
    }

    override suspend fun updatePost(postId: Long, title: String, body: String, tag: String) {
        val existing = postDao.getById(postId) ?: throw IllegalArgumentException("Post not found")
        val updated = existing.copy(
            title = title.trim(),
            body = body.trim(),
            tag = tag.trim(),
            updatedAt = System.currentTimeMillis()
        )
        postDao.update(updated)
    }

    override suspend fun createComment(
        postId: Long,
        authorId: Long,
        title: String?,
        body: String
    ): Long {
        val now = System.currentTimeMillis()
        val entity = CommentEntity(
            postId = postId,
            authorId = authorId,
            title = title?.takeIf { it.isNotBlank() }?.trim(),
            body = body.trim(),
            createdAt = now,
            updatedAt = now
        )
        return commentDao.insert(entity)
    }

    override suspend fun updateComment(commentId: Long, title: String?, body: String) {
        val existing = commentDao.getById(commentId) ?: throw IllegalArgumentException("Comment not found")
        val updated = existing.copy(
            title = title?.takeIf { it.isNotBlank() }?.trim(),
            body = body.trim(),
            updatedAt = System.currentTimeMillis()
        )
        commentDao.update(updated)
    }

    override suspend fun voteOnPost(postId: Long, userId: Long, value: Int) {
        when (value) {
            0 -> postVoteDao.delete(postId, userId)
            -1, 1 -> postVoteDao.insert(
                PostVoteEntity(
                    postId = postId,
                    userId = userId,
                    value = value
                )
            )
            else -> throw IllegalArgumentException("Vote must be -1, 0, or 1")
        }
    }

    override suspend fun voteOnComment(commentId: Long, userId: Long, value: Int) {
        when (value) {
            0 -> commentVoteDao.delete(commentId, userId)
            -1, 1 -> commentVoteDao.insert(
                CommentVoteEntity(
                    commentId = commentId,
                    userId = userId,
                    value = value
                )
            )
            else -> throw IllegalArgumentException("Vote must be -1, 0, or 1")
        }
    }

    override suspend fun createPrompt(
        authorId: Long,
        title: String,
        description: String,
        content: String,
        tag: String
    ): Long {
        val now = System.currentTimeMillis()
        val entity = PromptEntity(
            authorId = authorId,
            title = title.trim(),
            description = description.trim(),
            content = content.trim(),
            tag = tag.trim(),
            createdAt = now,
            updatedAt = now
        )
        return promptDao.insert(entity)
    }

    override suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        content: String,
        tag: String
    ) {
        val existing = promptDao.getById(promptId) ?: throw IllegalArgumentException("Prompt not found")
        val updated = existing.copy(
            title = title.trim(),
            description = description.trim(),
            content = content.trim(),
            tag = tag.trim(),
            updatedAt = System.currentTimeMillis()
        )
        promptDao.update(updated)
    }

    override suspend fun deletePrompt(promptId: Long) {
        promptDao.delete(promptId)
    }
}

private fun String.toHash(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
