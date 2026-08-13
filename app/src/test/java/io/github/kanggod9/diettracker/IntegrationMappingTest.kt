package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.*
import io.github.kanggod9.diettracker.integration.*
import org.junit.Assert.*
import org.junit.Test

class IntegrationMappingTest {
    @Test fun healthMappingRoundTripsSupportedValuesWithoutFillingMissing() {
        val n = Nutrients(mapOf(NutrientKey.PROTEIN to 12.0, NutrientKey.WATER to 250.0))
        val draft = HealthNutritionMapper.toDraft("Lunch", MealType.LUNCH, n)
        val mapped = HealthNutritionMapper.fromDraft(draft)
        assertEquals(12.0, mapped[NutrientKey.PROTEIN]!!, 0.0)
        assertNull(mapped[NutrientKey.SODIUM])
    }

    @Test fun photoLabelWinsAndUsdaFillsOnlyMissingFields() {
        val draft = AiContractParser.parse(
            """{"schema_version":1,"source_type":"PACKAGE","name":"Cereal","generic_name":"cereal","usda_query":"cereal","kind":"FOOD","amount_value":50.0,"amount_unit":"GRAM","meal_type":"BREAKFAST","confidence":0.9,"nutrients":{"ENERGY":{"value":210.0,"unit":"kcal","basis":"visible label","source":"PACKAGE_LABEL"}},"warnings":[]}""",
        )
        val usdaSource = NutrientProvenance(
            DataSet.USDA_FOUNDATION,
            sourceId = "10",
            sourceLabel = "USDA Foundation cereal",
            verified = true,
        )
        val food = UsdaFood(
            10,
            "Cereal",
            UsdaDataType.FOUNDATION,
            Nutrients(
                values = mapOf(NutrientKey.ENERGY to 300.0, NutrientKey.PROTEIN to 12.0),
                provenance = mapOf(NutrientKey.ENERGY to usdaSource, NutrientKey.PROTEIN to usdaSource),
            ),
        )

        val merged = draft.withMissingUsda(food, 50.0)
        assertEquals(210.0, merged.nutrients[NutrientKey.ENERGY]!!, 0.0)
        assertEquals(DataSet.PACKAGE_LABEL, merged.nutrients.provenance[NutrientKey.ENERGY]?.dataSet)
        assertEquals(6.0, merged.nutrients[NutrientKey.PROTEIN]!!, 0.0)
        assertEquals(DataSet.USDA_FOUNDATION, merged.nutrients.provenance[NutrientKey.PROTEIN]?.dataSet)
    }

    @Test fun aiContractAcceptsBothAsAnEditableEstimateKind() {
        val draft = AiContractParser.parse(
            """{"schema_version":1,"source_type":"INGREDIENT","name":"Soup and drink","generic_name":"meal","usda_query":"soup","kind":"BOTH","amount_value":1.0,"amount_unit":"SERVING","meal_type":"LUNCH","confidence":0.7,"nutrients":{"ENERGY":{"value":300.0,"unit":"kcal","basis":"estimated portion","source":"AI_ESTIMATE"},"WATER":{"value":250.0,"unit":"g","basis":"estimated liquid","source":"AI_ESTIMATE"}},"warnings":[]}""",
        )

        assertEquals(EntryKind.BOTH, draft.kind)
        assertEquals(300.0, draft.nutrients[NutrientKey.ENERGY]!!, 0.0)
        assertEquals(250.0, draft.nutrients[NutrientKey.WATER]!!, 0.0)
    }

    @Test fun usdaContractAllowsOnlyFoundationAndSrLegacy() {
        val contract = UsdaSearchContract("oats")
        assertEquals("Foundation,SR Legacy", contract.dataTypeParameter)
    }
}