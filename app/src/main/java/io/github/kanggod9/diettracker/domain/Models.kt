package io.github.kanggod9.diettracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class EntryKind(val displayName: String) { FOOD("Food"), DRINK("Drink") }

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    LATE_NIGHT("Late night"),
    COOKING_OIL("Cooking oil"),
    UNKNOWN("Unspecified"),
}

enum class AmountUnit(val symbol: String) {
    GRAM("g"),
    MILLILITRE("mL"),
    SERVING("serving"),
}

data class EntryAmount(val value: Double, val unit: AmountUnit) {
    init {
        require(value.isFinite() && value > 0.0 && value <= 100_000.0) {
            "Amount must be finite and between 0 and 100,000"
        }
    }
}

enum class DataSet {
    USDA_FOUNDATION,
    USDA_SR_LEGACY,
    PACKAGE_LABEL,
    MANUAL,
    AI_DRAFT,
    HEALTH_CONNECT,
}

enum class NutrientKey(val label: String, val unit: String) {
    ENERGY("Energy", "kcal"),
    ENERGY_FROM_FAT("Energy from fat", "kcal"),
    PROTEIN("Protein", "g"),
    TOTAL_CARBOHYDRATE("Carbohydrate", "g"),
    TOTAL_FAT("Fat", "g"),
    SATURATED_FAT("Saturated fat", "g"),
    MONOUNSATURATED_FAT("Monounsaturated fat", "g"),
    POLYUNSATURATED_FAT("Polyunsaturated fat", "g"),
    UNSATURATED_FAT("Unsaturated fat", "g"),
    TRANS_FAT("Trans fat", "g"),
    DIETARY_FIBER("Dietary fibre", "g"),
    TOTAL_SUGAR("Total sugar", "g"),
    ADDED_SUGAR("Added sugar", "g"),
    SODIUM("Sodium", "mg"),
    CHOLESTEROL("Cholesterol", "mg"),
    CAFFEINE("Caffeine", "mg"),
    WATER("Water", "g"),
    CALCIUM("Calcium", "mg"),
    CHLORIDE("Chloride", "mg"),
    CHROMIUM("Chromium", "mcg"),
    COPPER("Copper", "mg"),
    FOLATE("Folate", "mcg"),
    FOLIC_ACID("Folic acid", "mcg"),
    IODINE("Iodine", "mcg"),
    IRON("Iron", "mg"),
    MAGNESIUM("Magnesium", "mg"),
    MANGANESE("Manganese", "mg"),
    MOLYBDENUM("Molybdenum", "mcg"),
    NIACIN("Niacin", "mg"),
    PANTOTHENIC_ACID("Pantothenic acid", "mg"),
    PHOSPHORUS("Phosphorus", "mg"),
    POTASSIUM("Potassium", "mg"),
    RIBOFLAVIN("Riboflavin", "mg"),
    SELENIUM("Selenium", "mcg"),
    THIAMIN("Thiamin", "mg"),
    VITAMIN_A("Vitamin A", "mcg"),
    VITAMIN_B6("Vitamin B6", "mg"),
    VITAMIN_B12("Vitamin B12", "mcg"),
    VITAMIN_C("Vitamin C", "mg"),
    VITAMIN_D("Vitamin D", "mcg"),
    VITAMIN_E("Vitamin E", "mg"),
    VITAMIN_K("Vitamin K", "mcg"),
    ZINC("Zinc", "mg"),
    BIOTIN("Biotin", "mcg"),
}

data class NutrientProvenance(
    val dataSet: DataSet,
    val sourceId: String? = null,
    val sourceLabel: String,
    val sourceUrl: String? = null,
    val sourceVersion: String? = null,
    val retrievedAt: Instant? = null,
    val verified: Boolean = false,
)

