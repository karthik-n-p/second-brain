package `in`.karthiknp.secondbrain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.TaskStatus

/**
 * Room entity representing a single memory (idea, task, or event).
 *
 * @param id Auto-generated primary key.
 * @param type The classification of this memory.
 * @param content The raw user input / cleaned content.
 * @param datetime Optional timestamp for tasks and events (epoch millis).
 * @param metadata JSON string for extra key-value pairs (e.g., person name for birthdays).
 * @param status Lifecycle status of the task (PENDING, COMPLETED, CANCELLED).
 * @param tags Comma-separated list of tags extracted from the content.
 * @param isRecurring Whether an event recurs annually (e.g., birthdays).
 * @param createdAt Timestamp of when this memory was captured (epoch millis).
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: MemoryType,
    val content: String,
    val datetime: Long? = null,
    val metadata: String = "{}",
    val status: TaskStatus = TaskStatus.PENDING,
    val tags: String = "",
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
