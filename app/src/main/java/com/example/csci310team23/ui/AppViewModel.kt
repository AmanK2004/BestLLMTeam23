package com.example.csci310team23.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.csci310team23.data.local.PreferencesManager
import com.example.csci310team23.data.model.AuthMode
import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.SearchState
import com.example.csci310team23.data.model.TagWatchHistory
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserSummary
import com.example.csci310team23.data.model.UserWatchHistory
import com.example.csci310team23.data.model.VoteSummary
import com.example.csci310team23.data.model.toDomain
import com.example.csci310team23.data.model.toProfile
import com.example.csci310team23.data.repository.AppDataSnapshot
import com.example.csci310team23.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class UserSearchResult(
    val user: UserSummary,
    val prompts: List<Prompt>
)

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val hasSeenLanding: Boolean = false
)

data class AppUiState(
    val currentUser: UserProfile? = null,
    val requiresProfileSetup: Boolean = false,
    val posts: List<Post> = emptyList(),
    val prompts: List<Prompt> = emptyList(),
    val trending: List<Post> = emptyList(),
    val searchState: SearchState = SearchState(),
    val watchedTagsHistory: List<TagWatchHistory> = emptyList(),
    val watchedUsersHistory: List<UserWatchHistory> = emptyList(),
    val allUsers: List<UserProfile> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isFeedRefreshing: Boolean = false
)


