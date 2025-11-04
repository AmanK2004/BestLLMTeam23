package com.example.csci310team23.data.model

import com.example.csci310team23.ui.UserSearchResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class UserSummary(
    val id: Long,
    val name: String,
    val email: String,
    val department: String,
    val school: String
)

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val studentId: String,
    val department: String,
    val school: String,
    val birthDate: LocalDate?,
    val bio: String,
    val isProfileComplete: Boolean = false
) {
    val summary: UserSummary
        get() = UserSummary(id, name, email, department, school)
}

data class VoteSummary(
    val upvotes: Int,
    val downvotes: Int
) {
    val score: Int
        get() = upvotes - downvotes
}

data class Comment(
    val id: Long,
    val postId: Long,
    val author: UserSummary,
    val title: String?,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val voteSummary: VoteSummary,
    val currentUserVote: Int?,
    val isEdited: Boolean = false
)

data class Post(
    val id: Long,
    val author: UserSummary,
    val title: String,
    val body: String,
    val tag: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val commentCount: Int,
    val comments: List<Comment>,
    val voteSummary: VoteSummary,
    val currentUserVote: Int?,
    val isPublished: Boolean = true,
    val isEdited: Boolean = false
)

data class Prompt(
    val id: Long,
    val author: UserSummary,
    val title: String,
    val description: String,
    val content: String,
    val tag: String,
    val temperature: String? = null,
    val context: String? = null,
    val memoryTokens: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPrivate: Boolean = false
)

enum class PostSearchType { TAG, AUTHOR, TITLE, FULL_TEXT }

data class SearchState(
    val postSearchType: PostSearchType? = null,
    val keyword: String = "",
    val postResults: List<Post> = emptyList(),
    val promptTag: String = "",
    val promptResults: List<Prompt> = emptyList(),
    val userEmail: String = "",
    val userResults: List<UserSearchResult> = emptyList(),
    val allUsers: List<UserProfile> = emptyList()
) {
    fun recompute(
        posts: List<Post>,
        prompts: List<Prompt>,
        users: List<UserProfile>,
        currentUserId: Long? = null
    ): SearchState {
        val refreshedPosts = if (postSearchType != null && keyword.isNotBlank()) {
            posts.filterBy(postSearchType, keyword)
        } else {
            emptyList()
        }

        val refreshedPrompts = if (promptTag.isNotBlank()) {
            prompts.filter {
                !it.isPrivate && it.tag.equals(promptTag, ignoreCase = true)
            }
        } else {
            emptyList()
        }

        val refreshedUsers = if (userEmail.isNotBlank()) {
            val matchingUsers = users.filter { user ->
                user.email.equals(userEmail.trim(), ignoreCase = true) ||
                        user.name.lowercase().contains(userEmail.trim().lowercase())
            }
            matchingUsers.map { user ->
                val userPrompts = if (user.id == currentUserId) {
                    prompts.filter { it.author.id == user.id }
                } else {
                    prompts.filter { it.author.id == user.id && !it.isPrivate }
                }
                UserSearchResult(user.summary, userPrompts)
            }
        } else {
            emptyList()
        }

        return copy(
            postResults = refreshedPosts,
            promptResults = refreshedPrompts,
            userResults = refreshedUsers,
            allUsers = users
        )
    }
}


fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

fun Long?.toLocalDate(): LocalDate? = this?.let { LocalDate.ofEpochDay(it) }

fun LocalDate?.toEpochDayOrNull(): Long? = this?.toEpochDay()

fun Instant.toEpochMillis(): Long = toEpochMilli()

fun Instant.formatRelative(): String {
    val now = Instant.now()
    val seconds = (now.epochSecond - epochSecond).coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        seconds < 86400 * 7 -> "${seconds / 86400}d ago"
        else -> java.time.ZonedDateTime.ofInstant(this, ZoneOffset.systemDefault()).toLocalDate()
            .toString()
    }
}

private fun List<Post>.filterBy(type: PostSearchType, keyword: String): List<Post> {
    if (keyword.isBlank()) return emptyList()
    val lower = keyword.lowercase()
    return when (type) {
        PostSearchType.TAG -> filter { it.tag.equals(keyword, ignoreCase = true) }
        PostSearchType.AUTHOR -> filter {
            it.author.name.lowercase().contains(lower) ||
                    it.author.email.lowercase().contains(lower)
        }

        PostSearchType.TITLE -> filter { it.title.lowercase().contains(lower) }
        PostSearchType.FULL_TEXT -> filter {
            it.title.lowercase().contains(lower) || it.body.lowercase().contains(lower)
        }
    }
}

data class TagWatchHistory(
    val tag: String,
    val startTime: Instant,
    val endTime: Instant? = null
)

data class UserWatchHistory(
    val email: String,
    val startTime: Instant,
    val endTime: Instant? = null
)
