package `in`.karthiknp.secondbrain.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import `in`.karthiknp.secondbrain.MainActivity
import `in`.karthiknp.secondbrain.R
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MorningPlanWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val dao = db.memoryDao()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        val tasks = dao.getTasksForToday(startOfDay, endOfDay).first()
        val events = dao.getEventsForToday(startOfDay, endOfDay).first()

        if (tasks.isEmpty() && events.isEmpty()) {
            return Result.success()
        }

        val content = buildString {
            if (tasks.isNotEmpty()) append("📋 ${tasks.size} Task(s)\n")
            if (events.isNotEmpty()) append("📅 ${events.size} Event(s)\n")
            append("Tap to view your dashboard.")
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DAILY_PLAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌅 Good Morning! Here's your plan:")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }

        return Result.success()
    }
}
