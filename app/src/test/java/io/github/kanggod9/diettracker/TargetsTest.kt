package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TargetsTest {
    @Test fun customTargetOverridesSelectedRegionalDefault() {
        val settings = mapOf(NutrientTargets.settingKey(NutrientKey.ENERGY) to "1750")
        val targets = NutrientTargets.resolved(GuidanceRegion.SINGAPORE, settings)
        assertEquals(1750.0, targets[NutrientKey.ENERGY]!!, 0.0)
        assertEquals(2000.0, targets[NutrientKey.SODIUM]!!, 0.0)
    }

    @Test fun invalidCustomTargetDoesNotReplaceDefault() {
        val settings = mapOf(NutrientTargets.settingKey(NutrientKey.ENERGY) to "0")
        val targets = NutrientTargets.resolved(GuidanceRegion.US, settings)
        assertEquals(2000.0, targets[NutrientKey.ENERGY]!!, 0.0)
        assertFalse(targets.values.any { it <= 0.0 })
    }
}