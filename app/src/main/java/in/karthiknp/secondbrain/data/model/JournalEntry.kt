package `in`.karthiknp.secondbrain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily journal entry — one per day.
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String,  // "2026-04-13"
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
