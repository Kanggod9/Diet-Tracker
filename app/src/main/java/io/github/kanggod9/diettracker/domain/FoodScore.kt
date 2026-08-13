package io.github.kanggod9.diettracker.domain

import java.util.Locale
import kotlin.math.roundToInt

enum class ScoreDirection { ENCOURAGE, LIMIT }

data class ScoreComponent(
    val key: NutrientKey,
    val label: String,
    val direction: ScoreDirection,
    val points: Int,
    val valuePer100Kcal: Double,
    val expectedPer100Kcal: Double,
    val densityRatio: Double,
    val maximumPoints: Int,
    val dailyValue: Double,
    val explanation: String,
)

data class FoodScore(
    val score: Int?,
    val formulaVersion: String,
    val components: List<ScoreComponent>,
    val completeness: Double,
    val missingComponents: List<NutrientKey>,
    val basis: String,
    val unavailableReason: String? = null,
)

object FoodScoreCalculator {
    const val FORMULA_VERSION = "DT-DV-2"
    const val DISCLAIMER =
        "Informational nutrient-density indicator; not an FDA score and not medical advice."

    private const val DAILY_ENERGY_KCAL = 2_000.0
    private const val SCORE_BASIS_KCAL = 100.0


    private val encouraged = linkedMapOf(
        NutrientKey.DIETARY_FIBER to 28.0,
        NutrientKey.PROTEIN to 50.0,
        NutrientKey.POTASSIUM to 4_700.0,
        NutrientKey.CALCIUM to 1_300.0,
        NutrientKey.IRON to 18.0,
        NutrientKey.VITAMIN_D to 20.0,
        NutrientKey.VITAMIN_C to 90.0,
    )

    private val limited = linkedMapOf(
        NutrientKey.SATURATED_FAT to 20.0,
        NutrientKey.SODIUM to 2_300.0,
        NutrientKey.ADDED_SUGAR to 50.0,
        NutrientKey.CHOLESTEROL to 300.0,
    )

    /**
     * Version 2 compares any reported nutrient values with FDA Daily Value density per 100 kcal. Values may come
     * from USDA, a package label, an AI draft, Health Connect, or manual review. Unreported nutrients are excluded
     * rather than treated as zero, and both the positive and limiting pools contribute around a neutral base of 50.
     */
    fun calculate(nutrients: Nutrients): FoodScore {
        val basis = "Reported values per 100 kcal compared with FDA Daily Values."
        val energy = nutrients[NutrientKey.ENERGY]
            ?: return unavailable(nutrients, basis, "Energy is required.")
        if (energy == 0.0) {
            return unavailable(
                nutrients,
                basis,
                "A per-100-kcal score is not applicable to a zero-energy food or drink.",
            )
        }

        val positiveValues = encouraged.mapNotNull { (key, dailyValue) ->
            nutrients[key]?.let { Triple(key, dailyValue, it) }
        }
        val limitValues = limited.mapNotNull { (key, dailyValue) ->
            nutrients[key]?.let { Triple(key, dailyValue, it) }
        }
        val expectedCount = encouraged.size + limited.size
        val presentCount = positiveValues.size + limitValues.size
        val completeness = presentCount.toDouble() / expectedCount
        val missing = (encouraged.keys + limited.keys).filter { nutrients[it] == null }

        if (positiveValues.isEmpty() || limitValues.isEmpty()) {
            return FoodScore(
                score = null,
                formulaVersion = FORMULA_VERSION,
                components = emptyList(),
                completeness = completeness,
                missingComponents = missing,
                basis = basis,
                unavailableReason = "At least one encouraged and one limiting nutrient are required.",
            )
        }

        val positiveWeight = 50.0 / positiveValues.size
        val limitWeight = 50.0 / limitValues.size
        val components = buildList {
            positiveValues.forEach { (key, dailyValue, servingValue) ->
                add(component(key, dailyValue, servingValue, energy, ScoreDirection.ENCOURAGE, positiveWeight))
            }
            limitValues.forEach { (key, dailyValue, servingValue) ->
                add(component(key, dailyValue, servingValue, energy, ScoreDirection.LIMIT, limitWeight))
            }
        }
        val score = (50 + components.sumOf { it.points }).coerceIn(0, 100)

        return FoodScore(
            score = score,
            formulaVersion = FORMULA_VERSION,
            components = components,
            completeness = completeness,
            missingComponents = missing,
            basis = basis,
        )
    }

    private fun component(
        key: NutrientKey,
        dailyValue: Double,
        servingValue: Double,
        energy: Double,
        direction: ScoreDirection,
        weight: Double,
    ): ScoreComponent {
        val per100Kcal = servingValue * SCORE_BASIS_KCAL / energy
        val expectedPer100Kcal = dailyValue * SCORE_BASIS_KCAL / DAILY_ENERGY_KCAL
        val densityRatio = per100Kcal / expectedPer100Kcal
        val normalized = (densityRatio / 2.0).coerceIn(0.0, 1.0)
        val signedPoints = (normalized * weight * if (direction == ScoreDirection.ENCOURAGE) 1 else -1)
            .roundToInt()
        val sign = if (signedPoints > 0) "+" else ""
        val action = if (direction == ScoreDirection.ENCOURAGE) {
            "adds points because higher nutrient density is encouraged"
        } else {
            "deducts points because lower intake is encouraged"
        }
        val maximumPoints = weight.roundToInt()
        return ScoreComponent(
            key = key,
            label = key.label,
            direction = direction,
            points = signedPoints,
            valuePer100Kcal = per100Kcal,
            expectedPer100Kcal = expectedPer100Kcal,
            densityRatio = densityRatio,
            maximumPoints = maximumPoints,
            dailyValue = dailyValue,
            explanation =
                "$sign$signedPoints points - ${key.label} $action: " +
                    "${format(per100Kcal)} ${key.unit}/100 kcal versus " +
                    "${format(expectedPer100Kcal)} ${key.unit}/100 kcal at FDA Daily Value density " +
                    "(${format(densityRatio * 100.0)}%); maximum " +
                    "${if (direction == ScoreDirection.ENCOURAGE) "+" else "-"}$maximumPoints points.",
        )
    }

    private fun unavailable(nutrients: Nutrients, basis: String, reason: String): FoodScore {
        val all = encouraged.keys + limited.keys
        val present = all.count { nutrients[it] != null }
        return FoodScore(
            score = null,
            formulaVersion = FORMULA_VERSION,
            components = emptyList(),
            completeness = present.toDouble() / all.size,
            missingComponents = all.filter { nutrients[it] == null },
            basis = basis,
            unavailableReason = reason,
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
}
