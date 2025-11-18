package com.example.csci310team23

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@LargeTest
class FeedInteractionsTest {

    private val dataRule = TestDataRule(TEST_EMAIL, TEST_PASSWORD)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    private fun createPrompt(
        title: String,
        description: String,
        content: String,
        tag: String,
        asPrivate: Boolean = false,
        temperature: String? = null,
        context: String? = null,
        memoryTokens: String? = null
    ) {
        ensurePromptComposerVisible()
        composeRule.onNodeWithTag("create_prompt_title_field").performTextReplacement(title)
        composeRule.onNodeWithTag("create_prompt_description_field").performTextReplacement(description)
        composeRule.onNodeWithTag("create_prompt_content_field").performTextReplacement(content)
        composeRule.onNodeWithTag("create_prompt_tag_field").performTextReplacement(tag)

        val includeOptional = listOf(temperature, context, memoryTokens).any { !it.isNullOrBlank() }
        if (includeOptional) {
            composeRule.onNodeWithTag("create_prompt_optional_toggle").performClick()
            temperature?.let { composeRule.onNodeWithTag("create_prompt_temperature_field").performTextReplacement(it) }
            context?.let { composeRule.onNodeWithTag("create_prompt_context_field").performTextReplacement(it) }
            memoryTokens?.let { composeRule.onNodeWithTag("create_prompt_memory_field").performTextReplacement(it) }
        }

        setPromptPrivacy(asPrivate)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("create_prompt_submit_button").performClick()
        waitForText(title)
    }


    @get:Rule
    val chain: TestRule = RuleChain.outerRule(dataRule).around(composeRule)

    @Test
    fun publishPost_displaysInFeed() {
        loginAndOpenFeed()
        val title = uniqueTitle("Published")
        createPost(title = title, body = "Sharing tips", tag = "GPT-4")

        waitForText(title)
        composeRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun publishDraft_hidesBadge() {
        val draftTitle = uniqueTitle("Draft")
        seedPost(draftTitle, "Need review", "Gemini", isPublished = false)

        loginAndOpenFeed()
        waitForText(draftTitle)

        openPostMenu()
        composeRule.onNodeWithText("Publish", useUnmergedTree = true).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !nodeExists(hasText("DRAFT", substring = false))
        }
    }