data class Nutrients(
    val values: Map<NutrientKey, Double> = emptyMap(),
    val provenance: Map<NutrientKey, NutrientProvenance> = emptyMap(),
) {
    init {
        require(values.values.all { it.isFinite() && it >= 0.0 }) {
            "Nutrients must be finite and non-negative"
        }
        require(provenance.keys.all { it in values }) {
            "Provenance cannot exist without a nutrient value"
        }
    }

    operator fun get(key: NutrientKey): Double? = values[key]

    fun scaled(factor: Double): Nutrients {
        require(factor.isFinite() && factor >= 0.0)
        return copy(values = values.mapValues { (_, value) -> value * factor })
    }

    fun verifiedUsdaValue(key: NutrientKey): Double? {
        val source = provenance[key] ?: return null
        return values[key]?.takeIf {
            source.verified && source.dataSet in setOf(DataSet.USDA_FOUNDATION, DataSet.USDA_SR_LEGACY)
        }
    }

    companion object {
        fun totals(items: Iterable<Nutrients>): Nutrients {
            val all = items.toList()
            val totals = NutrientKey.entries.mapNotNull { key ->
                val present = all.mapNotNull { it[key] }
                if (present.isEmpty()) null else key to present.sum()
            }.toMap()
            return Nutrients(totals)
        }
    }
}

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: EntryKind,
    val mealType: MealType,
    val servingDescription: String,
    val servingGrams: Double?,
    val amount: EntryAmount = servingGrams?.let { EntryAmount(it, AmountUnit.GRAM) }
        ?: EntryAmount(1.0, AmountUnit.SERVING),
    val loggedAt: Instant = Instant.now(),
    val nutrients: Nutrients,
    val note: String = "",
) {
    init {
        require(name.isNotBlank() && name.length <= 120)
        require(note.length <= 2_000)
        require(servingGrams == null || servingGrams.isFinite() && servingGrams > 0.0)
    }
}

data class QuickFood(
    val id: String,
    val name: String,
    val kind: EntryKind,
    val servingDescription: String,
    val servingGrams: Double?,
    val nutrients: Nutrients,
    val amount: EntryAmount = servingGrams?.let { EntryAmount(it, AmountUnit.GRAM) }
        ?: EntryAmount(1.0, AmountUnit.SERVING),
    val mealType: MealType = MealType.SNACK,
)

data class NutrientAggregate(
    val nutrients: Nutrients,
    val contributingEntries: Map<NutrientKey, Int>,
    val verifiedContributingEntries: Map<NutrientKey, Int>,
    val totalEntries: Int,
) {
    fun completeness(key: NutrientKey): Double =
        if (totalEntries == 0) 0.0
        else contributingEntries.getOrDefault(key, 0).toDouble() / totalEntries

    fun adequate(key: NutrientKey, minimum: Double = 0.8): Boolean =
        completeness(key) >= minimum

    fun verifiedCompleteness(key: NutrientKey): Double =
        if (totalEntries == 0) 0.0
        else verifiedContributingEntries.getOrDefault(key, 0).toDouble() / totalEntries

    fun verifiedAdequate(key: NutrientKey, minimum: Double = 0.8): Boolean =
        verifiedCompleteness(key) >= minimum
}

data class DailyTotals(val date: LocalDate, val aggregate: NutrientAggregate) {
    val nutrients: Nutrients get() = aggregate.nutrients
}

object NutrientAggregator {
    fun aggregate(items: Iterable<Nutrients>): NutrientAggregate {
        val all = items.toList()
        return NutrientAggregate(
            nutrients = Nutrients.totals(all),
            contributingEntries = NutrientKey.entries.associateWith { key ->
                all.count { it[key] != null }
            },
            verifiedContributingEntries = NutrientKey.entries.associateWith { key ->
                all.count { item -> item.provenance[key]?.verified == true }
            },
            totalEntries = all.size,
        )
    }
}

fun aggregateNutrients(items: Iterable<Nutrients>): NutrientAggregate =
    NutrientAggregator.aggregate(items)

fun JournalEntry.localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    loggedAt.atZone(zoneId).toLocalDate()
