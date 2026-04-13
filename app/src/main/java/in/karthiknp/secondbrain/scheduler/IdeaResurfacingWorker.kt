package `in`.karthiknp.secondbrain.scheduler

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import `in`.karthiknp.secondbrain.R
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.repository.MemoryRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that runs once daily to resurface a random old idea.
 * This is the "Idea Resurfacing" feature — keeps ideas alive.
 */
class IdeaResurfacingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val repository = MemoryRepository(db.memoryDao())

        val idea = repository.getRandomIdea() ?: return Result.success()

        val age = System.currentTimeMillis() - idea.createdAt
        val daysAgo = TimeUnit.MILLISECONDS.toDays(age)

        val timeAgo = when {
            daysAgo == 0L -> "today"
            daysAgo == 1L -> "yesterday"
            daysAgo < 30 -> "$daysAgo days ago"
            daysAgo < 365 -> "${daysAgo / 30} months ago"
            else -> "on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(idea.createdAt))}"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_IDEAS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💡 Remember this idea?")
            .setContentText("You captured this $timeAgo: \"${idea.content}\"")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You captured this $timeAgo:\n\n\"${idea.content}\"")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 9999
        const val WORK_NAME = "idea_resurfacing_worker"

        /**
         * Enqueue a periodic work request to run once per day.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<IdeaResurfacingWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(12, TimeUnit.HOURS) // First run after 12 hours
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
