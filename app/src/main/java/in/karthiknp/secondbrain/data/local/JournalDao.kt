package `in`.karthiknp.secondbrain.data.local

import androidx.room.*
import `in`.karthiknp.secondbrain.data.model.AppUsageEntity
import `in`.karthiknp.secondbrain.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntry): Long

    @Query("SELECT * FROM journal_entries WHERE dateString = :dateString LIMIT 1")
    fun getEntryForDate(dateString: String): Flow<JournalEntry?>

    @Query("SELECT * FROM journal_entries ORDER BY dateString DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    // ─── App Usage (Global Streak) ──────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordAppUsage(usage: AppUsageEntity)

    @Query("SELECT * FROM app_usage ORDER BY dateString DESC")
    suspend fun getAllUsageDates(): List<AppUsageEntity>

    @Query("SELECT dateString FROM app_usage ORDER BY dateString DESC")
    suspend fun getAllUsageDateStrings(): List<String>
}
