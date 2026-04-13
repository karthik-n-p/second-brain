package `in`.karthiknp.secondbrain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import java.util.Calendar

/**
 * Schedules exact-time alarms for tasks and events.
 *
 * Uses AlarmManager for precise delivery even when the app is closed.
 * For recurring events (birthdays), schedules the next occurrence automatically.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule a reminder for a memory entity.
     * Only schedules if the entity has a valid future datetime.
     */
    fun scheduleReminder(memory: MemoryEntity) {
        val triggerTime = memory.datetime ?: return

        // Don't schedule past reminders (unless recurring)
        if (triggerTime <= System.currentTimeMillis()) {
            if (memory.isRecurring) {
                // For recurring events, schedule for next year
                val cal = Calendar.getInstance().apply { timeInMillis = triggerTime }
                val now = Calendar.getInstance()
                while (cal.before(now)) {
                    cal.add(Calendar.YEAR, 1)
                }
                scheduleExactAlarm(memory.id, cal.timeInMillis, memory.content)
            }
            return
        }

        scheduleExactAlarm(memory.id, triggerTime, memory.content)
    }

    /**
     * Cancel a previously scheduled reminder.
     */
    fun cancelReminder(memoryId: Long) {
        val intent = createAlarmIntent(memoryId, "")
        alarmManager.cancel(intent)
        intent.cancel()
    }

    private fun scheduleExactAlarm(memoryId: Long, triggerTime: Long, content: String) {
        val pendingIntent = createAlarmIntent(memoryId, content)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if exact scheduling is not permitted
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun createAlarmIntent(memoryId: Long, content: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEMORY_ID, memoryId)
            putExtra(ReminderReceiver.EXTRA_CONTENT, content)
        }
        return PendingIntent.getBroadcast(
            context,
            memoryId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
