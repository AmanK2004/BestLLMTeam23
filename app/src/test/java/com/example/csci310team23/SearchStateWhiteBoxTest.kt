package com.example.csci310team23

import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.SearchState
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserSummary
import com.example.csci310team23.data.model.VoteSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchStateWhiteBoxTest {

    private val baseTime = Instant.parse("2024-03-01T00:00:00Z")
    private val alice = userProfile(1L, "Alice Kim", "alice@usc.edu")
    private val bob = userProfile(2L, "Bob Lee", "bob@usc.edu")

    @Test
    fun searchPostsByTag_filtersCaseInsensitive() {
        val posts = listOf(
            post(1, alice.summary, "GPT-4", "Alpha"),
            post(2, bob.summary, "claude", "Beta")
        )
        val state = SearchState(postSearchType = PostSearchType.TAG, keyword = "gpt-4")
        val recomputed = state.recompute(posts, emptyList(), listOf(alice, bob), currentUserId = alice.id)
        assertEquals(1, recomputed.postResults.size)
        assertEquals("Alpha", recomputed.postResults.first().title)
    }

    @Test
    fun searchPrompts_filtersByTagAndVisibility() {
        val prompts = listOf(
            prompt(1, alice.summary, "claude", isPrivate = false),
            prompt(2, alice.summary, "claude", isPrivate = true),
            prompt(3, bob.summary, "gpt-4", isPrivate = false)
        )
        val state = SearchState(promptTag = "claude")
        val recomputed = state.recompute(emptyList(), prompts, listOf(alice, bob), currentUserId = bob.id)
        assertEquals(1, recomputed.promptResults.size)
        assertEquals(1L, recomputed.promptResults.first().id)
    }

    @Test
    fun searchUsers_includesPrivatePromptsOnlyForCurrentUser() {
        val prompts = listOf(
            prompt(1, alice.summary, "gpt-4", isPrivate = false),
            prompt(2, alice.summary, "gpt-4", isPrivate = true)
        )
        val state = SearchState(userEmail = "ali")
        val seenByBob = state.recompute(emptyList(), prompts, listOf(alice, bob), currentUserId = bob.id)
        assertEquals(1, seenByBob.userResults.first().prompts.size)

        val seenByAlice = state.recompute(emptyList(), prompts, listOf(alice, bob), currentUserId = alice.id)
        assertEquals(2, seenByAlice.userResults.first().prompts.size)
    }

    private fun userProfile(id: Long, name: String, email: String) = UserProfile(
        id = id,
        name = name,
        email = email,
        studentId = "1234567890",
        department = "CS",
        school = "Viterbi",
        birthDate = null,
        bio = "",
        isProfileComplete = true
    )

    private fun post(
        id: Long,
        author: UserSummary,
        tag: String,
        title: String
    ) = Post(
        id = id,
        author = author,
        title = title,
        body = "Body $title",
        tag = tag,
        createdAt = baseTime,
        updatedAt = baseTime,
        commentCount = 0,
        comments = emptyList(),
        voteSummary = VoteSummary(0, 0),
        currentUserVote = null
    )

    private fun prompt(
        id: Long,
        author: UserSummary,
        tag: String,
        isPrivate: Boolean
    ) = Prompt(
        id = id,
        author = author,
        title = "Prompt $id",
        description = "Desc",
        content = "Content",
        tag = tag,
        temperature = null,
        context = null,
        memoryTokens = null,
        createdAt = baseTime,
        updatedAt = baseTime,
        isPrivate = isPrivate
    )
}