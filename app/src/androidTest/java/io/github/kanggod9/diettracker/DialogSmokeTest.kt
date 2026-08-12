package io.github.kanggod9.diettracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.ui.EntryEditorDialog
import io.github.kanggod9.diettracker.ui.PhotoConsentDialog
import io.github.kanggod9.diettracker.ui.ReviewSeed
import io.github.kanggod9.diettracker.ui.PhotoSourceDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
}
