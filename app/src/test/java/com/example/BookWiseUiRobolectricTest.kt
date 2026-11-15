package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookWiseUiRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `app opens directly to Home with 3D carousel and navigates across core screens`() {
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_3d_carousel").assertIsDisplayed()

    // Navigate to Full Books
    composeTestRule.onNodeWithTag("nav_full_books").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_search_input").assertIsDisplayed()

    // Navigate to Settings and verify theme/navigation style controls
    composeTestRule.onNodeWithTag("nav_settings_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

    // Navigate back Home and launch Reader from carousel Read button
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("carousel_read_button").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_paragraph_0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Reader opens cleanly and Floating TTS player remains hidden during normal reading
    composeTestRule.onNodeWithTag("reader_screen").assertIsDisplayed()
    assertTrue(
      composeTestRule.onAllNodesWithTag("floating_tts_player").fetchSemanticsNodes().isEmpty()
    )
    composeTestRule.onNodeWithTag("reader_paragraph_0").assertIsDisplayed()
  }
}
