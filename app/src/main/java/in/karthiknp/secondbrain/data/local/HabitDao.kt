package `in`.karthiknp.secondbrain.data.local

import androidx.room.*
import `in`.karthiknp.secondbrain.data.model.HabitCompletionEntity
import `in`.karthiknp.secondbrain.data.model.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    // ─── Completions ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markCompleted(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun unmarkCompleted(habitId: Long, dateString: String)

    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateString DESC")
    suspend fun getCompletionHistory(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString)")
    suspend fun isCompletedOnDate(habitId: Long, dateString: String): Boolean

    /** Get all completion dates for the calendar heatmap. */
    @Query("SELECT DISTINCT dateString FROM habit_completions ORDER BY dateString DESC")
    suspend fun getAllCompletionDates(): List<String>
}
