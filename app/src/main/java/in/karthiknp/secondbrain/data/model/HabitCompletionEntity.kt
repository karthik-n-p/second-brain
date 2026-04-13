package `in`.karthiknp.secondbrain.data.model

import androidx.room.Entity

/**
 * Tracks which habit was completed on which day.
 * Composite primary key of (habitId, dateString).
 */
@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dateString"]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val dateString: String,  // "2026-04-13"
    val completedAt: Long = System.currentTimeMillis()
)
