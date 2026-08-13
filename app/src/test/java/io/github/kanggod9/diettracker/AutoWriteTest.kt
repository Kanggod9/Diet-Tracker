package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.integration.HealthConnectGateway
import io.github.kanggod9.diettracker.integration.HealthNutritionMapper
import io.github.kanggod9.diettracker.integration.HealthWriteResult
import io.github.kanggod9.diettracker.integration.replaceHealthEntry
import io.github.kanggod9.diettracker.ui.shouldAutoUpdateHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class AutoWriteTest {
    private fun entry(
        id: String,
        nutrients: Nutrients = Nutrients(),
        kind: EntryKind = EntryKind.FOOD,
    ) = JournalEntry(
        id = id,
        name = "Oats",
        kind = kind,
        mealType = MealType.BREAKFAST,
        servingDescription = "1 serving",
        servingGrams = null,
        nutrients = nutrients,
    )

    @Test fun localLogsAutoUpdateButHealthImportsAreNotWrittenBack() {
        assertTrue(shouldAutoUpdateHealth(entry("local-entry")))
        assertFalse(shouldAutoUpdateHealth(entry("health-nutrition-platform-id")))
        assertFalse(shouldAutoUpdateHealth(entry("health-hydration-platform-id")))
    }

    @Test fun stableClientIdsAreUsedByNutritionAndHydrationRecords() {
        val local = entry(
            "abc",
            Nutrients(
                mapOf(
                    NutrientKey.PROTEIN to 12.0,
                    NutrientKey.WATER to 250.0,
                ),
            ),
        )
        val nutrition = HealthNutritionMapper.toNutritionRecord(local, 7L, ZoneOffset.UTC)
        val hydration = requireNotNull(HealthNutritionMapper.toHydrationRecord(local, 7L, ZoneOffset.UTC))

        assertEquals("abc", HealthConnectGateway.nutritionClientRecordId("abc"))
        assertEquals("abc-hydration", HealthConnectGateway.hydrationClientRecordId("abc"))
        assertEquals("abc", nutrition.metadata.clientRecordId)
        assertEquals("abc-hydration", hydration.metadata.clientRecordId)
        assertEquals(7L, nutrition.metadata.clientRecordVersion)
        assertEquals(7L, hydration.metadata.clientRecordVersion)
    }

    @Test fun editReplacementDeletesOldRecordsBeforeWritingTheNewVersion() = runBlocking {
        val events = mutableListOf<String>()
        val local = entry("abc")
        val result = replaceHealthEntry(
            entry = local,
            delete = { id -> events += "delete:$id" },
            write = { updated ->
                events += "write:${updated.id}"
                HealthWriteResult(nutritionRecordId = "new-platform-id")
            },
        )

        assertEquals(listOf("delete:abc", "write:abc"), events)
        assertEquals("new-platform-id", result.nutritionRecordId)
    }

    @Test fun bothEntryWithNutritionAndWaterCreatesBothHealthRecordTypes() {
        val both = entry(
            id = "meal-and-drink",
            nutrients = Nutrients(
                mapOf(
                    NutrientKey.ENERGY to 420.0,
                    NutrientKey.PROTEIN to 18.0,
                    NutrientKey.WATER to 350.0,
                ),
            ),
            kind = EntryKind.BOTH,
        )

        val nutrition = HealthNutritionMapper.toNutritionRecord(both, 9L, ZoneOffset.UTC)
        val hydration = requireNotNull(HealthNutritionMapper.toHydrationRecord(both, 9L, ZoneOffset.UTC))

        assertEquals("meal-and-drink", nutrition.metadata.clientRecordId)
        assertEquals(420.0, nutrition.energy!!.inKilocalories, 0.0)
        assertEquals("meal-and-drink-hydration", hydration.metadata.clientRecordId)
        assertEquals(350.0, hydration.volume.inMilliliters, 0.0)
    }
}
