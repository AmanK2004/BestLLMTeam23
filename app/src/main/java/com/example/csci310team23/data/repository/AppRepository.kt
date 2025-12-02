package com.example.csci310team23.data.repository

import com.example.csci310team23.data.local.CommentDao
import com.example.csci310team23.data.local.CommentEntity
import com.example.csci310team23.data.local.CommentVoteDao
import com.example.csci310team23.data.local.CommentVoteEntity
import com.example.csci310team23.data.local.PostBookmarkDao
import com.example.csci310team23.data.local.PostBookmarkEntity
import com.example.csci310team23.data.local.PostDao
import com.example.csci310team23.data.local.PostEntity
import com.example.csci310team23.data.local.PostVersionDao
import com.example.csci310team23.data.local.PostVersionEntity
import com.example.csci310team23.data.local.PostVoteDao
import com.example.csci310team23.data.local.PostVoteEntity
import com.example.csci310team23.data.local.PromptBookmarkDao
import com.example.csci310team23.data.local.PromptBookmarkEntity
import com.example.csci310team23.data.local.PromptDao
import com.example.csci310team23.data.local.PromptEntity
import com.example.csci310team23.data.local.PromptVersionDao
import com.example.csci310team23.data.local.PromptVersionEntity
import com.example.csci310team23.data.local.TagWatchHistoryDao
import com.example.csci310team23.data.local.TagWatchHistoryEntity
import com.example.csci310team23.data.local.UserDao
import com.example.csci310team23.data.local.UserEntity
import com.example.csci310team23.data.local.UserWatchHistoryDao
import com.example.csci310team23.data.local.UserWatchHistoryEntity
import com.example.csci310team23.data.model.TagWatchHistory
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserWatchHistory
import com.example.csci310team23.data.model.toEpochDayOrNull
import com.example.csci310team23.data.model.toProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.Period


data class AppDataSnapshot(
    val users: List<UserEntity>,
    val posts: List<PostEntity>,
    val comments: List<CommentEntity>,
    val prompts: List<PromptEntity>,
    val postVotes: List<PostVoteEntity>,
    val commentVotes: List<CommentVoteEntity>,
    val postBookmarks: List<PostBookmarkEntity>,
    val promptBookmarks: List<PromptBookmarkEntity>,
    val postVersions: List<PostVersionEntity>,
    val promptVersions: List<PromptVersionEntity>
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

    suspend fun getUserByEmail(email: String): UserProfile?

    suspend fun createPost(
        authorId: Long,
        title: String,
        body: String,
        tag: String,
        isAnonymous: Boolean = false,
        isPublished: Boolean = true
    ): Long

    suspend fun updatePost(
        postId: Long,
        title: String,
        body: String,
        tag: String,
        isAnonymous: Boolean
    )

    suspend fun deletePost(postId: Long)

    suspend fun publishDraft(postId: Long)

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
        tag: String,
        temperature: String?,
        context: String?,
        memoryTokens: String?,
        isPrivate: Boolean = false
    ): Long

    suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        content: String,
        tag: String,
        temperature: String?,
        context: String?,
        memoryTokens: String?,
        isPrivate: Boolean = false
    )

    suspend fun deletePrompt(promptId: Long)

    suspend fun togglePostBookmark(userId: Long, postId: Long): Boolean
    suspend fun togglePromptBookmark(userId: Long, promptId: Long): Boolean

    suspend fun saveTagWatchHistory(
        userId: Long,
        tag: String,
        startTime: Instant,
        endTime: Instant? = null
    )

    suspend fun saveUserWatchHistory(
        userId: Long,
        watchedUserEmail: String,
        startTime: Instant,
        endTime: Instant? = null
    )

    suspend fun getTagWatchHistory(userId: Long): List<TagWatchHistory>
    suspend fun getUserWatchHistory(userId: Long): List<UserWatchHistory>
}

