package io.github.kanggod9.diettracker

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test

class DietTrackerSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test fun manualLogRequiresReviewBeforeItAppearsInCategorizedJournal() {
        val uniqueName = "Smoke oats"
        compose.onNodeWithText("+ Log").performClick()
        compose.onNodeWithText("Detailed manual").performClick()
        compose.onNodeWithText("Name").performTextInput(uniqueName)
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithText("Review before saving").assertExists()
        compose.onNodeWithText("Confirm and save").performClick()
        compose.onNodeWithContentDescription("Expand").performClick()
        compose.onNodeWithText(uniqueName).assertExists()
    }

    @Test fun dashboardRendersThreeNutrientsThenSwipesToTheNextSix() {
        listOf("ENERGY", "PROTEIN", "TOTAL_CARBOHYDRATE").forEach { key ->
            compose.onNodeWithContentDescription("Nutrient tile $key").assertIsDisplayed()
        }

        compose.onNodeWithContentDescription("Daily nutrient dashboard")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()

        listOf(
            "TOTAL_FAT",
            "SATURATED_FAT",
            "DIETARY_FIBER",
            "TOTAL_SUGAR",
            "ADDED_SUGAR",
            "SODIUM",
        ).forEach { key ->
            compose.onNodeWithContentDescription("Nutrient tile $key").assertIsDisplayed()
        }
    }

    @Test fun analysisIsRemovedAndLogsTileOpensNutrientHistory() {
        compose.onNodeWithText("Logs").assertExists()
        compose.onNodeWithText("Analysis").assertDoesNotExist()
        compose.onNodeWithContentDescription("Nutrient tile ENERGY").performClick()
        compose.onNodeWithText("Entries").assertExists()
        compose.onNodeWithText("D").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("+ Log").assertExists()
    }

    @Test fun targetUsesCardsWithoutProgressBarsAndOpensTheSameHistory() {
        compose.onNodeWithText("Target").performClick()
val progressBar = SemanticsMatcher("has progress bar") { node ->
            node.config.contains(SemanticsProperties.ProgressBarRangeInfo)
        }
        compose.onAllNodes(progressBar).assertCountEquals(0)
        compose.onNodeWithContentDescription("Target card ENERGY").performClick()
        compose.onNodeWithText("Entries").assertExists()
        compose.onNodeWithText("W").assertExists()
    }

    @Test fun dateExpandsToMonthlyCalendar() {
        val today = java.time.LocalDate.now()
        compose.onNodeWithText(today.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))).performClick()
        compose.onNodeWithText(today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))).assertExists()
    }

    @Test fun settingsUseBuiltInHealthUpdatesWithoutToggleOrDescription() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Private AI and USDA gateway").assertExists()
        compose.onNodeWithText("Health Connect").assertExists()
        compose.onNodeWithText("Auto Write").assertDoesNotExist()
        compose.onNodeWithText("Auto Update").assertDoesNotExist()
    }
    @Test fun foodScoreTileOpensDedicatedHistoryAndReturnsToLogs() {
        compose.onNodeWithContentDescription("Food score tile").performClick()
        compose.onNodeWithText("Food Score").assertIsDisplayed()
        compose.onNodeWithText("Log nutrient contributions").assertExists()
        listOf("D", "W", "M", "3M", "Y").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("+ Log").assertIsDisplayed()
    }

    @Test fun compactFatNamesRenderOnTheirDashboardPages() {
        repeat(2) {
            compose.onNodeWithContentDescription("Daily nutrient dashboard")
                .performTouchInput { swipeLeft() }
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Nutrient tile MONOUNSATURATED_FAT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Nutrient tile POLYUNSATURATED_FAT").assertIsDisplayed()
        compose.onNodeWithText("MUFA").assertIsDisplayed()
        compose.onNodeWithText("PUFA").assertIsDisplayed()

        compose.onNodeWithContentDescription("Daily nutrient dashboard")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Nutrient tile UNSATURATED_FAT").assertIsDisplayed()
        compose.onNodeWithText("Unsat. fat").assertIsDisplayed()
    }
}
