package io.github.kanggod9.diettracker.domain

import io.github.kanggod9.diettracker.integration.UsdaFood
import java.time.Clock
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

data class ParsedManualEntry(
    val name: String,
    val amount: EntryAmount? = null,
    val targetCalories: Double? = null,
    val mealType: MealType,
    val kind: EntryKind,
    val originalText: String,
)

object ManualEntryParser {
    private val amountPattern = Regex(
        "(\\d+(?:\\.\\d+)?)\\s*(kilograms?|kg|grams?|g|millilit(?:er|re)s?|ml|lit(?:er|re)s?|l|servings?|portions?|kcal|calories?|cals?|\\u5343\\u5361|\\u5927\\u5361|\\u5361\\u8DEF\\u91CC|\\u5361|\\u516C\\u65A4|\\u5343\\u514B|\\u514B|\\u6BEB\\u5347|\\u5347|\\u4EFD)",
        RegexOption.IGNORE_CASE,
    )
    private val mealPattern = Regex(
        "\\b(breakfast|lunch|dinner|supper|snack)\\b|\\u65E9\\u9910|\\u65E9\\u996D|\\u5348\\u9910|\\u5348\\u996D|\\u665A\\u9910|\\u665A\\u996D|\\u591C\\u5BB5|\\u52A0\\u9910|\\u96F6\\u98DF",
        RegexOption.IGNORE_CASE,
    )
    private val fillerPattern = Regex(
        "\\b(i|ate|had|drink|drank|log|record|about|around|approximately|roughly|please|today|just)\\b|\\u4ECA\\u5929|\\u521A\\u624D|\\u6211|\\u5403\\u4E86|\\u5403\\u7684|\\u5403|\\u559D\\u4E86|\\u5927\\u6982|\\u5927\\u7EA6|\\u5DEE\\u4E0D\\u591A|\\u5E2E\\u6211|\\u8BB0\\u4E00\\u4E0B|\\u8BB0\\u5F55|\\u8865\\u8BB0|\\u5DE6\\u53F3",
        RegexOption.IGNORE_CASE,
    )

    fun parse(input: String, clock: Clock = Clock.systemDefaultZone()): ParsedManualEntry? {
        val original = input.trim().take(240)
        if (original.isBlank()) return null

        var amount: EntryAmount? = null
        var targetCalories: Double? = null
        var sawVolume = false
        amountPattern.findAll(original).forEach { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: return@forEach
            if (!value.isFinite() || value <= 0.0) return@forEach
            when (match.groupValues[2].lowercase(Locale.ROOT)) {
                "kilogram", "kilograms", "kg", "\u516C\u65A4", "\u5343\u514B" ->
                    if (amount == null) amount = EntryAmount(value * 1_000.0, AmountUnit.GRAM)
                "gram", "grams", "g", "\u514B" ->
                    if (amount == null) amount = EntryAmount(value, AmountUnit.GRAM)
                "milliliter", "milliliters", "millilitre", "millilitres", "ml", "\u6BEB\u5347" -> {
                    if (amount == null) amount = EntryAmount(value, AmountUnit.MILLILITRE)
                    sawVolume = true
                }
                "liter", "liters", "litre", "litres", "l", "\u5347" -> {
                    if (amount == null) amount = EntryAmount(value * 1_000.0, AmountUnit.MILLILITRE)
                    sawVolume = true
                }
                "serving", "servings", "portion", "portions", "\u4EFD" ->
                    if (amount == null) amount = EntryAmount(value, AmountUnit.SERVING)
                else -> if (targetCalories == null) targetCalories = value
            }
        }

        val name = original
            .replace(amountPattern, " ")
            .replace(mealPattern, " ")
            .replace(fillerPattern, " ")
            .replace(Regex("[,.;:\\uFF0C\\u3002\\uFF1B\\u3001\\uFF1A]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
        if (name.isBlank() || amount == null && targetCalories == null) return null
        if ((targetCalories ?: 0.0) > 10_000.0) return null

        val kind = if (
            sawVolume || Regex(
                "\\b(water|coffee|tea|juice|milk|soda|drink)\\b|\\u6C34|\\u5496\\u5561|\\u8336|\\u679C\\u6C41|\\u725B\\u5976|\\u996E\\u6599",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(name)
        ) EntryKind.DRINK else EntryKind.FOOD

        return ParsedManualEntry(
            name = name,
            amount = amount,
            targetCalories = targetCalories,
            mealType = inferMealType(original, clock),
            kind = kind,
            originalText = original,
        )
    }

    private fun inferMealType(text: String, clock: Clock): MealType = when {
        Regex("\\b(breakfast)\\b|\\u65E9\\u9910|\\u65E9\\u996D", RegexOption.IGNORE_CASE)
            .containsMatchIn(text) -> MealType.BREAKFAST
        Regex("\\b(lunch)\\b|\\u5348\\u9910|\\u5348\\u996D", RegexOption.IGNORE_CASE)
            .containsMatchIn(text) -> MealType.LUNCH
        Regex("\\b(dinner|supper)\\b|\\u665A\\u9910|\\u665A\\u996D", RegexOption.IGNORE_CASE)
            .containsMatchIn(text) -> MealType.DINNER
        Regex("\\u591C\\u5BB5").containsMatchIn(text) -> MealType.LATE_NIGHT
        Regex("\\b(snack)\\b|\\u52A0\\u9910|\\u96F6\\u98DF", RegexOption.IGNORE_CASE)
            .containsMatchIn(text) -> MealType.SNACK
        else -> when (LocalTime.now(clock).hour) {
            in 4..9 -> MealType.BREAKFAST
            in 10..14 -> MealType.LUNCH
            in 15..21 -> MealType.DINNER
            else -> MealType.SNACK
        }
    }
}

object ManualEntryEstimator {
    fun fromUsda(parsed: ParsedManualEntry, food: UsdaFood): JournalEntry? {
        val amount = when {
            parsed.amount?.unit == AmountUnit.GRAM -> parsed.amount
            parsed.targetCalories != null -> {
                val energyPer100g = food.nutrientsPer100g[NutrientKey.ENERGY]
                    ?.takeIf { it > 0.0 } ?: return null
                EntryAmount(parsed.targetCalories / energyPer100g * 100.0, AmountUnit.GRAM)
            }
            else -> return null
        }
        val nutrients = food.nutrientsForGrams(amount.value)
        return JournalEntry(
            id = UUID.randomUUID().toString(),
            name = parsed.name,
            kind = parsed.kind,
            mealType = parsed.mealType,
            servingDescription = "${amount.value.format1()} g",
            servingGrams = amount.value,
            amount = amount,
            nutrients = nutrients,
            note = if (parsed.targetCalories != null && parsed.amount == null) {
                "Weight inferred from ${parsed.targetCalories.format1()} kcal and the selected USDA reference; review before saving."
            } else {
                "Nutrition scaled from the selected USDA reference; review before saving."
            },
        )
    }

    private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
}