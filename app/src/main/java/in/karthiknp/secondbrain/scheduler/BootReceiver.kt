package `in`.karthiknp.secondbrain.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.repository.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver that triggers on device boot to reschedule all pending alarms.
 * Alarms are lost when the device reboots, so we must restore them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getInstance(context)
            val repository = MemoryRepository(db.memoryDao())
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val memories = repository.getSchedulableMemories()
                memories.forEach { memory ->
                    scheduler.scheduleReminder(memory)
                }
            }
        }
    }
}
