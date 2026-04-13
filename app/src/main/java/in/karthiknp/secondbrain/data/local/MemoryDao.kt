package `in`.karthiknp.secondbrain.data.local

import androidx.room.*
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the memories table.
 * Provides reactive queries via Flow and suspend functions for writes.
 */
@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    /** Get all memories ordered by newest first. */
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    /** Get memories filtered by type. */
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY createdAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>>

    /** Search content with LIKE (simple text search). */
    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    /** Get upcoming events (datetime in the future). */
    @Query("SELECT * FROM memories WHERE type = 'EVENT' AND datetime IS NOT NULL AND datetime >= :now ORDER BY datetime ASC")
    fun getUpcomingEvents(now: Long = System.currentTimeMillis()): Flow<List<MemoryEntity>>

    /** Get incomplete tasks. */
    @Query("SELECT * FROM memories WHERE type = 'TASK' AND status = 'PENDING' ORDER BY datetime ASC, createdAt DESC")
    fun getPendingTasks(): Flow<List<MemoryEntity>>

    /** Get a random idea for resurfacing. */
    @Query("SELECT * FROM memories WHERE type = 'IDEA' ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomIdea(): MemoryEntity?

    /** Get a specific memory by id. */
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    /** Get all memories with reminders that need scheduling. */
    @Query("SELECT * FROM memories WHERE datetime IS NOT NULL AND status = 'PENDING'")
    suspend fun getSchedulableMemories(): List<MemoryEntity>

    /** Get tasks scheduled for today. */
    @Query("SELECT * FROM memories WHERE type = 'TASK' AND datetime >= :startOfDay AND datetime <= :endOfDay ORDER BY datetime ASC")
    fun getTasksForToday(startOfDay: Long, endOfDay: Long): Flow<List<MemoryEntity>>

    /** Get pending tasks scheduled for today (for evening review). */
    @Query("SELECT * FROM memories WHERE type = 'TASK' AND status = 'PENDING' AND datetime >= :startOfDay AND datetime <= :endOfDay")
    suspend fun getPendingTasksForToday(startOfDay: Long, endOfDay: Long): List<MemoryEntity>

    /** Get events scheduled for today. */
    @Query("SELECT * FROM memories WHERE type = 'EVENT' AND datetime >= :startOfDay AND datetime <= :endOfDay ORDER BY datetime ASC")
    fun getEventsForToday(startOfDay: Long, endOfDay: Long): Flow<List<MemoryEntity>>
}
