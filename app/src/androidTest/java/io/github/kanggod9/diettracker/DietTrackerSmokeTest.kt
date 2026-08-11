package io.github.kanggod9.diettracker

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class DietTrackerSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test fun manualLogRequiresReviewBeforeItAppearsInJournal() {
        val uniqueName = "Smoke oats"
        compose.onNodeWithText("Text / USDA", substring = true).performClick()
        compose.onNodeWithText("Detailed manual").performClick()
        compose.onNodeWithText("Name").performTextInput(uniqueName)
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithText("Review before saving").assertExists()
        compose.onNodeWithText("Confirm and save").performClick()
        compose.onNodeWithText(uniqueName).assertExists()
    }

    @Test fun settingsExposePrivacyAndExplicitSyncControls() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Private AI and USDA gateway").assertExists()
        val settingsList = compose.onNode(hasScrollToIndexAction())
        settingsList.performScrollToIndex(3)
        compose.onNodeWithText("Health Connect").assertExists()
        settingsList.performScrollToIndex(4)
        compose.onNodeWithText("Your local data").assertExists()
        settingsList.performScrollToIndex(5)
        compose.onNodeWithText("Privacy boundaries").assertExists()
    }
}