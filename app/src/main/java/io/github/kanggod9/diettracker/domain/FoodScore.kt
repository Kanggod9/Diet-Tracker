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
    const val FORMULA_VERSION = "DT-USDA-DV-1"
    const val DISCLAIMER =
        "Informational nutrient-density indicator; not an FDA score, not medical advice, and not a judgement of a food."

    private const val DAILY_ENERGY_KCAL = 2_000.0
    private const val SCORE_BASIS_KCAL = 100.0
    private const val MINIMUM_COMPLETENESS = 0.5

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
     * Version 1 compares verified Foundation/SR Legacy nutrient density per 100 kcal with the FDA Daily Value
     * density implied by a 2,000 kcal day. Missing nutrients are never treated as zero. The positive and limiting
     * pools each contribute at most 50 points around a neutral base of 50.
     */
    fun calculate(nutrients: Nutrients): FoodScore {
        val basis = "Verified USDA Foundation/SR Legacy values per 100 kcal compared with FDA Daily Values."
        val energy = nutrients.verifiedUsdaValue(NutrientKey.ENERGY)
            ?: return unavailable(nutrients, basis, "A verified USDA energy value is required.")
        if (energy == 0.0) {
            return unavailable(
                nutrients,
                basis,
                "A per-100-kcal score is not applicable to a verified zero-energy food or drink.",
            )
        }

        val positiveValues = encouraged.mapNotNull { (key, dailyValue) ->
            nutrients.verifiedUsdaValue(key)?.let { Triple(key, dailyValue, it) }
        }
        val limitValues = limited.mapNotNull { (key, dailyValue) ->
            nutrients.verifiedUsdaValue(key)?.let { Triple(key, dailyValue, it) }
        }
        val expectedCount = encouraged.size + limited.size
        val presentCount = positiveValues.size + limitValues.size
        val completeness = presentCount.toDouble() / expectedCount
        val missing = (encouraged.keys + limited.keys).filter { nutrients.verifiedUsdaValue(it) == null }

        if (completeness < MINIMUM_COMPLETENESS || positiveValues.size < 3 || limitValues.size < 2) {
            return FoodScore(
                score = null,
                formulaVersion = FORMULA_VERSION,
                components = emptyList(),
                completeness = completeness,
                missingComponents = missing,
                basis = basis,
                unavailableReason =
                    "At least 50% of score fields, including three encouraged and two limiting nutrients, are required.",
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
        return ScoreComponent(
            key = key,
            label = key.label,
            direction = direction,
            points = signedPoints,
            valuePer100Kcal = per100Kcal,
            dailyValue = dailyValue,
            explanation =
                "$sign$signedPoints points: ${format(per100Kcal)} ${key.unit}/100 kcal; " +
                    "FDA DV ${format(dailyValue)} ${key.unit}.",
        )
    }

    private fun unavailable(nutrients: Nutrients, basis: String, reason: String): FoodScore {
        val all = encouraged.keys + limited.keys
        val present = all.count { nutrients.verifiedUsdaValue(it) != null }
        return FoodScore(
            score = null,
            formulaVersion = FORMULA_VERSION,
            components = emptyList(),
            completeness = present.toDouble() / all.size,
            missingComponents = all.filter { nutrients.verifiedUsdaValue(it) == null },
            basis = basis,
            unavailableReason = reason,
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
}
