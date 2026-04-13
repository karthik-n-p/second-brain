package `in`.karthiknp.secondbrain.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.data.repository.MemoryRepository
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 * Provides memory data grouped by type with filtering and actions.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MemoryRepository(db.memoryDao())

    // ─── State ───────────────────────────────────────────────────────

    val allMemories: StateFlow<List<MemoryEntity>> = repository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ideas: StateFlow<List<MemoryEntity>> = repository.getMemoriesByType(MemoryType.IDEA)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingTasks: StateFlow<List<MemoryEntity>> = repository.getPendingTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val upcomingEvents: StateFlow<List<MemoryEntity>> = repository.getUpcomingEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeFilter = MutableStateFlow<MemoryType?>(null)
    val activeFilter: StateFlow<MemoryType?> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredMemories: StateFlow<List<MemoryEntity>> = combine(
        allMemories, _activeFilter, _searchQuery
    ) { memories, filter, query ->
        var result = memories

        // Apply type filter
        if (filter != null) {
            result = result.filter { it.type == filter }
        }

        // Apply search
        if (query.isNotBlank()) {
            result = result.filter {
                it.content.contains(query, ignoreCase = true)
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Actions ─────────────────────────────────────────────────────

    fun setFilter(type: MemoryType?) {
        _activeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTaskCompletion(memory: MemoryEntity) {
        viewModelScope.launch {
            val newStatus = if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) {
                `in`.karthiknp.secondbrain.domain.model.TaskStatus.PENDING
            } else {
                `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED
            }
            repository.update(memory.copy(status = newStatus))
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.delete(memory)
        }
    }
}
