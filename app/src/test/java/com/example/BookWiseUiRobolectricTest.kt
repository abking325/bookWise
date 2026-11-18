package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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

    // Carousel chevron steppers work
    composeTestRule.onNodeWithTag("carousel_next_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("carousel_prev_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Navigate to Full Books and verify Grid / List view toggle
    composeTestRule.onNodeWithTag("nav_full_books").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("library_grid_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("library_view_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_list_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("library_view_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_grid_view").assertIsDisplayed()

    // Navigate to Categories shelf
    composeTestRule.onNodeWithTag("nav_categories").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("create_category_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("create_category_button").assertIsDisplayed()

    // Toggle Light/Dark mode quickly from header
    composeTestRule.onNodeWithTag("quick_theme_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_theme_toggle").performClick()
    composeTestRule.waitForIdle()

    // Navigate to Settings and verify theme/navigation/language controls
    composeTestRule.onNodeWithTag("nav_settings_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_style_floating_bottom").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_style_top_bar").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("nav_add_book").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Navigate to Add Book (4-step editor) via Top Navigation
    composeTestRule.onNodeWithTag("nav_add_book").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("editor_title_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("editor_title_input").performTextInput("Quiet Craft")
    composeTestRule.onNodeWithTag("editor_step_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("editor_paste_text_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("paste_book_dialog_input").performTextInput(
      "Chapter 1: Clarity\nOpening paragraph of calm discipline.\n\nChapter 2: Steady Craft\nSecond chapter paragraph."
    )
    composeTestRule.onNodeWithTag("paste_book_detect_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("editor_add_chapter_button").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify dedicated Add Introduction and Table of Contents controls in Structure step
    composeTestRule.onNodeWithTag("editor_add_intro_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("editor_add_toc_button").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("editor_step_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("editor_finish_save_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("editor_save_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("editor_post_save_done_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("editor_post_save_done_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("details_read_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Navigate to Reader directly from Details and verify Floating TTS player stays hidden
    composeTestRule.onNodeWithTag("details_read_button").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_screen").fetchSemanticsNodes().isNotEmpty()
    }
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

    // Go back to Home and verify carousel Read button works too
    composeTestRule.onNodeWithTag("reader_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").performTouchInput {
      swipeUp(startY = 400f, endY = 200f)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("carousel_read_button").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_paragraph_0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("reader_screen").assertIsDisplayed()
  }

  @Test
  fun `reader table of contents typography sheet and reset progress work smoothly`() {
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("home_screen").performTouchInput {
      swipeUp(startY = 400f, endY = 200f)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("carousel_read_button").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_paragraph_0").fetchSemanticsNodes().isNotEmpty()
    }

    // Open Typography & Paper bottom sheet
    composeTestRule.onNodeWithTag("reader_typography_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_screen").fetchSemanticsNodes().isNotEmpty() &&
        runCatching { composeTestRule.onNodeWithText("Typography & Paper").assertIsDisplayed(); true }.getOrDefault(false)
    }
    composeTestRule.onNodeWithText("Sepia").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()

    // Open Table of Contents sheet and verify Restart Book action
    composeTestRule.onNodeWithTag("reader_toc_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("reader_restart_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("reader_restart_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("reader_screen").assertIsDisplayed()
  }
}
