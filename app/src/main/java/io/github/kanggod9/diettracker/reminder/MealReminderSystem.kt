package io.github.kanggod9.diettracker.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.kanggod9.diettracker.MainActivity
import io.github.kanggod9.diettracker.R
import io.github.kanggod9.diettracker.data.JournalRepository
import io.github.kanggod9.diettracker.data.LocalStore
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MealReminderPreferences {
    private const val MASTER_KEY = "meal_reminders.enabled"

    fun load(repository: JournalRepository): MealReminderSettings = MealReminderSettings(
        enabled = repository.setting(MASTER_KEY) == "true",
        meals = ReminderMeal.entries.associateWith { meal ->
            MealReminderOption(
                enabled = repository.setting(enabledKey(meal))?.toBooleanStrictOrNull() ?: true,
                time = repository.setting(timeKey(meal))?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?: meal.defaultTime,
            )
        },
    )

    fun setMasterEnabled(repository: JournalRepository, enabled: Boolean) {
        repository.setSetting(MASTER_KEY, enabled.toString())
    }

    fun setMealEnabled(repository: JournalRepository, meal: ReminderMeal, enabled: Boolean) {
        repository.setSetting(enabledKey(meal), enabled.toString())
    }

    fun setMealTime(repository: JournalRepository, meal: ReminderMeal, time: LocalTime) {
        repository.setSetting(timeKey(meal), time.toString())
    }

    private fun enabledKey(meal: ReminderMeal) = "meal_reminders.${meal.name.lowercase()}.enabled"
    private fun timeKey(meal: ReminderMeal) = "meal_reminders.${meal.name.lowercase()}.time"
}

fun notificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

object MealReminderScheduler {
    const val ACTION_REMIND = "io.github.kanggod9.diettracker.action.MEAL_REMINDER"
    const val EXTRA_MEAL = "meal"

    fun sync(context: Context, repository: JournalRepository = LocalStore(context.applicationContext)) {
        val settings = MealReminderPreferences.load(repository)
        ReminderMeal.entries.forEach { meal ->
            val option = settings.option(meal)
            if (settings.enabled && option.enabled) schedule(context, meal, option.time) else cancel(context, meal)
        }
    }

    fun schedule(
        context: Context,
        meal: ReminderMeal,
        time: LocalTime,
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextReminderInstant(now, time).toEpochMilli(),
            pendingIntent(context, meal),
        )
    }

    fun cancel(context: Context, meal: ReminderMeal) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, meal))
    }

    private fun pendingIntent(context: Context, meal: ReminderMeal): PendingIntent = PendingIntent.getBroadcast(
        context,
        meal.requestCode,
        Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_MEAL, meal.name)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MealReminderScheduler.ACTION_REMIND) return
        val meal = intent.getStringExtra(MealReminderScheduler.EXTRA_MEAL)
            ?.let { runCatching { ReminderMeal.valueOf(it) }.getOrNull() }
            ?: return
        val store = LocalStore(context.applicationContext)
        val settings = MealReminderPreferences.load(store)
        val option = settings.option(meal)
        val now = Instant.now()
        if (settings.enabled && option.enabled) {
            if (notificationPermissionGranted(context) && shouldSendMealReminder(store.entries(), meal, now)) {
                showNotification(context, meal)
            }
            MealReminderScheduler.schedule(
                context,
                meal,
                option.time,
                ZonedDateTime.ofInstant(now.plusSeconds(1), ZoneId.systemDefault()),
            )
        }
    }

    private fun showNotification(context: Context, meal: ReminderMeal) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Meal reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders to record breakfast, lunch, and dinner"
            },
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Log ${meal.displayName.lowercase()}")
            .setContentText("Remember to record your ${meal.displayName.lowercase()} in Diet Tracker.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(meal.notificationId, notification)
    }

    private companion object {
        const val CHANNEL_ID = "meal_reminders"
    }
}

class MealReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MealReminderScheduler.sync(context.applicationContext)
    }
}
