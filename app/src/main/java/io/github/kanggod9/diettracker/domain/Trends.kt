package io.github.kanggod9.diettracker.domain

import java.time.LocalDate

enum class TrendWindow(val days: Long?) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),
    ALL(null),
}

data class TrendSummary(
    val window: TrendWindow,
    val daysWithEntries: Int,
    val dailyAverage: Nutrients,
    val contributingDays: Map<NutrientKey, Int> = emptyMap(),
    val verifiedContributingDays: Map<NutrientKey, Int> = emptyMap(),
) {
    fun completeness(key: NutrientKey): Double =
        if (daysWithEntries == 0) 0.0
        else contributingDays.getOrDefault(key, 0).toDouble() / daysWithEntries

    fun adequate(key: NutrientKey, minimum: Double = 0.8): Boolean =
        completeness(key) >= minimum

    fun verifiedCompleteness(key: NutrientKey): Double =
        if (daysWithEntries == 0) 0.0
        else verifiedContributingDays.getOrDefault(key, 0).toDouble() / daysWithEntries

    fun verifiedAdequate(key: NutrientKey, minimum: Double = 0.8): Boolean =
        verifiedCompleteness(key) >= minimum
}

object TrendAnalyzer {
    fun summarize(days: List<DailyTotals>, window: TrendWindow, today: LocalDate): TrendSummary {
        val lowerBound = window.days?.let { today.minusDays(it - 1) }
        val selected = days
            .filter { !it.date.isAfter(today) }
            .filter { lowerBound == null || !it.date.isBefore(lowerBound) }
            .filter { it.aggregate.totalEntries > 0 }
            .distinctBy { it.date }
            .sortedBy { it.date }

        if (selected.isEmpty()) return TrendSummary(window, 0, Nutrients())

        val averages = mutableMapOf<NutrientKey, Double>()
        val contributing = mutableMapOf<NutrientKey, Int>()
        val verifiedContributing = mutableMapOf<NutrientKey, Int>()

        NutrientKey.entries.forEach { key ->
            val reliableDays = selected.filter { day ->
                day.aggregate.adequate(key) && day.nutrients[key] != null
            }
            if (reliableDays.isNotEmpty()) {
                averages[key] = reliableDays.mapNotNull { it.nutrients[key] }.average()
                contributing[key] = reliableDays.size
            }
            verifiedContributing[key] = selected.count { day ->
                day.aggregate.verifiedAdequate(key) && day.nutrients[key] != null
            }
        }

        return TrendSummary(
            window = window,
            daysWithEntries = selected.size,
            dailyAverage = Nutrients(averages),
            contributingDays = contributing,
            verifiedContributingDays = verifiedContributing,
        )
    }
}
