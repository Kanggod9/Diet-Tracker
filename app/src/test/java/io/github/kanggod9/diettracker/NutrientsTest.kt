package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.data.NutrientCodec
import io.github.kanggod9.diettracker.domain.*
import io.github.kanggod9.diettracker.domain.aggregateNutrients
import org.junit.Assert.*
import org.junit.Test

class NutrientsTest {
    private val usda = NutrientProvenance(DataSet.USDA_FOUNDATION, "123", "USDA demo", verified = true)

    @Test fun scalingPreservesSourceAndNulls() {
        val source = Nutrients(mapOf(NutrientKey.ENERGY to 100.0, NutrientKey.SODIUM to 0.0), mapOf(NutrientKey.ENERGY to usda))
        val scaled = source.scaled(1.5)
        assertEquals(150.0, scaled[NutrientKey.ENERGY]!!, 0.001)
        assertEquals(0.0, scaled[NutrientKey.SODIUM]!!, 0.001)
        assertNull(scaled[NutrientKey.PROTEIN])
        assertEquals(usda, scaled.provenance[NutrientKey.ENERGY])
    }

    @Test fun codecKeepsExplicitZeroDifferentFromMissing() {
        val decoded = NutrientCodec.decode(NutrientCodec.encode(Nutrients(mapOf(NutrientKey.CAFFEINE to 0.0))))
        assertEquals(0.0, decoded[NutrientKey.CAFFEINE]!!, 0.0)
        assertNull(decoded[NutrientKey.WATER])
    }

    @Test fun aggregateReportsCoverage() {
        val aggregate = NutrientAggregator.aggregate(listOf(Nutrients(mapOf(NutrientKey.PROTEIN to 2.0)), Nutrients()))
        assertEquals(2.0, aggregate.nutrients[NutrientKey.PROTEIN]!!, 0.0)
        assertEquals(.5, aggregate.completeness(NutrientKey.PROTEIN), 0.0)
        assertNull(aggregate.nutrients[NutrientKey.SODIUM])
    }
}
