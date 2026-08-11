package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.data.LocalSnapshot
import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.EntryAmount
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.ui.ExportJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExportJsonTest {
    @Test fun exportKeepsExplicitZeroAndMissingDistinctAndExcludesGatewayCredentials() {
        val entry = JournalEntry(
            name = "Coffee",
            kind = EntryKind.DRINK,
            mealType = MealType.BREAKFAST,
            servingDescription = "250 mL",
            servingGrams = null,
            amount = EntryAmount(250.0, AmountUnit.MILLILITRE),
            nutrients = Nutrients(mapOf(NutrientKey.CAFFEINE to 0.0)),
        )
        val encoded = ExportJson.encode(LocalSnapshot(listOf(entry), emptyList(), mapOf("guidance_region" to "SINGAPORE")))
        val root = Json.parseToJsonElement(encoded).jsonObject
        val nutrients = root["entries"]!!.jsonArray.single().jsonObject["nutrients"]!!.jsonObject

        assertEquals(0.0, nutrients["CAFFEINE"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertNull(nutrients["WATER"])
        assertFalse(encoded.contains("APP_ACCESS_TOKEN"))
        assertFalse(encoded.contains("OPENAI_API_KEY"))
    }
}