class RoomAppRepository(
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val promptDao: PromptDao,
    private val postVoteDao: PostVoteDao,
    private val commentVoteDao: CommentVoteDao,
    private val tagWatchHistoryDao: TagWatchHistoryDao,
    private val userWatchHistoryDao: UserWatchHistoryDao,
    private val postBookmarkDao: PostBookmarkDao,
    private val promptBookmarkDao: PromptBookmarkDao,
    private val postVersionDao: PostVersionDao,
    private val promptVersionDao: PromptVersionDao
) : AppRepository {
    companion object {
        const val MAX_BIO_LENGTH = 500
        const val MIN_AGE_YEARS = 18
    }

    override suspend fun registerUser(
        name: String,
        email: String,
        studentId: String,
        password: String
    ): UserProfile {
        if (name.isBlank() || email.isBlank() || studentId.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("All fields are required")
        }

        val normalizedEmail = email.lowercase()

        if (!normalizedEmail.endsWith("@usc.edu")) {
            throw IllegalArgumentException("Please use a valid USC email address (@usc.edu)")
        }

        if (!studentId.matches(Regex("^\\d{10}\$"))) {
            throw IllegalArgumentException("Student ID must be exactly 10 digits")
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
            bio = "",
            isProfileComplete = false
        )

        val id = userDao.insert(entity)
        return userDao.getById(id)?.toProfile()
            ?: error("User not found after registration")
    }

    override suspend fun getUserByEmail(email: String): UserProfile? {
        val normalizedEmail = email.lowercase()
        val entity = userDao.getByEmail(normalizedEmail)
        return entity?.toProfile()
    }

    override suspend fun completeProfile(
        userId: Long,
        department: String,
        school: String,
        birthDate: LocalDate?,
        bio: String
    ): UserProfile {
        val existing = userDao.getById(userId)
            ?: throw IllegalArgumentException("User not found")

        if (department.isBlank() || school.isBlank()) {
            throw IllegalArgumentException("Please complete all required fields")
        }

        if (bio.length > MAX_BIO_LENGTH) {
            throw IllegalArgumentException("Bio must not exceed $MAX_BIO_LENGTH characters")
        }

        if (birthDate != null) {
            val age = Period.between(birthDate, LocalDate.now()).years
            if (age < MIN_AGE_YEARS) {
                throw IllegalArgumentException("You must be 18 or older")
            }
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
            bio = bio.trim(),
            isProfileComplete = true
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

    override suspend fun updateProfile(
        userId: Long,
        birthDate: LocalDate?,
        bio: String
    ): UserProfile {
        val existing = userDao.getById(userId)
            ?: throw IllegalArgumentException("User not found")

        if (bio.length > MAX_BIO_LENGTH) {
            throw IllegalArgumentException("Bio must not exceed $MAX_BIO_LENGTH characters")
        }

        if (birthDate != null) {
            val age = Period.between(birthDate, LocalDate.now()).years
            if (age < MIN_AGE_YEARS) {
                throw IllegalArgumentException("You must be 18 or older")
            }
        }

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
        val postBookmarksFlow = postBookmarkDao.observeAll()
        val promptBookmarksFlow = promptBookmarkDao.observeAll()
        val postVersionsFlow = postVersionDao.observeAll()
        val promptVersionsFlow = promptVersionDao.observeAll()

        val baseSnapshot = combine(
            usersFlow,
            postsFlow,
            commentsFlow,
            promptsFlow,
            postVotesFlow
        ) { users, posts, comments, prompts, postVotes ->
            AppDataSnapshot(
                users = users,
                posts = posts,
                comments = comments,
                prompts = prompts,
                postVotes = postVotes,
                commentVotes = emptyList(),
                postBookmarks = emptyList(),
                promptBookmarks = emptyList(),
                postVersions = emptyList(),
                promptVersions = emptyList()
            )
        }

        val withCommentVotes = combine(baseSnapshot, commentVotesFlow) { snapshot, commentVotes ->
            snapshot.copy(commentVotes = commentVotes)
        }

        val withPostBookmarks = combine(withCommentVotes, postBookmarksFlow) { snapshot, postBookmarks ->
            snapshot.copy(postBookmarks = postBookmarks)
        }

        val withPromptBookmarks = combine(withPostBookmarks, promptBookmarksFlow) { snapshot, promptBookmarks ->
            snapshot.copy(promptBookmarks = promptBookmarks)
        }

        val withPostVersions = combine(withPromptBookmarks, postVersionsFlow) { snapshot, postVersions ->
            snapshot.copy(postVersions = postVersions)
        }

        return combine(withPostVersions, promptVersionsFlow) { snapshot, promptVersions ->
            snapshot.copy(promptVersions = promptVersions)
        }
    }

    override suspend fun createPost(
        authorId: Long,
        title: String,
        body: String,
        tag: String,
        isAnonymous: Boolean,
        isPublished: Boolean
    ): Long {
        if (title.isBlank() || body.isBlank()) {
            throw IllegalArgumentException("Title and body are required")
        }

        if (tag.isBlank()) {
            throw IllegalArgumentException("Please select an LLM tag")
        }

        val now = System.currentTimeMillis()
        val entity = PostEntity(
            authorId = authorId,
            title = title.trim(),
            body = body.trim(),
            tag = tag.trim(),
            createdAt = now,
            updatedAt = now,
            isAnonymous = isAnonymous,
            isPublished = isPublished,
            isEdited = false
        )
        return postDao.insert(entity)
    }

    override suspend fun updatePost(
        postId: Long,
        title: String,
        body: String,
        tag: String,
        isAnonymous: Boolean
    ) {
        val existing = postDao.getById(postId)
            ?: throw IllegalArgumentException("Post not found")

        if (title.isBlank() || body.isBlank()) {
            throw IllegalArgumentException("Title and body are required")
        }

        if (tag.isBlank()) {
            throw IllegalArgumentException("Please select an LLM tag")
        }

        val now = System.currentTimeMillis()
        postVersionDao.insert(
            PostVersionEntity(
                postId = existing.id,
                title = existing.title,
                body = existing.body,
                tag = existing.tag,
                createdAt = now
            )
        )

        val updated = existing.copy(
            title = title.trim(),
            body = body.trim(),
            tag = tag.trim(),
            isAnonymous = isAnonymous,
            updatedAt = now,
            isEdited = true
        )
        postDao.update(updated)
    }

    override suspend fun deletePost(postId: Long) {
        postDao.delete(postId)
    }

    override suspend fun publishDraft(postId: Long) {
        val existing = postDao.getById(postId)
            ?: throw IllegalArgumentException("Post not found")

        val updated = existing.copy(
            isPublished = true,
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
        if (body.isBlank()) {
            throw IllegalArgumentException("Comment cannot be empty")
        }

        val now = System.currentTimeMillis()
        val entity = CommentEntity(
            postId = postId,
            authorId = authorId,
            title = title?.takeIf { it.isNotBlank() }?.trim(),
            body = body.trim(),
            createdAt = now,
            updatedAt = now,
            isEdited = false
        )
        return commentDao.insert(entity)
    }

    override suspend fun updateComment(
        commentId: Long,
        title: String?,
        body: String
    ) {
        val existing = commentDao.getById(commentId)
            ?: throw IllegalArgumentException("Comment not found")

        if (body.isBlank()) {
            throw IllegalArgumentException("Comment cannot be empty")
        }

        val updated = existing.copy(
            title = title?.takeIf { it.isNotBlank() }?.trim(),
            body = body.trim(),
            updatedAt = System.currentTimeMillis(),
            isEdited = true
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
        tag: String,
        temperature: String?,
        context: String?,
        memoryTokens: String?,
        isPrivate: Boolean
    ): Long {
        if (title.isBlank() || content.isBlank() || tag.isBlank()) {
            throw IllegalArgumentException("Title, content, and AI model are required")
        }

        val now = System.currentTimeMillis()
        val entity = PromptEntity(
            authorId = authorId,
            title = title.trim(),
            description = description.trim(),
            content = content.trim(),
            tag = tag.trim(),
            temperature = temperature?.trim()?.takeIf { it.isNotBlank() },
            context = context?.trim()?.takeIf { it.isNotBlank() },
            memoryTokens = memoryTokens?.trim()?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
            isPrivate = isPrivate
        )
        return promptDao.insert(entity)
    }

    override suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        content: String,
        tag: String,
        temperature: String?,
        context: String?,
        memoryTokens: String?,
        isPrivate: Boolean
    ) {
        val existing = promptDao.getById(promptId)
            ?: throw IllegalArgumentException("Prompt not found")

        if (title.isBlank() || content.isBlank() || tag.isBlank()) {
            throw IllegalArgumentException("Title, content, and AI model are required")
        }

        val now = System.currentTimeMillis()
        promptVersionDao.insert(
            PromptVersionEntity(
                promptId = existing.id,
                title = existing.title,
                description = existing.description,
                content = existing.content,
                tag = existing.tag,
                temperature = existing.temperature,
                context = existing.context,
                memoryTokens = existing.memoryTokens,
                createdAt = now
            )
        )

        val updated = existing.copy(
            title = title.trim(),
            description = description.trim(),
            content = content.trim(),
            tag = tag.trim(),
            temperature = temperature?.trim()?.takeIf { it.isNotBlank() },
            context = context?.trim()?.takeIf { it.isNotBlank() },
            memoryTokens = memoryTokens?.trim()?.takeIf { it.isNotBlank() },
            updatedAt = now,
            isPrivate = isPrivate,
            isEdited = true
        )
        promptDao.update(updated)
    }

    override suspend fun deletePrompt(promptId: Long) {
        promptDao.delete(promptId)
    }

    override suspend fun togglePostBookmark(userId: Long, postId: Long): Boolean {
        val result = postBookmarkDao.insert(
            PostBookmarkEntity(
                userId = userId,
                postId = postId,
                createdAt = System.currentTimeMillis()
            )
        )
        return if (result == -1L) {
            postBookmarkDao.delete(userId, postId)
            false
        } else {
            true
        }
    }

    override suspend fun togglePromptBookmark(userId: Long, promptId: Long): Boolean {
        val result = promptBookmarkDao.insert(
            PromptBookmarkEntity(
                userId = userId,
                promptId = promptId,
                createdAt = System.currentTimeMillis()
            )
        )
        return if (result == -1L) {
            promptBookmarkDao.delete(userId, promptId)
            false
        } else {
            true
        }
    }

    override suspend fun saveTagWatchHistory(
        userId: Long,
        tag: String,
        startTime: Instant,
        endTime: Instant?
    ) {
        val entity = TagWatchHistoryEntity(
            userId = userId,
            tag = tag,
            startTime = startTime.toEpochMilli(),
            endTime = endTime?.toEpochMilli()
        )

        val existing = tagWatchHistoryDao.getByUserId(userId).find { it.tag == tag }
        if (existing != null) {
            tagWatchHistoryDao.update(entity.copy(id = existing.id))
        } else {
            tagWatchHistoryDao.insert(entity)
        }
    }

    override suspend fun saveUserWatchHistory(
        userId: Long,
        watchedUserEmail: String,
        startTime: Instant,
        endTime: Instant?
    ) {
        val entity = UserWatchHistoryEntity(
            userId = userId,
            watchedUserEmail = watchedUserEmail,
            startTime = startTime.toEpochMilli(),
            endTime = endTime?.toEpochMilli()
        )

        val existing =
            userWatchHistoryDao.getByUserId(userId).find { it.watchedUserEmail == watchedUserEmail }
        if (existing != null) {
            userWatchHistoryDao.update(entity.copy(id = existing.id))
        } else {
            userWatchHistoryDao.insert(entity)
        }
    }

    override suspend fun getTagWatchHistory(userId: Long): List<TagWatchHistory> {
        return tagWatchHistoryDao.getByUserId(userId).map { entity ->
            TagWatchHistory(
                tag = entity.tag,
                startTime = Instant.ofEpochMilli(entity.startTime),
                endTime = entity.endTime?.let { Instant.ofEpochMilli(it) }
            )
        }
    }

    override suspend fun getUserWatchHistory(userId: Long): List<UserWatchHistory> {
        return userWatchHistoryDao.getByUserId(userId).map { entity ->
            UserWatchHistory(
                email = entity.watchedUserEmail,
                startTime = Instant.ofEpochMilli(entity.startTime),
                endTime = entity.endTime?.let { Instant.ofEpochMilli(it) }
            )
        }
    }
}

private fun String.toHash(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
