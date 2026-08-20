package io.github.kanggod9.diettracker.reminder

import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.localDate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReminderMeal(
    val displayName: String,
    val mealType: MealType,
    val defaultTime: LocalTime,
    val requestCode: Int,
    val notificationId: Int,
) {
    BREAKFAST("Breakfast", MealType.BREAKFAST, LocalTime.of(8, 0), 3101, 4101),
    LUNCH("Lunch", MealType.LUNCH, LocalTime.of(12, 0), 3102, 4102),
    DINNER("Dinner", MealType.DINNER, LocalTime.of(18, 0), 3103, 4103),
}

data class MealReminderOption(val enabled: Boolean, val time: LocalTime)

data class MealReminderSettings(
    val enabled: Boolean,
    val meals: Map<ReminderMeal, MealReminderOption>,
) {
    fun option(meal: ReminderMeal): MealReminderOption =
        meals[meal] ?: MealReminderOption(enabled = true, time = meal.defaultTime)
}

internal fun nextReminderInstant(now: ZonedDateTime, time: LocalTime): Instant {
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return (if (today.isAfter(now)) today else today.plusDays(1)).toInstant()
}

internal fun shouldSendMealReminder(
    entries: List<JournalEntry>,
    meal: ReminderMeal,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean = entries.none { entry ->
    entry.mealType == meal.mealType &&
        entry.localDate(zone) == now.atZone(zone).toLocalDate() &&
        !entry.loggedAt.isAfter(now)
}