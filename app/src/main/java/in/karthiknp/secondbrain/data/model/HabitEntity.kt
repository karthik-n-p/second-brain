package `in`.karthiknp.secondbrain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.karthiknp.secondbrain.domain.model.HabitFrequency

/**
 * Represents a tracked habit.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val streakCount: Int = 0,
    val lastCompletedDate: String? = null, // "2026-04-13"
    val createdAt: Long = System.currentTimeMillis()
)