    @Test
    fun editPost_updatesTitle() {
        val original = uniqueTitle("Original")
        val updated = "$original - Edited"
        seedPost(original, "Body", "GPT-4")

        loginAndOpenFeed()
        waitForText(original)

        openPostMenu()
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("edit_post_title_field").performTextReplacement(updated)
        composeRule.onNodeWithTag("edit_post_body_field").performTextReplacement("Updated body")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("edit_post_save_button").performClick()

        waitForText(updated)
        composeRule.onNodeWithText(updated, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun deletePost_removesCard() {
        val title = uniqueTitle("Delete")
        seedPost(title, "Remove me", "GPT-4")

        loginAndOpenFeed()
        waitForText(title)

        openPostMenu()
        composeRule.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("delete_post_confirm_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !nodeExists(hasText(title, substring = false))
        }
    }

    @Test
    fun emptyComment_notSaved() {
        val title = uniqueTitle("NoComment")
        seedPost(title, "Hold comments", "GPT-4")

        loginAndOpenFeed()
        waitForText(title)

        getFirstNodeWithTag("post_comment_toggle_button").performClick()
        composeRule.onNodeWithTag("comment_title_field").performTextReplacement("Optional title")
        composeRule.onNodeWithTag("comment_body_field").performTextClearance()
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("comment_submit_button").performClick()

        val commentExists = nodeExists(hasText("Comments (", substring = true))
        assertTrue("Comment section should not exist", !commentExists)
    }

    @Test
    fun editComment_updatesBody() {
        val postTitle = uniqueTitle("EditComment")
        val postId = seedPost(postTitle, "Body", "GPT-4")
        seedComment(postId, "Plan", "Needs edits")

        loginAndOpenFeed()
        waitForText(postTitle)
        getFirstNodeWithTag("post_comment_toggle_button").performClick()
        waitForText("Plan")

        waitForText("Comments (1)")
        getFirstNodeWithTag("comment_menu_button").performClick()
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("edit_comment_body_field").performTextReplacement("Edited thoughts")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("edit_comment_save_button").performClick()

        waitForText("Edited thoughts")
        composeRule.onNodeWithText("Edited thoughts", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun upvotePost_updatesScore() {
        val title = uniqueTitle("Upvote")
        seedPost(title, "Vote body", "GPT-4")

        loginAndOpenFeed()
        waitForText(title)

        getFirstNodeWithTag("post_upvote_button").performClick()
        assertVoteScore("post_vote_row", "1")
    }

    @Test
    fun downvotePost_updatesScore() {
        val title = uniqueTitle("Downvote")
        seedPost(title, "Vote body", "GPT-4")

        loginAndOpenFeed()
        waitForText(title)

        getFirstNodeWithTag("post_downvote_button").performClick()
        assertVoteScore("post_vote_row", "-1")
    }

    @Test
    fun clearingPostVote_resetsScore() {
        val title = uniqueTitle("ClearVote")
        seedPost(title, "Vote body", "GPT-4")

        loginAndOpenFeed()
        waitForText(title)

        val upvote = getFirstNodeWithTag("post_upvote_button")
        upvote.performClick()
        assertVoteScore("post_vote_row", "1")
        upvote.performClick()
        assertVoteScore("post_vote_row", "0")
    }

    @Test
    fun commentUpvote_updatesScore() {
        val postTitle = uniqueTitle("CommentVote")
        val postId = seedPost(postTitle, "Body", "GPT-4")
        seedComment(postId, null, "Vote on me")

        loginAndOpenFeed()
        waitForText(postTitle)
        getFirstNodeWithTag("post_comment_toggle_button").performClick()
        waitForText("Vote on me")

        waitForText("Comments (1)")
        getFirstNodeWithTag("comment_upvote_button").performClick()
        assertVoteScore("comment_vote_row", "1")
    }

    @Test
    fun searchByTag_showsMatchingPost() {
        val gptTitle = uniqueTitle("GPT")
        seedPost(gptTitle, "GPT content", "GPT-4")
        seedPost(uniqueTitle("Claude"), "Claude content", "Claude")

        login()
        openSection("nav_search")

        openSection("nav_search")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("search_post_keyword_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("search_post_keyword_field").performTextReplacement("GPT-4")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("search_post_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("post_summary_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(gptTitle, useUnmergedTree = true).assertIsDisplayed()

        val claudeExists = nodeExists(hasText("Claude", substring = false))
        assertTrue("Claude post should not be displayed", !claudeExists)
    }

    @Test
    fun sharePrompt_displaysInMyLibrary() {
        loginAndOpenPrompts()

        val title = uniqueTitle("Prompt")
        createPrompt(
            title = title,
            description = "Prompt for instrumentation",
            content = "Use chain-of-thought",
            tag = "GPT-4"
        )

        composeRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun editPrompt_updatesTitle() {
        val original = uniqueTitle("PromptEdit")
        val updated = "$original Updated"
        seedPrompt(original, "Original description", "Prompt body", "GPT-4")

        loginAndOpenPrompts()
        waitForText(original)

        openPromptMenu()
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("edit_prompt_title_field").performTextReplacement(updated)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("edit_prompt_save_button").performClick()

        waitForText(updated)
        composeRule.onNodeWithText(updated, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun editPrompt_canMakePrivatePromptPublic() {
        val title = uniqueTitle("PromptPrivacy")
        seedPrompt(title, "Hidden", "Private prompt", "Claude", isPrivate = true)

        loginAndOpenPrompts()
        waitForText(title)
        composeRule.onNodeWithText("PRIVATE", useUnmergedTree = true).assertIsDisplayed()

        openPromptMenu()
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("edit_prompt_private_checkbox").performClick()
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("edit_prompt_save_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !nodeExists(hasText("PRIVATE", substring = false))
        }
        composeRule.onNodeWithText("Community", useUnmergedTree = true).performClick()
        waitForText(title)
        composeRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun deletePrompt_removesFromLibrary() {
        val title = uniqueTitle("PromptDelete")
        seedPrompt(title, "Remove", "Old content", "GPT-4")

        loginAndOpenPrompts()
        waitForText(title)

        openPromptMenu()
        composeRule.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("delete_prompt_confirm_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !nodeExists(hasText(title, substring = false))
        }
    }

    @Test
    fun promptWithOptionalDetails_showsValuesWhenExpanded() {
        val title = uniqueTitle("PromptDetails")
        seedPrompt(
            title = title,
            description = "Details",
            content = "Optional info",
            tag = "GPT-4",
            temperature = "0.2",
            context = "Use USC policy",
            memoryTokens = "256"
        )

        loginAndOpenPrompts()
        waitForText(title)

        composeRule.onNodeWithText("Show Details", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Temperature: 0.2", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Context: Use USC policy", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Memory Tokens: 256", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun myPrompts_emptyState_showsGuidance() {
        loginAndOpenPrompts()

        val emptyMessage = "You haven't created any prompts yet. Start building your library!"
        waitForText(emptyMessage)
        composeRule.onNodeWithText(emptyMessage, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun communityPrompt_showsOnCommunityTab() {
        val title = uniqueTitle("CommunityPrompt")
        seedPrompt(title, "Desc", "Prompt body", "GPT-4")

        loginAndOpenPrompts()
        waitForText(title)

        composeRule.onNodeWithTag("prompts_tab_community").performClick()
        waitForText(title)
        composeRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun searchPosts_noResults_showsMessage() {
        val existingTitle = uniqueTitle("ExistingPost")
        seedPost(existingTitle, "Body", "GPT-4")

        login()
        openSection("nav_search")
        waitForNodeWithTag("search_post_keyword_field")

        composeRule.onNodeWithText("TITLE", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("search_post_keyword_field").performTextReplacement("NoMatch")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("search_post_button").performClick()

        waitForText("No results found.")
        composeRule.onNodeWithText("No results found.", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun searchPrompts_byTag_showsMatchingResult() {
        val gptTitle = uniqueTitle("PromptSearchGPT")
        seedPrompt(gptTitle, "Desc", "Prompt body", "GPT-4")
        seedPrompt(uniqueTitle("PromptSearchClaude"), "Desc", "Prompt body", "Claude")

        login()
        openSection("nav_search")
        composeRule.onNodeWithTag("search_tab_prompts").performClick()
        waitForNodeWithTag("search_prompt_tag_field")

        composeRule.onNodeWithTag("search_prompt_tag_field").performTextReplacement("GPT-4")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("search_prompt_button").performClick()

        waitForText(gptTitle)
        composeRule.onNodeWithText(gptTitle, useUnmergedTree = true).assertIsDisplayed()
        assertTrue(!nodeExists(hasText("PromptSearchClaude", substring = true)))
    }

    @Test
    fun searchUsers_byEmail_showsProfileCard() {
        val otherName = "Friend Search"
        val otherEmail = "friend+${System.currentTimeMillis()}@usc.edu"
        val userId = seedAdditionalUser(otherName, otherEmail)
        val promptTitle = uniqueTitle("FriendPrompt")
        seedPromptForUser(userId, promptTitle, "GPT-4")

        login()
        openSection("nav_search")
        composeRule.onNodeWithTag("search_tab_users").performClick()
        waitForNodeWithTag("search_user_email_field")

        composeRule.onNodeWithTag("search_user_email_field").performTextReplacement(otherEmail)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("search_user_button").performClick()

        waitForText(otherName)
        composeRule.onNodeWithText(otherName, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(promptTitle, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun loginAndOpenFeed() {
        login()
        openSection("nav_feed")
        waitForNodeWithTag("toggle_create_post_button")
    }

    private fun loginAndOpenPrompts() {
        login()
        openSection("nav_prompts")
        waitForNodeWithTag("toggle_create_prompt_button")
    }

    private fun login() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_feed").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_feed").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle_create_post_button").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSection(tag: String) {
        waitForNodeWithTag(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun createPost(title: String, body: String, tag: String, asDraft: Boolean = false) {
        ensureComposerVisible()
        composeRule.onNodeWithTag("create_post_title_field").performTextReplacement(title)
        composeRule.onNodeWithTag("create_post_tag_field").performTextReplacement(tag)
        composeRule.onNodeWithTag("create_post_body_field").performTextReplacement(body)
        setDraftCheckbox(asDraft)
        Espresso.closeSoftKeyboard()
        waitForNodeWithTag("create_post_submit_button")
        composeRule.onNodeWithTag("create_post_submit_button").performClick()
        waitForText(title)
    }

    private fun ensureComposerVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle_create_post_button").fetchSemanticsNodes().isNotEmpty()
        }
        val composerVisible = composeRule.onAllNodesWithTag("create_post_title_field")
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (!composerVisible) {
            composeRule.onNodeWithTag("toggle_create_post_button").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("create_post_title_field").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensurePromptComposerVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle_create_prompt_button").fetchSemanticsNodes().isNotEmpty()
        }
        val composerVisible = composeRule.onAllNodesWithTag("create_prompt_title_field")
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (!composerVisible) {
            composeRule.onNodeWithTag("toggle_create_prompt_button").performClick()
        }
        waitForNodeWithTag("create_prompt_title_field")
    }

    private fun getFirstNodeWithTag(tag: String): SemanticsNodeInteraction {
        waitForNodeWithTag(tag)
        return composeRule.onAllNodesWithTag(tag)[0]
    }

    private fun waitForNodeWithTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openPostMenu() {
        getFirstNodeWithTag("post_menu_button").performClick()
    }

    private fun openPromptMenu() {
        getFirstNodeWithTag("prompt_menu_button").performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            nodeExists(hasText(text, substring = false))
        }
    }

    private fun setDraftCheckbox(asDraft: Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("create_post_draft_checkbox").fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNodeWithTag("create_post_draft_checkbox").fetchSemanticsNode()
        val isChecked = node.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
        if (isChecked != asDraft) {
            composeRule.onNodeWithTag("create_post_draft_checkbox").performClick()
        }
    }

    private fun setPromptPrivacy(asPrivate: Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("create_prompt_private_checkbox").fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNodeWithTag("create_prompt_private_checkbox").fetchSemanticsNode()
        val isChecked = node.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
        if (isChecked != asPrivate) {
            composeRule.onNodeWithTag("create_prompt_private_checkbox").performClick()
        }
    }

    private fun nodeExists(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false
    ): Boolean = runCatching {
        composeRule.onNode(matcher, useUnmergedTree).fetchSemanticsNode()
    }.isSuccess

    private fun assertVoteScore(rowTag: String, expectedScore: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(rowTag).fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onAllNodesWithTag(rowTag)[0].fetchSemanticsNode()
        val aggregatedText = buildString {
            node.children.forEach { child ->
                child.config.getOrNull(SemanticsProperties.Text)?.forEach { append(it.text) }
            }
        }
        assertTrue(
            "Expected score $expectedScore but saw '$aggregatedText'",
            aggregatedText.contains(expectedScore)
        )
    }

    private fun seedPost(
        title: String,
        body: String,
        tag: String,
        isPublished: Boolean = true
    ): Long = withTestUser { userId ->
        dataRule.repository.createPost(userId, title, body, tag, isPublished)
    }

    private fun seedPrompt(
        title: String,
        description: String,
        content: String,
        tag: String,
        temperature: String? = null,
        context: String? = null,
        memoryTokens: String? = null,
        isPrivate: Boolean = false
    ): Long = withTestUser { userId ->
        dataRule.repository.createPrompt(
            authorId = userId,
            title = title,
            description = description,
            content = content,
            tag = tag,
            temperature = temperature,
            context = context,
            memoryTokens = memoryTokens,
            isPrivate = isPrivate
        )
    }

    private fun seedPromptForUser(
        userId: Long,
        title: String,
        tag: String,
        description: String = "Search prompt",
        content: String = "Prompt content"
    ): Long = runBlocking {
        withContext(Dispatchers.IO) {
            dataRule.repository.createPrompt(
                authorId = userId,
                title = title,
                description = description,
                content = content,
                tag = tag,
                temperature = null,
                context = null,
                memoryTokens = null,
                isPrivate = false
            )
        }
    }

    private fun seedComment(postId: Long, title: String?, body: String): Long = withTestUser { userId ->
        dataRule.repository.createComment(postId, userId, title, body)
    }

    private fun seedAdditionalUser(name: String, email: String): Long = runBlocking {
        withContext(Dispatchers.IO) {
            val profile = dataRule.repository.registerUser(
                name = name,
                email = email,
                studentId = uniqueStudentId(),
                password = TEST_PASSWORD
            )
            dataRule.repository.completeProfile(
                userId = profile.id,
                department = "CS",
                school = "Viterbi",
                birthDate = LocalDate.now().minusYears(20),
                bio = "Searchable user"
            )
            profile.id
        }
    }

    private fun uniqueStudentId(): String {
        val millis = System.currentTimeMillis().toString()
        return millis.takeLast(10).padStart(10, '0')
    }

    private fun <T> withTestUser(block: suspend (Long) -> T): T = runBlocking {
        withContext(Dispatchers.IO) {
            val user = dataRule.repository.getUserByEmail(TEST_EMAIL)
                ?: error("Test user not found")
            block(user.id)
        }
    }

    private fun uniqueTitle(prefix: String): String = "$prefix ${System.currentTimeMillis()}"

    companion object {
        private const val TEST_EMAIL = "tester@usc.edu"
        private const val TEST_PASSWORD = "Password1!"
    }
}