package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SuggestionsAndTrendsTest {
    @Test fun missingNutrientDoesNotTriggerSuggestion() {
        assertTrue(SuggestionEngine.generate(Nutrients(), GuidanceProfiles.US).isEmpty())
    }

    @Test fun upperReferenceCanTriggerNeutralSuggestion() {
        val result = SuggestionEngine.generate(Nutrients(mapOf(NutrientKey.SODIUM to 2200.0)), GuidanceProfiles.US)
        assertEquals(1, result.size)
        assertTrue(result.single().message.contains("Review later choices"))
    }

    @Test fun trendWindowsUseOnlySelectedDays() {
        val today = LocalDate.of(2026, 8, 11)
        val days = listOf(
            DailyTotals(today, NutrientAggregator.aggregate(listOf(Nutrients(mapOf(NutrientKey.ENERGY to 1000.0))))),
            DailyTotals(today.minusDays(8), NutrientAggregator.aggregate(listOf(Nutrients(mapOf(NutrientKey.ENERGY to 3000.0))))),
        )
        val result = TrendAnalyzer.summarize(days, TrendWindow.DAYS_7, today)
        assertEquals(1, result.daysWithEntries)
        assertEquals(1000.0, result.dailyAverage[NutrientKey.ENERGY]!!, 0.0)
    }
}
