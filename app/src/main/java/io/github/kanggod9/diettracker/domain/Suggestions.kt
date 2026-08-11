package io.github.kanggod9.diettracker.domain

import java.time.LocalDate
import java.util.Locale

enum class GuidanceRegion(val displayName: String) {
    US("United States"),
    EU("European Union"),
    SINGAPORE("Singapore"),
}

enum class TargetDirection { REFERENCE, MINIMUM, UPPER_LIMIT }
enum class SuggestionBasis { TODAY, TREND }

data class GuidanceTarget(
    val key: NutrientKey,
    val amount: Double,
    val direction: TargetDirection,
    val allowTrendLow: Boolean = direction != TargetDirection.UPPER_LIMIT,
    val requiresVerifiedDistinctField: Boolean = false,
)

data class GuidanceProfile(
    val region: GuidanceRegion,
    val title: String,
    val sourceUrl: String,
    val sourceEffectiveVersion: String,
    val retrievedOn: LocalDate,
    val disclaimer: String,
    val targets: List<GuidanceTarget>,
)

data class Suggestion(
    val id: String,
    val message: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val region: GuidanceRegion? = null,
    val basis: SuggestionBasis = SuggestionBasis.TODAY,
    val nutrient: NutrientKey? = null,
    val evidence: String = "",
)

object GuidanceProfiles {
    private const val GENERAL =
        "General adult reference for comparison only; not a personalised prescription or medical advice."
    private val RETRIEVED_ON = LocalDate.of(2026, 8, 11)

    val US = GuidanceProfile(
        region = GuidanceRegion.US,
        title = "US FDA Daily Values",
        sourceUrl =
            "https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels",
        sourceEffectiveVersion = "Current FDA Daily Values for adults and children age 4+",
        retrievedOn = RETRIEVED_ON,
        disclaimer = GENERAL,
        targets = listOf(
            GuidanceTarget(NutrientKey.ENERGY, 2_000.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.TOTAL_FAT, 78.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.SATURATED_FAT, 20.0, TargetDirection.UPPER_LIMIT),
            GuidanceTarget(NutrientKey.CHOLESTEROL, 300.0, TargetDirection.UPPER_LIMIT),
            GuidanceTarget(NutrientKey.SODIUM, 2_300.0, TargetDirection.UPPER_LIMIT),
            GuidanceTarget(NutrientKey.TOTAL_CARBOHYDRATE, 275.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.DIETARY_FIBER, 28.0, TargetDirection.MINIMUM),
            GuidanceTarget(NutrientKey.ADDED_SUGAR, 50.0, TargetDirection.UPPER_LIMIT, requiresVerifiedDistinctField = true),
            GuidanceTarget(NutrientKey.PROTEIN, 50.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.VITAMIN_D, 20.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CALCIUM, 1_300.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.IRON, 18.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.POTASSIUM, 4_700.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_A, 900.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_C, 90.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_E, 15.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_K, 120.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.THIAMIN, 1.2, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.RIBOFLAVIN, 1.3, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.NIACIN, 16.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_B6, 1.7, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.FOLATE, 400.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_B12, 2.4, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.BIOTIN, 30.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.PANTOTHENIC_ACID, 5.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.PHOSPHORUS, 1_250.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.IODINE, 150.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MAGNESIUM, 420.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.ZINC, 11.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.SELENIUM, 55.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.COPPER, 0.9, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MANGANESE, 2.3, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CHROMIUM, 35.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MOLYBDENUM, 45.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CHLORIDE, 2_300.0, TargetDirection.REFERENCE),
        ),
    )

    val EU = GuidanceProfile(
        region = GuidanceRegion.EU,
        title = "EU Reference Intakes and Nutrient Reference Values",
        sourceUrl =
            "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011R1169-20250401",
        sourceEffectiveVersion = "Regulation (EU) No 1169/2011, consolidated 1 April 2025, Annex XIII",
        retrievedOn = RETRIEVED_ON,
        disclaimer = GENERAL,
        targets = listOf(
            GuidanceTarget(NutrientKey.ENERGY, 2_000.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.TOTAL_FAT, 70.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.SATURATED_FAT, 20.0, TargetDirection.UPPER_LIMIT),
            GuidanceTarget(NutrientKey.TOTAL_CARBOHYDRATE, 260.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.TOTAL_SUGAR, 90.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.PROTEIN, 50.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.SODIUM, 2_400.0, TargetDirection.REFERENCE, allowTrendLow = false),
            GuidanceTarget(NutrientKey.VITAMIN_A, 800.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_D, 5.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_E, 12.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_K, 75.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_C, 80.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.THIAMIN, 1.1, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.RIBOFLAVIN, 1.4, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.NIACIN, 16.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_B6, 1.4, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.FOLIC_ACID, 200.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.VITAMIN_B12, 2.5, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.BIOTIN, 50.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.PANTOTHENIC_ACID, 6.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.POTASSIUM, 2_000.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CHLORIDE, 800.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CALCIUM, 800.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.PHOSPHORUS, 700.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MAGNESIUM, 375.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.IRON, 14.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.ZINC, 10.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.COPPER, 1.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MANGANESE, 2.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.SELENIUM, 55.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.CHROMIUM, 40.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.MOLYBDENUM, 50.0, TargetDirection.REFERENCE),
            GuidanceTarget(NutrientKey.IODINE, 150.0, TargetDirection.REFERENCE),
        ),
    )