class AppViewModel(
    private val repository: AppRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val _authState = MutableStateFlow(
        AuthUiState(hasSeenLanding = preferencesManager.hasSeenLanding)
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val currentUserId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            combine(currentUserId, repository.observeData()) { userId, snapshot ->
                userId to snapshot
            }.collect { (userId, snapshot) ->
                val userProfiles = snapshot.users.associate { it.id to it.toProfile() }
                val currentUser = userId?.let { userProfiles[it] }
                val allPosts = buildAllPosts(snapshot, userProfiles, userId)
                val allPrompts = buildPrompts(snapshot, userProfiles, userId)
                val allUsers = snapshot.users.map { it.toProfile() }

                // REMOVE THE DUPLICATE - use only this one feedPosts declaration
                val feedPosts = filterFeedPosts(
                    allPosts,
                    userId,
                    _uiState.value.watchedTagsHistory,
                    _uiState.value.watchedUsersHistory,
                    allUsers
                )

                val trending = allPosts
                    .filter { it.isPublished }
                    .sortedWith(
                        compareByDescending<Post> { it.voteSummary.upvotes }
                            .thenByDescending { it.voteSummary.score }
                            .thenByDescending { it.createdAt }
                    ).take(TRENDING_LIMIT)

                val requiresProfile = currentUser?.let { !it.isProfileComplete } == true

                _uiState.update { state ->
                    val updatedSearch =
                        state.searchState.recompute(allPosts, allPrompts, allUsers, userId)
                    state.copy(
                        currentUser = currentUser,
                        requiresProfileSetup = requiresProfile,
                        posts = feedPosts,
                        prompts = allPrompts,
                        trending = trending,
                        searchState = updatedSearch,
                        allUsers = allUsers
                    )
                }
            }
        }
    }

    private fun filterFeedPosts(
        allPosts: List<Post>,
        userId: Long?,
        watchedTagsHistory: List<TagWatchHistory>,
        watchedUsersHistory: List<UserWatchHistory>,
        allUsers: List<UserProfile> // Add this parameter
    ): List<Post> {
        val userPosts = allPosts.filter { post ->
            post.author.id == userId
        }

        // Create a mapping of user IDs to emails for fallback
        val userIdToEmailMap = allUsers.associate { it.id to it.email }

        // Debug: Print watched users and their emails
        println("DEBUG: Watched users history: $watchedUsersHistory")
        println("DEBUG: All posts count: ${allPosts.size}")
        allPosts.forEach { post ->
            println("DEBUG: Post ${post.id} by ${post.author.email} (${post.author.name})")
        }

        // Get posts from watched users (all historical posts for users that were ever watched)
        val watchedUserPosts = allPosts.filter { post ->
            post.isPublished && watchedUsersHistory.any { userHistory ->
                // Try matching by email first, then fall back to user ID lookup
                val matchesByEmail = post.author.email == userHistory.email
                val matchesById = userIdToEmailMap[post.author.id] == userHistory.email

                val matches = matchesByEmail || matchesById
                val isInTimeRange =
                    userHistory.endTime == null || post.createdAt.isBefore(userHistory.endTime)

                if (matches) {
                    println("DEBUG: Post ${post.id} matches user ${userHistory.email}, in time range: $isInTimeRange")
                }

                matches && isInTimeRange
            }
        }

        // Get posts from watched tags (only during watch period)
        val watchedTagPosts = allPosts.filter { post ->
            post.isPublished && watchedTagsHistory.any { tagHistory ->
                val matches = post.tag == tagHistory.tag
                val isInTimeRange = post.createdAt.isAfter(tagHistory.startTime) &&
                        (tagHistory.endTime == null || post.createdAt.isBefore(tagHistory.endTime))

                if (matches) {
                    println("DEBUG: Post ${post.id} matches tag ${tagHistory.tag}, in time range: $isInTimeRange")
                }

                matches && isInTimeRange
            }
        }

        // Debug: Print what we found
        println("DEBUG: User posts: ${userPosts.size}")
        println("DEBUG: Watched user posts: ${watchedUserPosts.size}")
        println("DEBUG: Watched tag posts: ${watchedTagPosts.size}")

        // Apply limits: 8 from users, 8 from tags
        val limitedUserPosts = watchedUserPosts
            .sortedByDescending { it.createdAt }
            .take(8)

        val limitedTagPosts = watchedTagPosts
            .sortedByDescending { it.createdAt }
            .take(8)

        // Combine: own posts + limited watched user posts + limited watched tag posts
        val combinedPosts = (userPosts + limitedUserPosts + limitedTagPosts)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }

        println("DEBUG: Final combined posts: ${combinedPosts.size}")
        combinedPosts.forEach { post ->
            println("DEBUG: Final post ${post.id} by ${post.author.email}")
        }

        return combinedPosts
    }

    fun completeLanding() {
        preferencesManager.hasSeenLanding = true
        _authState.update { it.copy(hasSeenLanding = true) }
    }

    fun setAuthMode(mode: AuthMode) {
        _authState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val user = repository.authenticate(email, password)
                if (user != null) {
                    currentUserId.value = user.id
                } else {
                    _authState.update { it.copy(errorMessage = "Invalid credentials") }
                }
            } catch (e: Exception) {
                _authState.update { it.copy(errorMessage = e.message ?: "Unable to sign in") }
            } finally {
                _authState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun register(name: String, email: String, studentId: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val profile = repository.registerUser(name, email, studentId, password)
                currentUserId.value = profile.id
            } catch (e: Exception) {
                _authState.update { it.copy(errorMessage = e.message ?: "Unable to register") }
            } finally {
                _authState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun completeProfile(department: String, school: String, birthDate: LocalDate?, bio: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                val profile = repository.completeProfile(userId, department, school, birthDate, bio)
                _uiState.update { it.copy(currentUser = profile, infoMessage = "Profile updated") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Unable to complete profile"
                    )
                }
            }
        }
    }

    fun updateProfile(birthDate: LocalDate?, bio: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                val profile = repository.updateProfile(userId, birthDate, bio)
                _uiState.update { it.copy(currentUser = profile, infoMessage = "Profile updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to update profile") }
            }
        }
    }

    fun resetPassword(newPassword: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.resetPassword(userId, newPassword)
                _uiState.update { it.copy(infoMessage = "Password reset successful") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to reset password") }
            }
        }
    }

    fun createPost(title: String, body: String, tag: String, isDraft: Boolean = false) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                val isPublished = !isDraft
                repository.createPost(userId, title, body, tag, isPublished)
                val message = if (isDraft) "Post saved as draft" else "Post published"
                _uiState.update { it.copy(infoMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to create post") }
            }
        }
    }

    fun updatePost(postId: Long, title: String, body: String, tag: String) {
        viewModelScope.launch {
            try {
                repository.updatePost(postId, title, body, tag)
                _uiState.update { it.copy(infoMessage = "Post updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to update post") }
            }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
                _uiState.update { it.copy(infoMessage = "Post deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to delete post") }
            }
        }
    }

    fun publishDraft(postId: Long) {
        viewModelScope.launch {
            try {
                repository.publishDraft(postId)
                _uiState.update { it.copy(infoMessage = "Post published") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to publish post") }
            }
        }
    }

    fun createComment(postId: Long, title: String?, body: String) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.createComment(postId, userId, title, body)
                _uiState.update { it.copy(infoMessage = "Comment added") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to add comment") }
            }
        }
    }

    fun updateComment(commentId: Long, title: String?, body: String) {
        viewModelScope.launch {
            try {
                repository.updateComment(commentId, title, body)
                _uiState.update { it.copy(infoMessage = "Comment updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to update comment") }
            }
        }
    }

    fun voteOnPost(postId: Long, value: Int) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.voteOnPost(postId, userId, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to vote on post") }
            }
        }
    }

    fun voteOnComment(commentId: Long, value: Int) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.voteOnComment(commentId, userId, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to vote on comment") }
            }
        }
    }

    fun createPrompt(
        title: String,
        description: String,
        content: String,
        tag: String,
        temperature: String?,
        context: String?,
        memoryTokens: String?,
        isPrivate: Boolean = false
    ) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.createPrompt(
                    userId,
                    title,
                    description,
                    content,
                    tag,
                    temperature,
                    context,
                    memoryTokens,
                    isPrivate
                )
                val message =
                    if (isPrivate) "Private prompt saved" else "Prompt shared with community"
                _uiState.update { it.copy(infoMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to share prompt") }
            }
        }
    }

    fun updatePrompt(
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
        viewModelScope.launch {
            try {
                repository.updatePrompt(
                    promptId,
                    title,
                    description,
                    content,
                    tag,
                    temperature,
                    context,
                    memoryTokens,
                    isPrivate
                )
                _uiState.update { it.copy(infoMessage = "Prompt updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to update prompt") }
            }
        }
    }

    fun deletePrompt(promptId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePrompt(promptId)
                _uiState.update { it.copy(infoMessage = "Prompt removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to delete prompt") }
            }
        }
    }

    fun searchPosts(type: PostSearchType, keyword: String) {
        _uiState.update { state ->
            val newSearch = state.searchState.copy(
                postSearchType = type,
                keyword = keyword
            ).recompute(
                state.posts,
                state.prompts,
                state.allUsers,
                state.currentUser?.id
            )
            state.copy(searchState = newSearch)
        }
    }

    fun searchPrompts(tag: String) {
        _uiState.update { state ->
            val newSearch = state.searchState.copy(
                promptTag = tag
            ).recompute(
                state.posts,
                state.prompts,
                state.allUsers,
                state.currentUser?.id
            )
            state.copy(searchState = newSearch)
        }
    }

    fun searchUsers(email: String) {
        _uiState.update { state ->
            val newSearch = state.searchState.copy(
                userEmail = email
            ).recompute(
                state.posts,
                state.prompts,
                state.allUsers,
                state.currentUser?.id
            )
            state.copy(searchState = newSearch)
        }
    }

    // Add this function to force refresh the feed
