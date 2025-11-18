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
    val allPosts: List<Post> = emptyList(),
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
    private val _watchedTagsHistoryFlow = MutableStateFlow<List<TagWatchHistory>>(emptyList())
    private val _watchedUsersHistoryFlow = MutableStateFlow<List<UserWatchHistory>>(emptyList())

    private val currentUserId = MutableStateFlow<Long?>(null)

    private data class CombinedData(
        val currentUser: UserProfile?,
        val requiresProfileSetup: Boolean,
        val feedPosts: List<Post>,
        val allPosts: List<Post>,
        val allPrompts: List<Prompt>,
        val trending: List<Post>,
        val allUsers: List<UserProfile>,
        val userId: Long?
    )

    init {
        viewModelScope.launch {
            combine(
                currentUserId,
                repository.observeData(),
                _watchedTagsHistoryFlow,
                _watchedUsersHistoryFlow
            ) { userId, snapshot, watchedTags, watchedUsers ->
                val userProfiles = snapshot.users.associate { it.id to it.toProfile() }
                val currentUser = userId?.let { userProfiles[it] }
                val allPosts = buildAllPosts(snapshot, userProfiles, userId)
                val allPrompts = buildPrompts(snapshot, userProfiles, userId)
                val allUsers = snapshot.users.map { it.toProfile() }

                val feedPosts = filterFeedPosts(
                    allPosts,
                    userId,
                    watchedTags,
                    watchedUsers,
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

                CombinedData(
                    currentUser = currentUser,
                    requiresProfileSetup = requiresProfile,
                    feedPosts = feedPosts,
                    allPosts = allPosts,
                    allPrompts = allPrompts,
                    trending = trending,
                    allUsers = allUsers,
                    userId = userId
                )
            }.collect { combinedData ->
                _uiState.update { state ->
                    val updatedSearch = state.searchState.recompute(
                        combinedData.allPosts,
                        combinedData.allPrompts,
                        combinedData.allUsers,
                        combinedData.userId
                    )
                    state.copy(
                        currentUser = combinedData.currentUser,
                        requiresProfileSetup = combinedData.requiresProfileSetup,
                        posts = combinedData.feedPosts,
                        allPosts = combinedData.allPosts,
                        prompts = combinedData.allPrompts,
                        trending = combinedData.trending,
                        searchState = updatedSearch,
                        allUsers = combinedData.allUsers
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
        allUsers: List<UserProfile>
    ): List<Post> {
        if (userId == null) return emptyList()

        val userPosts = allPosts.filter { post ->
            post.author.id == userId
        }

        val emailToUserIdMap = allUsers.associate { it.email to it.id }

        val watchedUserPosts = allPosts.filter { post ->
            post.isPublished && watchedUsersHistory.any { userHistory ->
                val postUserId = post.author.id
                val matchesUser = emailToUserIdMap[userHistory.email] == postUserId

                if (matchesUser) {
                    if (userHistory.endTime == null) {
                        true
                    } else {
                        post.createdAt.isAfter(userHistory.startTime) &&
                                post.createdAt.isBefore(userHistory.endTime)
                    }
                } else {
                    false
                }
            }
        }

        val watchedTagPosts = allPosts.filter { post ->
            post.isPublished && watchedTagsHistory.any { tagHistory ->
                val matchesTag = post.tag == tagHistory.tag
                val isInTimeRange = post.createdAt.isAfter(tagHistory.startTime) &&
                        (tagHistory.endTime == null || post.createdAt.isBefore(tagHistory.endTime))

                matchesTag && isInTimeRange
            }
        }

        val limitedWatchedUserPosts = watchedUserPosts
            .sortedByDescending { it.createdAt }
            .take(8)

        val limitedWatchedTagPosts = watchedTagPosts
            .sortedByDescending { it.createdAt }
            .take(8)

        val combinedPosts = (userPosts + limitedWatchedUserPosts + limitedWatchedTagPosts)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }

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
                    loadWatchHistory(user.id)
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
                state.allPosts,
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
                state.allPosts,
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
                state.allPosts,
                state.prompts,
                state.allUsers,
                state.currentUser?.id
            )
            state.copy(searchState = newSearch)
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedRefreshing = true) }
            try {
                _watchedTagsHistoryFlow.value = _uiState.value.watchedTagsHistory
                _watchedUsersHistoryFlow.value = _uiState.value.watchedUsersHistory
                delay(500)
            } finally {
                _uiState.update { it.copy(isFeedRefreshing = false) }
            }
        }
    }

    fun watchTag(tag: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            val existingHistory = _uiState.value.watchedTagsHistory.find { it.tag == tag }

            val isCurrentlyWatching = existingHistory?.endTime == null

            _uiState.update { state ->
                val newHistory = if (existingHistory != null) {
                    if (isCurrentlyWatching) {
                        state.watchedTagsHistory.map { history ->
                            if (history.tag == tag) history.copy(endTime = Instant.now()) else history
                        }
                    } else {
                        state.watchedTagsHistory.map { history ->
                            if (history.tag == tag) history.copy(
                                endTime = null,
                                startTime = Instant.now()
                            ) else history
                        }
                    }
                } else {
                    state.watchedTagsHistory + TagWatchHistory(
                        tag = tag,
                        startTime = Instant.now(),
                        endTime = null
                    )
                }
                state.copy(watchedTagsHistory = newHistory)
            }

            val currentState = _uiState.value
            val updatedHistory = currentState.watchedTagsHistory.find { it.tag == tag }

            if (updatedHistory != null) {
                repository.saveTagWatchHistory(
                    userId,
                    tag,
                    updatedHistory.startTime,
                    updatedHistory.endTime
                )
            }

            _watchedTagsHistoryFlow.value = _uiState.value.watchedTagsHistory

            val action = if (isCurrentlyWatching) "stopped watching" else "watching"
            _uiState.update { it.copy(infoMessage = "Now $action #$tag") }
        }
    }

    fun watchUser(email: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch

            val user = repository.getUserByEmail(email)
            if (user == null) {
                _uiState.update { state ->
                    state.copy(errorMessage = "User with email $email not found")
                }
                return@launch
            }

            val existingHistory = _uiState.value.watchedUsersHistory.find {
                it.email.equals(email, ignoreCase = true)
            }

            val isCurrentlyWatching = existingHistory?.endTime == null

            _uiState.update { state ->
                val newHistory = if (existingHistory != null) {
                    if (isCurrentlyWatching) {
                        state.watchedUsersHistory.map { history ->
                            if (history.email.equals(email, ignoreCase = true))
                                history.copy(endTime = Instant.now())
                            else history
                        }
                    } else {
                        state.watchedUsersHistory.map { history ->
                            if (history.email.equals(email, ignoreCase = true))
                                history.copy(endTime = null, startTime = Instant.now())
                            else history
                        }
                    }
                } else {
                    state.watchedUsersHistory + UserWatchHistory(
                        email = email,
                        startTime = Instant.now(),
                        endTime = null
                    )
                }
                state.copy(watchedUsersHistory = newHistory)
            }

            val currentState = _uiState.value
            val updatedHistory = currentState.watchedUsersHistory.find {
                it.email.equals(email, ignoreCase = true)
            }

            if (updatedHistory != null) {
                repository.saveUserWatchHistory(
                    userId,
                    email,
                    updatedHistory.startTime,
                    updatedHistory.endTime
                )
            }

            _watchedUsersHistoryFlow.value = _uiState.value.watchedUsersHistory

            val action = if (isCurrentlyWatching) "stopped watching" else "watching"
            _uiState.update { it.copy(infoMessage = "Now $action ${user.name}") }
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

            post.toDomain(author, postComments, summary, currentVote)
        }
        return posts
    }

    private fun loadWatchHistory(userId: Long) {
        viewModelScope.launch {
            val tagHistory = repository.getTagWatchHistory(userId)
            val userHistory = repository.getUserWatchHistory(userId)

            _uiState.update { state ->
                state.copy(
                    watchedTagsHistory = tagHistory,
                    watchedUsersHistory = userHistory
                )
            }

            _watchedTagsHistoryFlow.value = tagHistory
            _watchedUsersHistoryFlow.value = userHistory
        }
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
                email = profile.email,
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
