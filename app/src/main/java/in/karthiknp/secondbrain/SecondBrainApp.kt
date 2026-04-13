package `in`.karthiknp.secondbrain

import android.app.Application
import androidx.work.*
import `in`.karthiknp.secondbrain.scheduler.IdeaResurfacingWorker
import `in`.karthiknp.secondbrain.scheduler.NotificationHelper
import `in`.karthiknp.secondbrain.scheduler.MorningPlanWorker
import `in`.karthiknp.secondbrain.scheduler.EveningReviewWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Application class for Second Brain.
 * Initializes notification channels and background workers at startup.
 */
class SecondBrainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Start periodic idea resurfacing
        IdeaResurfacingWorker.enqueue(this)
        
        // Schedule proactive daily workers
        scheduleDailyWorkers()
    }

    private fun scheduleDailyWorkers() {
        val workManager = WorkManager.getInstance(this)
        
        // 8 AM Morning Plan
        val morningDelay = calculateDelayToHour(8)
        val morningRequest = PeriodicWorkRequestBuilder<MorningPlanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(morningDelay, TimeUnit.MILLISECONDS)
            .build()
            
        // 8 PM Evening Review
        val eveningDelay = calculateDelayToHour(20)
        val eveningRequest = PeriodicWorkRequestBuilder<EveningReviewWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(eveningDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork("MorningPlan", ExistingPeriodicWorkPolicy.UPDATE, morningRequest)
        workManager.enqueueUniquePeriodicWork("EveningReview", ExistingPeriodicWorkPolicy.UPDATE, eveningRequest)
    }

    private fun calculateDelayToHour(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
