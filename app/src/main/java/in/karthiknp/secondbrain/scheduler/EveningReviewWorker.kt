package `in`.karthiknp.secondbrain.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import `in`.karthiknp.secondbrain.R
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import java.util.Calendar

class EveningReviewWorker(
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

        val pendingTasks = dao.getPendingTasksForToday(startOfDay, endOfDay)

        if (pendingTasks.isEmpty()) {
            return Result.success()
        }

        // Prompt for the first pending task
        val task = pendingTasks.first()

        val completeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_MARK_COMPLETED
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), completeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_MARK_CANCELLED
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt() + 1000, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = "Did you complete: \"${task.content}\"?"

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DAILY_PLAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 Evening Review")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Yes", completePendingIntent)
            .addAction(0, "Not Yet", cancelPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }

        return Result.success()
    }
}
