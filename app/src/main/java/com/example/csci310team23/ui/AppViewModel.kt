package com.example.csci310team23.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.csci310team23.data.local.CommentVoteEntity
import com.example.csci310team23.data.local.PostVoteEntity
import com.example.csci310team23.data.model.Comment
import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.SearchState
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserSummary
import com.example.csci310team23.data.model.VoteSummary
import com.example.csci310team23.data.model.toDomain
import com.example.csci310team23.data.model.toProfile
import com.example.csci310team23.data.repository.AppDataSnapshot
import com.example.csci310team23.data.repository.AppRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.comparisons.compareByDescending
import kotlin.comparisons.thenByDescending

data class UserSearchResult(
    val user: UserSummary,
    val prompts: List<Prompt>
)

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val hasSeenLanding: Boolean = false
)

enum class AuthMode { SIGN_IN, REGISTER }

data class AppUiState(
    val currentUser: UserProfile? = null,
    val requiresProfileSetup: Boolean = false,
    val posts: List<Post> = emptyList(),
    val prompts: List<Prompt> = emptyList(),
    val trending: List<Post> = emptyList(),
    val searchState: SearchState = SearchState(),
    val watchedTags: Set<String> = emptySet(),
    val watchedUsers: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AppViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
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

                // Filter posts for feed based on watched content
                val feedPosts = filterFeedPosts(allPosts, userId, _uiState.value.watchedTags, _uiState.value.watchedUsers)

                val trending = allPosts
                    .filter { it.isPublished }
                    .sortedWith(
                        compareByDescending<Post> { it.voteSummary.upvotes }
                            .thenByDescending { it.voteSummary.score }
                            .thenByDescending { it.createdAt }
                    ).take(TRENDING_LIMIT)

                val requiresProfile = currentUser?.let { !it.isProfileComplete } == true

                _uiState.update { state ->
                    val updatedSearch = state.searchState.recompute(allPosts, allPrompts, snapshot.users.map { it.toProfile() })
                    state.copy(
                        currentUser = currentUser,
                        requiresProfileSetup = requiresProfile,
                        posts = feedPosts,
                        prompts = allPrompts,
                        trending = trending,
                        searchState = updatedSearch
                    )
                }
            }
        }
    }

    private fun filterFeedPosts(
        allPosts: List<Post>,
        userId: Long?,
        watchedTags: Set<String>,
        watchedUsers: Set<Long>
    ): List<Post> {
        return allPosts.filter { post ->
            // Show user's own posts/drafts
            post.author.id == userId ||
                    // Show published posts from watched users
                    (post.isPublished && post.author.id in watchedUsers) ||
                    // Show published posts with watched tags
                    (post.isPublished && post.tag in watchedTags)
        }
    }

    fun completeLanding() {
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
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to complete profile") }
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

    fun logout() {
        currentUserId.value = null
        _uiState.update { it.copy(infoMessage = "Signed out", errorMessage = null) }
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

    fun createPrompt(title: String, description: String, content: String, tag: String, isPrivate: Boolean = false) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.createPrompt(userId, title, description, content, tag, isPrivate)
                val message = if (isPrivate) "Private prompt saved" else "Prompt shared with community"
                _uiState.update { it.copy(infoMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unable to share prompt") }
            }
        }
    }

    fun updatePrompt(promptId: Long, title: String, description: String, content: String, tag: String, isPrivate: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePrompt(promptId, title, description, content, tag, isPrivate)
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
            ).recompute(state.posts, state.prompts, state.searchState.allUsers)
            state.copy(searchState = newSearch)
        }
    }

    fun searchPrompts(tag: String) {
        _uiState.update { state ->
            val newSearch = state.searchState.copy(
                promptTag = tag
            ).recompute(state.posts, state.prompts, state.searchState.allUsers)
            state.copy(searchState = newSearch)
        }
    }

    fun searchUsers(email: String) {
        _uiState.update { state ->
            val newSearch = state.searchState.copy(
                userEmail = email
            ).recompute(state.posts, state.prompts, state.searchState.allUsers)
            state.copy(searchState = newSearch)
        }
    }

    fun watchTag(tag: String) {
        _uiState.update { state ->
            val newTags = if (tag in state.watchedTags) {
                state.watchedTags - tag
            } else {
                state.watchedTags + tag
            }
            state.copy(watchedTags = newTags)
        }
    }

    fun watchUser(userId: Long) {
        _uiState.update { state ->
            val newUsers = if (userId in state.watchedUsers) {
                state.watchedUsers - userId
            } else {
                state.watchedUsers + userId
            }
            state.copy(watchedUsers = newUsers)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun refreshAuthError() {
        _authState.update { it.copy(errorMessage = null) }
    }

    private fun buildAllPosts(
        snapshot: AppDataSnapshot,
        userProfiles: Map<Long, UserProfile>,
        currentUserId: Long?
    ): List<Post> {
        val userSummaries = userProfiles.mapValues { (_, profile) -> profile.summary }
        val commentVotes = snapshot.commentVotes.groupBy { it.commentId }
        val postVotes = snapshot.postVotes.groupBy { it.postId }
        val commentsByPost = snapshot.comments.groupBy { it.postId }

        return snapshot.posts.sortedByDescending { it.createdAt }.mapNotNull { post ->
            // Show all posts - filtering happens in filterFeedPosts
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
    }

    private fun buildPrompts(
        snapshot: AppDataSnapshot,
        userProfiles: Map<Long, UserProfile>,
        currentUserId: Long?
    ): List<Prompt> {
        val userSummaries = userProfiles.mapValues { (_, profile) -> profile.summary }
        return snapshot.prompts.sortedByDescending { it.createdAt }
            .mapNotNull { prompt ->
                // Filter: show public prompts OR user's own private prompts
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

        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                        return AppViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}