package com.example.csci310team23

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthProfileTest {

    private val dataRule = TestDataRule(TEST_EMAIL, TEST_PASSWORD)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: TestRule = RuleChain.outerRule(dataRule).around(composeRule)

    @Test
    fun register_validUser_navigatesToProfileSetup() {
        waitForNode("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val email = "new${System.currentTimeMillis()}@usc.edu"
        val sid = (1000000000..9999999999).random().toString()

        composeRule.onNodeWithText("Full Name").performTextReplacement("New Tester")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(email)
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement(sid)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(timeoutMillis = 15000) {
            nodeExists(hasText("Department")) ||
                    nodeExists(hasText("Complete Your Profile"))
        }
    }

    @Test
    fun register_missingFields_showsError() {
        composeRule.onNodeWithTag("auth_register_tab").performClick()
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("")
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(4000) {
            nodeExists(hasText("registration fields", substring = true))
        }
    }

    @Test
    fun register_passwordMismatch_showsError() {
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val sid = (1000000000..9999999999).random().toString()

        composeRule.onNodeWithText("Full Name").performTextReplacement("Mismatch User")
        composeRule.onNodeWithTag("auth_email_field")
            .performTextReplacement("pw${System.currentTimeMillis()}@usc.edu")
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement(sid)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("WrongPassword!")
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(4000) {
            nodeExists(hasText("Passwords do not match"))
        }
    }

    @Test
    fun register_invalidStudentId_showsError() {
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val email = "sid${System.currentTimeMillis()}@usc.edu"

        composeRule.onNodeWithText("Full Name").performTextReplacement("Test User")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(email)
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement("123")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(4000) {
            nodeExists(hasText("valid values", substring = true))
        }
    }

    @Test
    fun login_validCredentials_showsMainScreen() {
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Profile"))
        }
    }

    @Test
    fun login_caseInsensitive_works() {
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL.uppercase())
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Feed"))
        }
    }

    @Test
    fun profile_editBio_savesSuccessfully() {
        loginAndGoToProfile()
        composeRule.onNodeWithTag("profile_edit_toggle").performClick()

        waitForNode("edit_profile_bio_field")
        val newBio = "Updated ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("edit_profile_bio_field").performTextReplacement(newBio)
        composeRule.onNodeWithTag("edit_profile_save_button").performClick()

        composeRule.waitUntil(4000) {
            nodeExists(hasText("Profile updated"))
        }
    }

    @Test
    fun profile_emptyBio_savesSuccessfully() {
        loginAndGoToProfile()
        composeRule.onNodeWithTag("profile_edit_toggle").performClick()

        waitForNode("edit_profile_bio_field")
        composeRule.onNodeWithTag("edit_profile_bio_field").performTextClearance()
        composeRule.onNodeWithTag("edit_profile_save_button").performClick()

        composeRule.waitUntil(4000) {
            nodeExists(hasText("Profile updated"))
        }
    }

    @Test
    fun profile_toggleCommentTitles_works() {
        loginAndGoToProfile()

        composeRule.onNodeWithTag("profile_display_toggle").performClick()
        waitForNode("profile_display_show_comment_titles")

        composeRule.onNodeWithTag("profile_display_show_comment_titles").performClick()

        assertTrue(
            composeRule.onAllNodesWithText("Show Comment Titles")
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun logout_returnsToAuthScreen() {
        loginAndGoToProfile()
        composeRule.onNodeWithText("Log Out").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("USC Email"))
        }
    }

    private fun waitForNode(tag: String) {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun nodeExists(matcher: SemanticsMatcher, useUnmerged: Boolean = true): Boolean =
        runCatching {
            composeRule.onNode(matcher, useUnmerged).fetchSemanticsNode()
        }.isSuccess

    private fun loginAndGoToProfile() {
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Profile"))
        }
        composeRule.onNodeWithTag("nav_profile").performClick()
    }

    companion object {
        private const val TEST_EMAIL = "tester@usc.edu"
        private const val TEST_PASSWORD = "Password1!"
    }
}
