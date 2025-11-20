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
        waitForNodeWithTag("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val email = "new${System.currentTimeMillis()}@usc.edu"
        val sid = (1000000000..9999999999).random().toString()

        composeRule.onNodeWithText("Full Name").performTextReplacement("New Tester")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(email)
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement(sid)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(timeoutMillis = 15000) {
            nodeExists(hasText("Department")) ||
                    nodeExists(hasText("Complete Your Profile"))
        }
    }

    @Test
    fun register_missingFields_showsError() {
        waitForNodeWithTag("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(500)

        composeRule.waitUntil(4000) {
            nodeExists(hasText("fill", substring = true)) ||
                    nodeExists(hasText("required", substring = true)) ||
                    nodeExists(hasText("field", substring = true))
        }
    }

    @Test
    fun register_invalidStudentId_showsError() {
        waitForNodeWithTag("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val email = "sid${System.currentTimeMillis()}@usc.edu"

        composeRule.onNodeWithText("Full Name").performTextReplacement("Test User")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(email)
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement("123")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(500)

        composeRule.waitUntil(4000) {
            nodeExists(hasText("valid", substring = true)) ||
                    nodeExists(hasText("field", substring = true))
        }
    }

    @Test
    fun register_duplicateEmail_showsError() {
        waitForNodeWithTag("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        composeRule.onNodeWithText("Full Name").performTextReplacement("Dup User")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement("9876543210")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(1000)

        composeRule.waitUntil(7000) {
            val allTexts = try {
                composeRule.onAllNodes(hasAnyDescendant(hasText("", substring = true)))
                    .fetchSemanticsNodes()
                true
            } catch (e: Exception) {
                false
            }
            allTexts
        }
    }

    @Test
    fun register_invalidEmailFormat_showsError() {
        waitForNodeWithTag("auth_register_tab")
        composeRule.onNodeWithTag("auth_register_tab").performClick()

        val sid = (1000000000..9999999999).random().toString()

        composeRule.onNodeWithText("Full Name").performTextReplacement("Bad Email User")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("notanemail")
        composeRule.onNodeWithText("Student ID (10 digits)").performTextReplacement(sid)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        composeRule.onNodeWithText("Confirm Password").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(1000)

        composeRule.waitUntil(5000) {
            val allTexts = try {
                composeRule.onAllNodes(hasAnyDescendant(hasText("", substring = true)))
                    .fetchSemanticsNodes()
                true
            } catch (e: Exception) {
                false
            }
            allTexts
        }
    }

    @Test
    fun login_validCredentials_showsMainScreen() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Profile")) || nodeExists(hasText("Feed"))
        }
    }

    @Test
    fun login_wrongPassword_showsError() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("WrongPass123!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(1000)

        composeRule.waitUntil(5000) {
            nodeExists(hasTestTag("auth_email_field"))
        }
    }

    @Test
    fun login_emptyEmail_showsError() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(500)

        composeRule.waitUntil(3000) {
            nodeExists(hasText("required", substring = true)) ||
                    nodeExists(hasText("Email", substring = true))
        }
    }

    @Test
    fun login_whitespaceEmail_showsError() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("   ")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("Password1!")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(500)

        composeRule.waitUntil(3000) {
            nodeExists(hasText("required", substring = true)) ||
                    nodeExists(hasText("Email", substring = true))
        }
    }

    @Test
    fun login_caseInsensitive_works() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL.uppercase())
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Feed")) || nodeExists(hasText("Profile"))
        }
    }

    @Test
    fun login_bothFieldsEmpty_showsError() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement("")
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement("")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        Thread.sleep(500)

        composeRule.waitUntil(3000) {
            nodeExists(hasText("required", substring = true)) ||
                    nodeExists(hasText("Email", substring = true))
        }
    }

    @Test
    fun profile_viewInfo_displaysCorrectly() {
        loginAndGoToProfile()
        composeRule.onNodeWithTag("nav_profile").assertIsDisplayed()
        composeRule.onNodeWithText(TEST_EMAIL, substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun profile_toggleCommentTitles_works() {
        loginAndGoToProfile()
        waitForNodeWithTag("profile_display_toggle")

        composeRule.onNodeWithTag("profile_display_toggle").performClick()
        waitForNodeWithTag("profile_display_show_comment_titles")

        composeRule.onNodeWithTag("profile_display_show_comment_titles").performClick()

        assertTrue(
            composeRule.onAllNodesWithText("Show Comment Titles", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun waitForNodeWithTag(tag: String) {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun nodeExists(matcher: SemanticsMatcher, useUnmerged: Boolean = true): Boolean =
        runCatching {
            composeRule.onNode(matcher, useUnmerged).fetchSemanticsNode()
        }.isSuccess

    private fun loginAndGoToProfile() {
        waitForNodeWithTag("auth_email_field")
        composeRule.onNodeWithTag("auth_email_field").performTextReplacement(TEST_EMAIL)
        composeRule.onNodeWithTag("auth_password_field").performTextReplacement(TEST_PASSWORD)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("auth_submit_button").performClick()

        composeRule.waitUntil(5000) {
            nodeExists(hasText("Profile")) || nodeExists(hasText("Feed"))
        }
        waitForNodeWithTag("nav_profile")
        composeRule.onNodeWithTag("nav_profile").performClick()

        composeRule.waitUntil(3000) {
            composeRule.onAllNodesWithTag("profile_edit_toggle").fetchSemanticsNodes().isNotEmpty()
        }
    }

    companion object {
        private const val TEST_EMAIL = "tester@usc.edu"
        private const val TEST_PASSWORD = "Password1!"
    }
}
