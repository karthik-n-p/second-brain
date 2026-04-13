package `in`.karthiknp.secondbrain.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import `in`.karthiknp.secondbrain.R

/**
 * BroadcastReceiver that fires when an alarm goes off.
 * Displays a local notification with the memory content.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEMORY_ID = "extra_memory_id"
        const val EXTRA_CONTENT = "extra_content"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val memoryId = intent.getLongExtra(EXTRA_MEMORY_ID, -1)
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "You have a reminder!"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🧠 Second Brain Reminder")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(memoryId.toInt(), notification)
    }
}
