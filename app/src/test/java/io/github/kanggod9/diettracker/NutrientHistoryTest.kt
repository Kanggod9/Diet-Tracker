package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.FoodScoreCalculator
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.ui.HistoryPeriod
import io.github.kanggod9.diettracker.ui.calendarFoodScores
import io.github.kanggod9.diettracker.ui.calendarDateForWeekPage
import io.github.kanggod9.diettracker.ui.calendarWeekPage
import io.github.kanggod9.diettracker.ui.calendarWeekStart
import io.github.kanggod9.diettracker.ui.dashboardNutrientOrder
import io.github.kanggod9.diettracker.ui.dashboardNutrientLabel
import io.github.kanggod9.diettracker.ui.dashboardPages
import io.github.kanggod9.diettracker.ui.foodScoreHistoryBuckets
import io.github.kanggod9.diettracker.ui.foodScorePeriodSummary
import io.github.kanggod9.diettracker.ui.mealJournalGroups
import io.github.kanggod9.diettracker.ui.nutrientHistoryBuckets
import io.github.kanggod9.diettracker.ui.nutrientProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class NutrientHistoryTest {
    private fun entry(
        id: String,
        time: String,
        energy: Double,
        mealType: MealType = MealType.LUNCH,
    ) = JournalEntry(
        id = id,
        name = "Meal $id",
        kind = EntryKind.FOOD,
        mealType = mealType,
        servingDescription = "1 serving",
        servingGrams = null,
        loggedAt = Instant.parse(time),
        nutrients = Nutrients(mapOf(NutrientKey.ENERGY to energy)),
    )

    private fun scoredEntry(id: String, time: String, fiber: Double, sodium: Double) = JournalEntry(
        id = id,
        name = "Scored $id",
        kind = EntryKind.FOOD,
        mealType = MealType.LUNCH,
        servingDescription = "1 serving",
        servingGrams = null,
        loggedAt = Instant.parse(time),
        nutrients = Nutrients(
            mapOf(
                NutrientKey.ENERGY to 100.0,
                NutrientKey.DIETARY_FIBER to fiber,
                NutrientKey.SODIUM to sodium,
            ),
        ),
    )

    @Test fun weekBucketsIncludeZerosAndDailyTotals() {
        val anchor = LocalDate.of(2026, 8, 13)
        val buckets = nutrientHistoryBuckets(
            listOf(
                entry("a", "2026-08-10T04:00:00Z", 300.0),
                entry("b", "2026-08-10T08:00:00Z", 200.0),
                entry("c", "2026-08-12T04:00:00Z", 100.0),
            ),
            NutrientKey.ENERGY,
            HistoryPeriod.WEEK,
            anchor,
            ZoneOffset.UTC,
        )
        assertEquals(7, buckets.size)
        assertEquals(500.0, buckets[0].value, 0.0)
        assertEquals(0.0, buckets[1].value, 0.0)
        assertEquals(100.0, buckets[2].value, 0.0)
    }

    @Test fun everyHistoryPeriodBuildsTheExpectedHistogramBuckets() {
        val entries = listOf(entry("a", "2026-08-13T04:00:00Z", 300.0))
        val anchor = LocalDate.of(2026, 8, 13)
        val expected = mapOf(
            HistoryPeriod.DAY to 24,
            HistoryPeriod.WEEK to 7,
            HistoryPeriod.MONTH to 31,
            HistoryPeriod.THREE_MONTHS to 3,
            HistoryPeriod.YEAR to 12,
        )
        expected.forEach { (period, size) ->
            assertEquals(
                period.name,
                size,
                nutrientHistoryBuckets(entries, NutrientKey.ENERGY, period, anchor, ZoneOffset.UTC).size,
            )
        }
    }

    @Test fun dashboardPagesMatchTheThreeThenSixContractAndKeepEveryNutrient() {
        val pages = dashboardPages()
        assertEquals(3, pages.first().size)
        assertTrue(pages.drop(1).dropLast(1).all { it.size == 6 })
        assertTrue(pages.last().size in 1..6)
        assertEquals(dashboardNutrientOrder, pages.flatten())
        assertEquals(NutrientKey.entries.size, pages.flatten().size)
        assertEquals(NutrientKey.entries.toSet(), pages.flatten().toSet())
        assertEquals(
            listOf(NutrientKey.ENERGY, NutrientKey.PROTEIN, NutrientKey.TOTAL_CARBOHYDRATE),
            pages.first(),
        )
    }

    @Test fun dashboardProgressClampsAtTargetAndOnlyExceedingChangesState() {
        val halfway = nutrientProgressState(50.0, 100.0)
        assertEquals(0.5f, halfway.fill, 0.0f)
        assertFalse(halfway.exceeded)

        val atTarget = nutrientProgressState(100.0, 100.0)
        assertEquals(1.0f, atTarget.fill, 0.0f)
        assertFalse(atTarget.exceeded)

        val exceeded = nutrientProgressState(101.0, 100.0)
        assertEquals(1.0f, exceeded.fill, 0.0f)
        assertTrue(exceeded.exceeded)
    }

    @Test fun journalGroupsUseExistingMealCategoriesAndModelOrder() {
        val groups = mealJournalGroups(
            listOf(
                entry("lunch", "2026-08-13T04:00:00Z", 300.0, MealType.LUNCH),
                entry("breakfast", "2026-08-13T00:00:00Z", 200.0, MealType.BREAKFAST),
                entry("snack", "2026-08-13T08:00:00Z", 100.0, MealType.SNACK),
            ),
        )
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK),
            groups.map { it.first },
        )
        assertEquals(listOf("breakfast"), groups[0].second.map { it.id })
        assertEquals(listOf("lunch"), groups[1].second.map { it.id })
    }

    @Test fun foodScoreDailyLogBucketsAndLongerPeriodAveragesUseDailyScores() {
        val anchor = LocalDate.of(2026, 8, 13)
        val first = scoredEntry("a", "2026-08-13T04:00:00Z", 3.0, 100.0)
        val second = scoredEntry("b", "2026-08-13T08:00:00Z", 1.0, 300.0)
        val nextDay = scoredEntry("c", "2026-08-14T04:00:00Z", 2.0, 150.0)
        val entries = listOf(first, second, nextDay)

        val dailyLogBuckets = foodScoreHistoryBuckets(entries, HistoryPeriod.DAY, anchor, ZoneOffset.UTC)
        assertEquals(2, dailyLogBuckets.size)
        assertTrue(dailyLogBuckets.all { it.scoreCount == 1 && it.average != null })

        val expectedDaily = FoodScoreCalculator.calculate(
            Nutrients.totals(listOf(first.nutrients, second.nutrients)),
        ).score!!
        val expectedNextDay = FoodScoreCalculator.calculate(nextDay.nutrients).score!!
        val daySummary = foodScorePeriodSummary(entries, HistoryPeriod.DAY, anchor, ZoneOffset.UTC)
        assertEquals(1, daySummary.scoreCount)
        assertEquals(expectedDaily.toDouble(), daySummary.average!!, 0.0)

        val calendar = calendarFoodScores(entries, ZoneOffset.UTC)
        assertEquals(expectedDaily, calendar[anchor])
        assertEquals(expectedNextDay, calendar[anchor.plusDays(1)])

        val week = foodScoreHistoryBuckets(entries, HistoryPeriod.WEEK, anchor, ZoneOffset.UTC)
        assertEquals(expectedDaily.toDouble(), week[3].average!!, 0.0)
        assertEquals(expectedNextDay.toDouble(), week[4].average!!, 0.0)

        val threeMonths = foodScoreHistoryBuckets(entries, HistoryPeriod.THREE_MONTHS, anchor, ZoneOffset.UTC)
        val august = threeMonths.last()
        assertEquals(2, august.scoreCount)
        assertEquals(listOf(expectedDaily, expectedNextDay).average(), august.average!!, 0.0)
        val period = foodScorePeriodSummary(entries, HistoryPeriod.THREE_MONTHS, anchor, ZoneOffset.UTC)
        assertEquals(2, period.scoreCount)
        assertEquals(august.average!!, period.average!!, 0.0)
    }

    @Test fun fatNamesAreFullExceptOnCompactDashboardTiles() {
        assertEquals("Monounsaturated fat", NutrientKey.MONOUNSATURATED_FAT.label)
        assertEquals("Polyunsaturated fat", NutrientKey.POLYUNSATURATED_FAT.label)
        assertEquals("Unsaturated fat", NutrientKey.UNSATURATED_FAT.label)
        assertEquals("MUFA", dashboardNutrientLabel(NutrientKey.MONOUNSATURATED_FAT))
        assertEquals("PUFA", dashboardNutrientLabel(NutrientKey.POLYUNSATURATED_FAT))
        assertEquals("Unsat. fat", dashboardNutrientLabel(NutrientKey.UNSATURATED_FAT))
        assertEquals("Calcium", dashboardNutrientLabel(NutrientKey.CALCIUM))
    }

    @Test fun onlyDashboardUsesTheNewLongNameAbbreviations() {
        assertEquals("Energy from fat", NutrientKey.ENERGY_FROM_FAT.label)
        assertEquals("Pantothenic acid", NutrientKey.PANTOTHENIC_ACID.label)
        assertEquals("Fat kcal", dashboardNutrientLabel(NutrientKey.ENERGY_FROM_FAT))
        assertEquals("Vitamin B5", dashboardNutrientLabel(NutrientKey.PANTOTHENIC_ACID))
    }

    @Test fun weekPagerUsesCurrentWeekAsTheLastPageAndPreservesWeekday() {
        val today = LocalDate.of(2026, 8, 20) // Thursday
        assertEquals(LocalDate.of(2026, 8, 17), calendarWeekStart(today))
        val currentPage = calendarWeekPage(today, today)
        assertEquals(currentPage - 1, calendarWeekPage(today.minusWeeks(1), today))
        assertEquals(currentPage, calendarWeekPage(today.plusWeeks(1), today))
        assertEquals(
            LocalDate.of(2026, 8, 13),
            calendarDateForWeekPage(currentPage - 1, today, today),
        )
        assertEquals(today, calendarDateForWeekPage(currentPage, today.minusWeeks(1), today))
    }
}
