package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.*
import org.junit.Assert.*
import org.junit.Test

class FoodScoreTest {
    @Test fun scoreAcceptsReportedValuesWithoutUsdaVerification() {
        val n = Nutrients(
            mapOf(
                NutrientKey.ENERGY to 100.0,
                NutrientKey.DIETARY_FIBER to 8.0,
                NutrientKey.SODIUM to 100.0,
            ),
        )
        assertNotNull(FoodScoreCalculator.calculate(n).score)
    }

    @Test fun verifiedUsdaFieldsProduceExplainableScore() {
        val p = NutrientProvenance(DataSet.USDA_SR_LEGACY, "9", "USDA SR Legacy", verified = true)
        val values = mapOf(
            NutrientKey.ENERGY to 200.0,
            NutrientKey.DIETARY_FIBER to 5.0,
            NutrientKey.PROTEIN to 12.0,
            NutrientKey.CALCIUM to 150.0,
            NutrientKey.SODIUM to 100.0,
            NutrientKey.SATURATED_FAT to 2.0,
            NutrientKey.CHOLESTEROL to 10.0,
        )
        val result = FoodScoreCalculator.calculate(Nutrients(values, values.keys.associateWith { p }))
        assertNotNull(result.score)
        assertTrue(result.components.all { it.explanation.isNotBlank() })
        assertTrue(FoodScoreCalculator.DISCLAIMER.contains("not medical advice"))
        assertFalse(FoodScoreCalculator.DISCLAIMER.contains("judgement"))
    }

    @Test fun explanationsNameTheNutrientReasonReferenceAndMaximumPoints() {
        val score = FoodScoreCalculator.calculate(
            Nutrients(
                mapOf(
                    NutrientKey.ENERGY to 100.0,
                    NutrientKey.DIETARY_FIBER to 3.0,
                    NutrientKey.SODIUM to 135.0,
                ),
            ),
        )
        val addition = score.components.single { it.key == NutrientKey.DIETARY_FIBER }
        val deduction = score.components.single { it.key == NutrientKey.SODIUM }

        assertTrue(addition.explanation.contains("Dietary fibre"))
        assertTrue(addition.explanation.contains("adds points"))
        assertTrue(addition.explanation.contains("versus"))
        assertTrue(addition.explanation.contains("FDA Daily Value density"))
        assertTrue(addition.explanation.contains("maximum +"))
        assertTrue(addition.expectedPer100Kcal > 0.0)
        assertTrue(addition.densityRatio > 0.0)

        assertTrue(deduction.explanation.contains("Sodium"))
        assertTrue(deduction.explanation.contains("deducts points"))
        assertTrue(deduction.explanation.contains("maximum -"))
        assertTrue(deduction.maximumPoints > 0)
    }
}