// Update the refreshFeed function to actually refresh data
    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedRefreshing = true) }
            try {
                // Force a small delay to show the refresh indicator
                delay(1000)
                // The feed will automatically update because we're using flows
                // The combine block in init will recompute the feed with current watch history
            } finally {
                _uiState.update { it.copy(isFeedRefreshing = false) }
            }
        }
    }

    // Update watchTag function to trigger refresh
    fun watchTag(tag: String) {
        _uiState.update { state ->
            val existingHistory = state.watchedTagsHistory.find { it.tag == tag }
            val newHistory = if (existingHistory != null) {
                // If currently watching, stop watching by setting end time
                if (existingHistory.endTime == null) {
                    state.watchedTagsHistory.map { history ->
                        if (history.tag == tag) history.copy(endTime = Instant.now()) else history
                    }
                } else {
                    // If previously watched, start watching again
                    state.watchedTagsHistory + TagWatchHistory(
                        tag = tag,
                        startTime = Instant.now(),
                        endTime = null
                    )
                }
            } else {
                // New watch
                state.watchedTagsHistory + TagWatchHistory(
                    tag = tag,
                    startTime = Instant.now(),
                    endTime = null
                )
            }
            state.copy(watchedTagsHistory = newHistory)
        }
        // Trigger refresh after updating watch state
        refreshFeed()
    }

    // Update watchUser function to trigger refresh
    fun watchUser(email: String) {
        viewModelScope.launch {
            // Validate that the user exists
            val user = repository.getUserByEmail(email)
            if (user == null) {
                _uiState.update { state ->
                    state.copy(errorMessage = "User with email $email not found")
                }
                return@launch
            }

            _uiState.update { state ->
                val existingHistory = state.watchedUsersHistory.find { it.email == email }
                val newHistory = if (existingHistory != null) {
                    // If currently watching, stop watching by setting end time
                    if (existingHistory.endTime == null) {
                        state.watchedUsersHistory.map { history ->
                            if (history.email == email) history.copy(endTime = Instant.now()) else history
                        }
                    } else {
                        // If previously watched, start watching again
                        state.watchedUsersHistory + UserWatchHistory(
                            email = email,
                            startTime = Instant.now(),
                            endTime = null
                        )
                    }
                } else {
                    // New watch
                    state.watchedUsersHistory + UserWatchHistory(
                        email = email,
                        startTime = Instant.now(),
                        endTime = null
                    )
                }
                state.copy(watchedUsersHistory = newHistory)
            }

            // Show success message
            _uiState.update { state ->
                val existingHistory = state.watchedUsersHistory.find { it.email == email }
                val action =
                    if (existingHistory?.endTime == null) "watching" else "stopped watching"
                state.copy(infoMessage = "Now $action ${user.name}")
            }
        }
    }

    fun setShowCommentTitles(show: Boolean) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            preferencesManager.setShowCommentTitles(userId, show)
        }
    }

    fun getShowCommentTitles(): Boolean {
        val userId = currentUserId.value ?: return true
        return preferencesManager.getShowCommentTitles(userId)
    }

    fun logout() {
        _authState.update { it.copy(infoMessage = "Signed out successfully") }
        currentUserId.value = null
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
        _authState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun buildAllPosts(
        snapshot: AppDataSnapshot,
        userProfiles: Map<Long, UserProfile>,
        currentUserId: Long?
    ): List<Post> {
        val userSummaries = userProfiles.mapValues { (_, profile) ->
            UserSummary(
                id = profile.id,
                name = profile.name,
                email = profile.email,
                department = profile.department,
                school = profile.school
            )
        }

        // Debug: Print user information
        println("DEBUG: Building posts - total users: ${userProfiles.size}")
        userProfiles.forEach { (id, profile) ->
            println("DEBUG: User $id: ${profile.name} (${profile.email})")
        }

        val commentVotes = snapshot.commentVotes.groupBy { it.commentId }
        val postVotes = snapshot.postVotes.groupBy { it.postId }
        val commentsByPost = snapshot.comments.groupBy { it.postId }

        val posts = snapshot.posts.sortedByDescending { it.createdAt }.mapNotNull { post ->
            val author = userSummaries[post.authorId] ?: return@mapNotNull null
            val postComments = commentsByPost[post.id].orEmpty().mapNotNull { comment ->
                val commentAuthor = userSummaries[comment.authorId] ?: return@mapNotNull null
                val votes = commentVotes[comment.id].orEmpty()
                val summary = votes.toVoteSummary { it.value }
                val currentVote = currentUserId?.let { id ->
                    votes.firstOrNull { it.userId == id }?.value
                }
                comment.toDomain(commentAuthor, summary, currentVote)
            }
            val votes = postVotes[post.id].orEmpty()
            val summary = votes.toVoteSummary { it.value }
            val currentVote = currentUserId?.let { id ->
                votes.firstOrNull { it.userId == id }?.value
            }

            // Debug: Print post information
            println("DEBUG: Post ${post.id} by ${author.email} (${author.name}) - tag: ${post.tag}")

            post.toDomain(author, postComments, summary, currentVote)
        }

        println("DEBUG: Built ${posts.size} posts total")
        return posts
    }

    private fun buildPrompts(
        snapshot: AppDataSnapshot,
        userProfiles: Map<Long, UserProfile>,
        currentUserId: Long?
    ): List<Prompt> {
        val userSummaries = userProfiles.mapValues { (_, profile) ->
            UserSummary(
                id = profile.id,
                name = profile.name,
                email = profile.email, // Include email
                department = profile.department,
                school = profile.school
            )
        }
        return snapshot.prompts.sortedByDescending { it.createdAt }
            .mapNotNull { prompt ->
                if (prompt.isPrivate && prompt.authorId != currentUserId) {
                    return@mapNotNull null
                }

                val author = userSummaries[prompt.authorId] ?: return@mapNotNull null
                prompt.toDomain(author)
            }
    }

    private inline fun <T> List<T>.toVoteSummary(crossinline selector: (T) -> Int): VoteSummary =
        VoteSummary(
            upvotes = count { selector(it) > 0 },
            downvotes = count { selector(it) < 0 }
        )

    companion object {
        private const val TRENDING_LIMIT = 5
        fun provideFactory(
            repository: AppRepository,
            preferencesManager: PreferencesManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                        return AppViewModel(repository, preferencesManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}