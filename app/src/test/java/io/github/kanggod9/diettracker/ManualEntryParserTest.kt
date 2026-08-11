package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.ManualEntryParser
import io.github.kanggod9.diettracker.domain.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ManualEntryParserTest {
    private val breakfastClock = Clock.fixed(Instant.parse("2026-08-11T00:15:00Z"), ZoneOffset.ofHours(8))

    @Test fun parsesEnglishMealAndMass() {
        val parsed = requireNotNull(ManualEntryParser.parse("lunch rice 250 g", breakfastClock))
        assertEquals("rice", parsed.name)
        assertEquals(MealType.LUNCH, parsed.mealType)
        assertEquals(EntryKind.FOOD, parsed.kind)
        assertEquals(AmountUnit.GRAM, parsed.amount?.unit)
        assertEquals(250.0, parsed.amount?.value ?: 0.0, 0.001)
    }

    @Test fun parsesChineseMealAndKilogramsWithoutSourceEncoding() {
        val input = "\u5348\u9910 \u7C73\u996D 0.25 \u516C\u65A4"
        val parsed = requireNotNull(ManualEntryParser.parse(input, breakfastClock))
        assertEquals("\u7C73\u996D", parsed.name)
        assertEquals(MealType.LUNCH, parsed.mealType)
        assertEquals(250.0, parsed.amount?.value ?: 0.0, 0.001)
    }

    @Test fun volumeInfersDrinkAndClockInfersBreakfast() {
        val parsed = requireNotNull(ManualEntryParser.parse("coffee 350 ml", breakfastClock))
        assertEquals(EntryKind.DRINK, parsed.kind)
        assertEquals(MealType.BREAKFAST, parsed.mealType)
        assertEquals(AmountUnit.MILLILITRE, parsed.amount?.unit)
    }

    @Test fun calorieTargetRemainsDistinctFromMass() {
        val parsed = requireNotNull(ManualEntryParser.parse("dinner pasta 600 kcal", breakfastClock))
        assertEquals(MealType.DINNER, parsed.mealType)
        assertNull(parsed.amount)
        assertEquals(600.0, parsed.targetCalories ?: 0.0, 0.001)
    }

    @Test fun refusesAmbiguousTextWithoutQuantity() {
        assertNull(ManualEntryParser.parse("some rice", breakfastClock))
        assertTrue(ManualEntryParser.parse("rice 250 g", breakfastClock)!!.originalText.isNotBlank())
    }
}