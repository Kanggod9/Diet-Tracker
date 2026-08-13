package io.github.kanggod9.diettracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.integration.UsdaDataType
import io.github.kanggod9.diettracker.integration.UsdaFood
import io.github.kanggod9.diettracker.integration.UsdaFoodDataSource
import io.github.kanggod9.diettracker.ui.EntryEditorDialog
import io.github.kanggod9.diettracker.ui.FoodScoreHistoryScreen
import io.github.kanggod9.diettracker.ui.LogChooserDialog
import io.github.kanggod9.diettracker.ui.PhotoConsentDialog
import io.github.kanggod9.diettracker.ui.PhotoSourceDialog
import io.github.kanggod9.diettracker.ui.ReviewSeed
import io.github.kanggod9.diettracker.ui.UsdaLookupRequest
import io.github.kanggod9.diettracker.ui.UsdaSearchDialog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DialogSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun photoSourceOffersCameraAndAlbum() {
        compose.setContent {
            MaterialTheme {
                PhotoSourceDialog(onDismiss = {}, onCamera = {}, onAlbum = {})
            }
        }
        compose.onNodeWithText("Camera").assertExists()
        compose.onNodeWithText("Album").assertExists()
    }

    @Test fun usdaSearchBackIsSeparateFromCancel() {
        var visible by mutableStateOf(true)
        var backed = false
        var dismissed = false
        val dataSource = object : UsdaFoodDataSource {
            override suspend fun search(
                query: String,
                allowedTypes: Set<UsdaDataType>,
            ): List<UsdaFood> = emptyList()

            override suspend fun food(fdcId: Long): UsdaFood? = null
        }
        compose.setContent {
            MaterialTheme {
                if (visible) {
                    UsdaSearchDialog(
                        request = UsdaLookupRequest("water"),
                        dataSource = dataSource,
                        onBack = { backed = true; visible = false },
                        onDismiss = { dismissed = true; visible = false },
                        onReview = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("USDA FoodData Central").assertDoesNotExist()
        assertTrue(backed)
        assertFalse(dismissed)
    }

    @Test fun detailedManualOffersFoodDrinkAndBoth() {
        compose.setContent {
            MaterialTheme {
                EntryEditorDialog(
                    seed = ReviewSeed(null, "Detailed manual"),
                    nutrientTargets = emptyMap(),
                    onDismiss = {},
                    onSave = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Food").assertExists()
        compose.onNodeWithText("Drink").assertExists()
        compose.onNodeWithText("Both").assertExists()
    }

    @Test fun aiEstimateOffersFoodDrinkAndBoth() {
        val estimate = JournalEntry(
            name = "AI estimate",
            kind = EntryKind.FOOD,
            mealType = MealType.UNKNOWN,
            servingDescription = "1 serving",
            servingGrams = null,
            nutrients = Nutrients(),
        )
        compose.setContent {
            MaterialTheme {
                EntryEditorDialog(
                    seed = ReviewSeed(estimate, "Review AI photo estimate"),
                    nutrientTargets = emptyMap(),
                    onDismiss = {},
                    onSave = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Food").assertExists()
        compose.onNodeWithText("Drink").assertExists()
        compose.onNodeWithText("Both").assertExists()
    }

    @Test fun nutrientProgressValueOpensEditor() {
        compose.setContent {
            MaterialTheme {
                EntryEditorDialog(
                    seed = ReviewSeed(null, "Detailed manual"),
                    nutrientTargets = mapOf(NutrientKey.ENERGY to 2_000.0),
                    onDismiss = {},
                    onSave = { _, _ -> },
                )
            }
        }
        compose.onAllNodesWithText("--")[0].performClick()
        compose.onNodeWithText("kcal").assertExists()
    }

    @Test fun consentClosesAndReturnsDontShowChoice() {
        var visible by mutableStateOf(true)
        var accepted = false
        compose.setContent {
            MaterialTheme {
                if (visible) PhotoConsentDialog(
                    endpoint = "https://gateway.example",
                    onDismiss = { visible = false },
                    onAnalyze = {
                        accepted = it
                        visible = false
                    },
                )
            }
        }
        compose.onNodeWithText("Don't show next time").performClick()
        compose.onNodeWithText("I consent").performClick()
        compose.onNodeWithText("I consent").assertDoesNotExist()
        assertTrue(accepted)
    }
    @Test fun foodScoreHistoryShowsPerLogNutrientContributionsAndPeriodAverage() {
        val date = LocalDate.now()
        val scored = JournalEntry(
            name = "Scored meal",
            kind = EntryKind.BOTH,
            mealType = MealType.LUNCH,
            servingDescription = "1 serving",
            servingGrams = null,
            loggedAt = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            nutrients = Nutrients(
                mapOf(
                    NutrientKey.ENERGY to 100.0,
                    NutrientKey.DIETARY_FIBER to 3.0,
                    NutrientKey.SODIUM to 135.0,
                    NutrientKey.WATER to 250.0,
                ),
            ),
        )
        compose.setContent {
            MaterialTheme {
                FoodScoreHistoryScreen(listOf(scored), date, onBack = {})
            }
        }

        compose.onNodeWithText("W").performClick()
        compose.onNodeWithText("Average from 1 scored day").assertExists()
        compose.onNodeWithText("D").performClick()
        compose.onNodeWithText("Log nutrient contributions").performScrollTo().assertExists()
        compose.onNodeWithText("Scored meal").performScrollTo().assertExists()
        compose.onNodeWithText("Adds points").performScrollTo().assertExists()
        compose.onNodeWithText("Deducts points").performScrollTo().assertExists()
    }
    @Test fun usdaBackReturnsToTheLogChooserLevel() {
        var level by mutableStateOf("chooser")
        val dataSource = object : UsdaFoodDataSource {
            override suspend fun search(
                query: String,
                allowedTypes: Set<UsdaDataType>,
            ): List<UsdaFood> = emptyList()

            override suspend fun food(fdcId: Long): UsdaFood? = null
        }
        compose.setContent {
            MaterialTheme {
                when (level) {
                    "chooser" -> LogChooserDialog(
                        quickFoods = emptyList(),
                        onlineConfigured = true,
                        onDismiss = { level = "closed" },
                        onDetailedManual = {},
                        onTextParsed = {},
                        onUsdaSearch = { level = "usda" },
                        onPhoto = {},
                        onQuickFood = {},
                    )
                    "usda" -> UsdaSearchDialog(
                        request = UsdaLookupRequest("water"),
                        dataSource = dataSource,
                        onBack = { level = "chooser" },
                        onDismiss = { level = "closed" },
                        onReview = {},
                    )
                }
            }
        }

        compose.onNodeWithText("USDA search").performClick()
        compose.onNodeWithText("USDA FoodData Central").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Log food or drink").assertExists()
        assertTrue(level == "chooser")
    }
}