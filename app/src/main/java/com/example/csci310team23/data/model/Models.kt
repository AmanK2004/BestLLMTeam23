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
    val isAnonymous: Boolean = false,
    val title: String,
    val body: String,
    val tag: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val commentCount: Int,
    val comments: List<Comment>,
    val voteSummary: VoteSummary,
    val currentUserVote: Int?,
    val isBookmarked: Boolean = false,
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
    val isPrivate: Boolean = false,
    val isBookmarked: Boolean = false,
    val isEdited: Boolean = false
)

enum class PostSearchType { TAG, AUTHOR, TITLE, FULL_TEXT }

data class SearchState(
    val postSearchType: PostSearchType? = null,
    val keyword: String = "",
    val postResults: List<Post> = emptyList(),
    val promptTag: String = "",
    val promptResults: List<Prompt> = emptyList(),
    val userQuery: String = "",
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
            posts.filterBy(postSearchType, keyword, currentUserId)
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

        val refreshedUsers = if (userQuery.isNotBlank()) {
            val tokens = tokenizeForSearch(userQuery)
            val matchingUsers = users.filter { user ->
                if (tokens.isEmpty()) return@filter false
                val nameTokens = tokenizeForSearch(user.name)
                tokens.all { token ->
                    isFuzzyMatch(token, user.email.lowercase()) ||
                            nameTokens.any { part -> isFuzzyMatch(token, part) }
                }
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

private fun List<Post>.filterBy(
    type: PostSearchType,
    keyword: String,
    currentUserId: Long?
): List<Post> {
    if (keyword.isBlank()) return emptyList()
    val lower = keyword.lowercase()
    val queryTokens = tokenizeForSearch(keyword)
    return when (type) {
        PostSearchType.TAG -> filter { it.tag.equals(keyword, ignoreCase = true) }
        PostSearchType.AUTHOR -> filter {
            if (it.isAnonymous && it.author.id != currentUserId) return@filter false
            val authorTokens = tokenizeForSearch("${it.author.name} ${it.author.email}")
            queryTokens.any { token -> authorTokens.any { authorPart -> isFuzzyMatch(token, authorPart) } }
        }

        PostSearchType.TITLE -> filter { it.title.lowercase().contains(lower) }
        PostSearchType.FULL_TEXT -> filter {
            if (queryTokens.isEmpty()) return@filter false
            val contentTokens = tokenizeForSearch("${it.title} ${it.body}")
            queryTokens.all { token -> contentTokens.any { contentPart -> isFuzzyMatch(token, contentPart) } }
        }
    }
}

private fun tokenizeForSearch(text: String): List<String> =
    text.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }

private fun levenshteinDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[a.length][b.length]
}

private fun isFuzzyMatch(query: String, target: String): Boolean {
    if (query.isBlank() || target.isBlank()) return false
    if (target.contains(query)) return true
    val distance = levenshteinDistance(query, target)
    val tolerance = when {
        query.length <= 3 -> 1
        query.length <= 6 -> 2
        else -> 3
    }
    return distance <= tolerance || query.contains(target)
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

data class PostVersion(
    val id: Long,
    val postId: Long,
    val title: String,
    val body: String,
    val tag: String,
    val createdAt: Instant
)

data class PromptVersion(
    val id: Long,
    val promptId: Long,
    val title: String,
    val description: String,
    val content: String,
    val tag: String,
    val temperature: String?,
    val context: String?,
    val memoryTokens: String?,
    val createdAt: Instant
)
