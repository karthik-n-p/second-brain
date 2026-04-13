package `in`.karthiknp.secondbrain.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.model.*
import `in`.karthiknp.secondbrain.domain.model.HabitFrequency
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.TaskStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()
    private val habitDao = db.habitDao()
    private val journalDao = db.journalDao()

    private val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // ─── Journal ────────────────────────────────────────────────────

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText.asStateFlow()

    private var journalEntryId: Long = 0L

    // ─── Habits ─────────────────────────────────────────────────────

    val habits: StateFlow<List<HabitEntity>> = habitDao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todayCompletions: StateFlow<List<HabitCompletionEntity>> =
        habitDao.getCompletionsForDate(today)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Tasks (today) ──────────────────────────────────────────────

    val todayTasks: StateFlow<List<MemoryEntity>> = memoryDao.getPendingTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Global Streak ──────────────────────────────────────────────

    private val _globalStreak = MutableStateFlow(0)
    val globalStreak: StateFlow<Int> = _globalStreak.asStateFlow()

    private val _activeDays = MutableStateFlow<Set<String>>(emptySet())
    val activeDays: StateFlow<Set<String>> = _activeDays.asStateFlow()

    // ─── Init ───────────────────────────────────────────────────────

    init {
        // Record today's app usage
        viewModelScope.launch {
            journalDao.recordAppUsage(AppUsageEntity(dateString = today))
            loadGlobalStreak()
            loadJournal()
        }
    }

    private suspend fun loadJournal() {
        journalDao.getEntryForDate(today).collect { entry ->
            if (entry != null) {
                journalEntryId = entry.id
                // Only update if user hasn't started typing
                if (_journalText.value.isEmpty() && entry.content.isNotEmpty()) {
                    _journalText.value = entry.content
                }
            }
        }
    }

    private suspend fun loadGlobalStreak() {
        val dates = journalDao.getAllUsageDateStrings()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sortedDescending()

        _activeDays.value = dates.map { it.toString() }.toSet()

        if (dates.isEmpty()) {
            _globalStreak.value = 0
            return
        }

        var streak = 1
        for (i in 0 until dates.size - 1) {
            if (ChronoUnit.DAYS.between(dates[i + 1], dates[i]) == 1L) {
                streak++
            } else {
                break
            }
        }
        _globalStreak.value = streak
    }

    // ─── Actions ────────────────────────────────────────────────────

    fun updateJournalText(text: String) {
        _journalText.value = text
    }

    fun saveJournal() {
        val text = _journalText.value
        viewModelScope.launch {
            val entry = JournalEntry(
                id = journalEntryId,
                dateString = today,
                content = text,
                updatedAt = System.currentTimeMillis()
            )
            journalEntryId = journalDao.upsert(entry)
        }
    }

    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            habitDao.insertHabit(
                HabitEntity(name = name.trim(), frequency = HabitFrequency.DAILY)
            )
        }
    }

    fun toggleHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val isCompleted = habitDao.isCompletedOnDate(habit.id, today)
            if (isCompleted) {
                habitDao.unmarkCompleted(habit.id, today)
                // Recalculate streak
                val newStreak = calculateStreak(habit.id)
                habitDao.updateHabit(habit.copy(
                    streakCount = newStreak,
                    lastCompletedDate = if (newStreak > 0) {
                        getLastCompletionDate(habit.id)
                    } else null
                ))
            } else {
                habitDao.markCompleted(HabitCompletionEntity(habitId = habit.id, dateString = today))
                // Update streak
                val newStreak = calculateStreak(habit.id)
                habitDao.updateHabit(habit.copy(
                    streakCount = newStreak,
                    lastCompletedDate = today
                ))
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }

    private suspend fun calculateStreak(habitId: Long): Int {
        val history = habitDao.getCompletionHistory(habitId)
            .map { LocalDate.parse(it.dateString) }
            .sortedDescending()

        if (history.isEmpty()) return 0

        var streak = 1
        for (i in 0 until history.size - 1) {
            if (ChronoUnit.DAYS.between(history[i + 1], history[i]) == 1L) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private suspend fun getLastCompletionDate(habitId: Long): String? {
        return habitDao.getCompletionHistory(habitId).firstOrNull()?.dateString
    }

    fun addTask(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            memoryDao.insert(
                MemoryEntity(
                    type = MemoryType.TASK,
                    content = content.trim(),
                    datetime = System.currentTimeMillis(),
                    status = TaskStatus.PENDING
                )
            )
        }
    }

    fun toggleTask(task: MemoryEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.COMPLETED) {
                TaskStatus.PENDING
            } else {
                TaskStatus.COMPLETED
            }
            memoryDao.update(task.copy(status = newStatus))
        }
    }

    fun deleteTask(task: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.delete(task)
        }
    }
}
