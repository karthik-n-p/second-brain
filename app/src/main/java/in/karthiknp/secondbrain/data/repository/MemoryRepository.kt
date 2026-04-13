package `in`.karthiknp.secondbrain.data.repository

import `in`.karthiknp.secondbrain.data.local.MemoryDao
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import kotlinx.coroutines.flow.Flow

/**
 * Repository that acts as a single source of truth for memory data.
 * Abstracts the data source (Room) from the domain/UI layers.
 */
class MemoryRepository(private val memoryDao: MemoryDao) {

    fun getAllMemories(): Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>> =
        memoryDao.getMemoriesByType(type)

    fun searchMemories(query: String): Flow<List<MemoryEntity>> =
        memoryDao.searchMemories(query)

    fun getUpcomingEvents(): Flow<List<MemoryEntity>> =
        memoryDao.getUpcomingEvents()

    fun getPendingTasks(): Flow<List<MemoryEntity>> =
        memoryDao.getPendingTasks()

    suspend fun getRandomIdea(): MemoryEntity? = memoryDao.getRandomIdea()

    suspend fun insert(memory: MemoryEntity): Long = memoryDao.insert(memory)

    suspend fun update(memory: MemoryEntity) = memoryDao.update(memory)

    suspend fun delete(memory: MemoryEntity) = memoryDao.delete(memory)

    suspend fun getMemoryById(id: Long): MemoryEntity? = memoryDao.getMemoryById(id)

    suspend fun getSchedulableMemories(): List<MemoryEntity> =
        memoryDao.getSchedulableMemories()
}
