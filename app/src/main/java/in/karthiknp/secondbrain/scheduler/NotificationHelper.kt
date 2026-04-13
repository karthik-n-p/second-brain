package `in`.karthiknp.secondbrain.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Creates notification channels required by the app.
 * Must be called at app startup (Application.onCreate).
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "second_brain_reminders"
    const val CHANNEL_IDEAS = "second_brain_ideas"
    const val CHANNEL_DAILY_PLAN = "second_brain_daily_plan"

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reminder notifications (high priority)
        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Task and event reminders"
            enableVibration(true)
        }

        // Idea resurfacing notifications (default priority)
        val ideaChannel = NotificationChannel(
            CHANNEL_IDEAS,
            "Idea Resurfacing",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Rediscover your old ideas"
        }

        // Daily plan notifications (default priority)
        val dailyPlanChannel = NotificationChannel(
            CHANNEL_DAILY_PLAN,
            "Daily Dashboard",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Morning plans and evening reviews"
        }

        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(ideaChannel)
        notificationManager.createNotificationChannel(dailyPlanChannel)
    }
}
