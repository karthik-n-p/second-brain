package `in`.karthiknp.secondbrain.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MARK_COMPLETED = "mark_completed"
        const val ACTION_MARK_CANCELLED = "mark_cancelled"
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val action = intent.action ?: return
        
        // Dismiss notification
        NotificationManagerCompat.from(context).cancel(taskId.toInt())

        val db = AppDatabase.getInstance(context)
        val dao = db.memoryDao()

        CoroutineScope(Dispatchers.IO).launch {
            val memory = dao.getMemoryById(taskId) ?: return@launch
            
            val newStatus = when (action) {
                ACTION_MARK_COMPLETED -> TaskStatus.COMPLETED
                ACTION_MARK_CANCELLED -> TaskStatus.CANCELLED
                else -> return@launch
            }
            
            dao.update(memory.copy(status = newStatus))
        }
    }
}
