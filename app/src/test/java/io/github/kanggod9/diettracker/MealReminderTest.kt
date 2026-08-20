package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.reminder.ReminderMeal
import io.github.kanggod9.diettracker.reminder.nextReminderInstant
import io.github.kanggod9.diettracker.reminder.shouldSendMealReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MealReminderTest {
    private val zone = ZoneId.of("Asia/Singapore")

    private fun entry(meal: MealType, loggedAt: String) = JournalEntry(
        name = "Meal",
        kind = EntryKind.FOOD,
        mealType = meal,
        servingDescription = "1 serving",
        servingGrams = null,
        loggedAt = Instant.parse(loggedAt),
        nutrients = Nutrients(),
    )

    @Test fun defaultsMatchBreakfastLunchAndDinnerRequirements() {
        assertEquals(LocalTime.of(8, 0), ReminderMeal.BREAKFAST.defaultTime)
        assertEquals(LocalTime.of(12, 0), ReminderMeal.LUNCH.defaultTime)
        assertEquals(LocalTime.of(18, 0), ReminderMeal.DINNER.defaultTime)
    }

    @Test fun schedulingSelectsTheNextOccurrenceInTheDeviceZone() {
        val morning = ZonedDateTime.of(2026, 8, 20, 7, 30, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 8, 20, 8, 0, 0, 0, zone).toInstant(),
            nextReminderInstant(morning, LocalTime.of(8, 0)),
        )
        val afternoon = ZonedDateTime.of(2026, 8, 20, 13, 0, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 8, 21, 8, 0, 0, 0, zone).toInstant(),
            nextReminderInstant(afternoon, LocalTime.of(8, 0)),
        )
    }

    @Test fun onlyAnAlreadyLoggedMatchingMealSuppressesTheReminder() {
        val now = Instant.parse("2026-08-20T04:00:00Z") // noon in Singapore
        assertFalse(
            shouldSendMealReminder(
                listOf(entry(MealType.LUNCH, "2026-08-20T03:00:00Z")),
                ReminderMeal.LUNCH,
                now,
                zone,
            ),
        )
        assertTrue(
            shouldSendMealReminder(
                listOf(entry(MealType.BREAKFAST, "2026-08-20T00:00:00Z")),
                ReminderMeal.LUNCH,
                now,
                zone,
            ),
        )
        assertTrue(
            shouldSendMealReminder(
                listOf(entry(MealType.LUNCH, "2026-08-19T03:00:00Z")),
                ReminderMeal.LUNCH,
                now,
                zone,
            ),
        )
    }
}