    val SINGAPORE = GuidanceProfile(
        region = GuidanceRegion.SINGAPORE,
        title = "Singapore HealthHub adult dietary guidance",
        sourceUrl =
            "https://www.healthhub.sg/well-being-and-lifestyle/food-diet-and-nutrition/dietary_guidelines_adults",
        sourceEffectiveVersion = "HealthHub adult guidance current at retrieval",
        retrievedOn = RETRIEVED_ON,
        disclaimer = GENERAL,
        targets = listOf(
            GuidanceTarget(NutrientKey.SODIUM, 2_000.0, TargetDirection.UPPER_LIMIT),
            GuidanceTarget(
                NutrientKey.ADDED_SUGAR,
                50.0,
                TargetDirection.UPPER_LIMIT,
                requiresVerifiedDistinctField = true,
            ),
        ),
    )

    val all: List<GuidanceProfile> = listOf(US, EU, SINGAPORE)
}

object SuggestionEngine {
    private const val MINIMUM_TREND_DAYS = 3
    private const val REQUIRED_COVERAGE = 0.8

    fun generate(
        today: NutrientAggregate,
        trend: TrendSummary?,
        profile: GuidanceProfile,
    ): List<Suggestion> = profile.targets.mapNotNull { target ->
        todayUpperSuggestion(today, target, profile)
            ?: trendSuggestion(trend, target, profile)
    }

    /** Compatibility overload for callers that only have totals; coverage is intentionally one complete item. */
    fun generate(today: Nutrients, profile: GuidanceProfile): List<Suggestion> =
        generate(aggregateNutrients(listOf(today)), null, profile)

    private fun todayUpperSuggestion(
        today: NutrientAggregate,
        target: GuidanceTarget,
        profile: GuidanceProfile,
    ): Suggestion? {
        if (target.direction != TargetDirection.UPPER_LIMIT) return null
        if (!today.hasCoverage(target)) return null
        val actual = today.nutrients[target.key] ?: return null
        val ratio = actual / target.amount
        if (ratio < 0.85) return null
        val evidence =
            "Today: ${format(actual)} ${target.key.unit} of ${format(target.amount)} ${target.key.unit}"
        return Suggestion(
            id = "${profile.region}-TODAY-${target.key}",
            message =
                "Today's logged ${target.key.label.lowercase()} is ${percent(ratio)} of this profile's " +
                    "general upper reference. Review later choices if that is useful to you.",
            sourceTitle = profile.title,
            sourceUrl = profile.sourceUrl,
            region = profile.region,
            basis = SuggestionBasis.TODAY,
            nutrient = target.key,
            evidence = evidence,
        )
    }

    private fun trendSuggestion(
        trend: TrendSummary?,
        target: GuidanceTarget,
        profile: GuidanceProfile,
    ): Suggestion? {
        trend ?: return null
        if (trend.daysWithEntries < MINIMUM_TREND_DAYS || !trend.hasCoverage(target)) return null
        val actual = trend.dailyAverage[target.key] ?: return null
        val ratio = actual / target.amount
        val shouldSuggest = when (target.direction) {
            TargetDirection.UPPER_LIMIT -> ratio >= 0.9
            TargetDirection.MINIMUM, TargetDirection.REFERENCE -> target.allowTrendLow && ratio < 0.6
        }
        if (!shouldSuggest) return null
        val directionText = when (target.direction) {
            TargetDirection.UPPER_LIMIT -> "near or above"
            TargetDirection.MINIMUM, TargetDirection.REFERENCE -> "below"
        }
        val evidence =
            "${trend.daysWithEntries}-day logged average: ${format(actual)} ${target.key.unit}; " +
                "reference ${format(target.amount)} ${target.key.unit}"
        return Suggestion(
            id = "${profile.region}-TREND-${target.key}",
            message =
                "Across ${trend.daysWithEntries} logged days, ${target.key.label.lowercase()} remained " +
                    "$directionText this profile's general reference. Check data completeness and, if appropriate, " +
                    "consider food sources of this nutrient.",
            sourceTitle = profile.title,
            sourceUrl = profile.sourceUrl,
            region = profile.region,
            basis = SuggestionBasis.TREND,
            nutrient = target.key,
            evidence = evidence,
        )
    }

    private fun NutrientAggregate.hasCoverage(target: GuidanceTarget): Boolean =
        if (target.requiresVerifiedDistinctField) verifiedAdequate(target.key, REQUIRED_COVERAGE)
        else adequate(target.key, REQUIRED_COVERAGE)

    private fun TrendSummary.hasCoverage(target: GuidanceTarget): Boolean =
        if (target.requiresVerifiedDistinctField) verifiedAdequate(target.key, REQUIRED_COVERAGE)
        else adequate(target.key, REQUIRED_COVERAGE)

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
    private fun percent(ratio: Double): String = "${(ratio * 100).toInt()}%"
}
