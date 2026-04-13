package `in`.karthiknp.secondbrain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks daily app usage for global streak calculation.
 * One row per active day.
 */
@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey
    val dateString: String,  // "2026-04-13"
    val openedAt: Long = System.currentTimeMillis()
)
