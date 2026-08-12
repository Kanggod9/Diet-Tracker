package io.github.kanggod9.diettracker

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
        compose.onNodeWithText(uniqueName).assertExists()
    }

    @Test fun logsAndTargetReplaceHistory() {
        compose.onNodeWithText("Logs").assertExists()
        compose.onNodeWithText("History").assertDoesNotExist()
        compose.onNodeWithText("Target").performClick()
        compose.onAllNodesWithText("Singapore")[0].assertExists()
    }

    @Test fun dateExpandsToMonthlyCalendar() {
        val today = java.time.LocalDate.now()
        compose.onNodeWithText(today.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))).performClick()
        compose.onNodeWithText(today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))).assertExists()
    }

    @Test fun settingsExposeAutoWrite() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Private AI and USDA gateway").assertExists()
        compose.onNodeWithText("Health Connect").assertExists()
        compose.onNodeWithText("Auto Write").assertExists()
    }
}
