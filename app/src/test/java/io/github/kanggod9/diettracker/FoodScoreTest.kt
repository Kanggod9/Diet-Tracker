package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.*
import org.junit.Assert.*
import org.junit.Test

class FoodScoreTest {
    @Test fun scoreRejectsUnverifiedValues() {
        val n = Nutrients(mapOf(NutrientKey.ENERGY to 100.0, NutrientKey.DIETARY_FIBER to 8.0))
        assertNull(FoodScoreCalculator.calculate(n).score)
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
    }
}
