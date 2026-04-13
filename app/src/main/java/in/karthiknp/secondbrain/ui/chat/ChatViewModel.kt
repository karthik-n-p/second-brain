package `in`.karthiknp.secondbrain.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.karthiknp.secondbrain.ai.LlamaEngine
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.repository.MemoryRepository
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.usecase.ProcessResult
import `in`.karthiknp.secondbrain.domain.usecase.ProcessUserInputUseCase
import `in`.karthiknp.secondbrain.parser.QueryIntent
import `in`.karthiknp.secondbrain.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the main chat screen.
 * Manages chat messages, user input, LLM state, and coordinates with the use case layer.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MemoryRepository(db.memoryDao())
    private val alarmScheduler = AlarmScheduler(application)
    private val llmEngine = LlamaEngine.getInstance()

    private val processInputUseCase = ProcessUserInputUseCase(
        context = application,
        repository = repository,
        alarmScheduler = alarmScheduler,
        llmEngine = llmEngine
    )

    // ─── State ───────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "👋 Hey! I'm your Second Brain.\n\nCapture ideas, tasks, and events just by typing naturally.\n\nTry:\n• \"Idea: build a scraping API\"\n• \"Remind me to study Kafka at 9pm\"\n• \"Arjun birthday Jan 12\"\n• \"Show my ideas\"",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Expose LLM state to the UI
    val llmState: StateFlow<LlamaEngine.ModelState> = llmEngine.modelState

    // ─── Initialization ─────────────────────────────────────────────

    init {
        // Initialize the LLM engine in the background (non-blocking)
        viewModelScope.launch {
            if (llmEngine.isModelDownloaded(getApplication())) {
                llmEngine.initialize(getApplication())

                val stateMessage = when (llmEngine.modelState.value) {
                    is LlamaEngine.ModelState.Ready -> {
                        ChatMessage(
                            text = "🧠 AI engine loaded! I'll generate intelligent responses.",
                            isUser = false
                        )
                    }
                    is LlamaEngine.ModelState.Error -> {
                        val err = (llmEngine.modelState.value as LlamaEngine.ModelState.Error).message
                        ChatMessage(
                            text = "⚠️ AI engine issue: $err\n\nUsing template responses.",
                            isUser = false
                        )
                    }
                    else -> null
                }

                if (stateMessage != null) {
                    _messages.value = _messages.value + stateMessage
                }
            }
            // If model not downloaded, silently use template mode — no error shown
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        // Add user message
        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = processInputUseCase.execute(text)

                when (result) {
                    is ProcessResult.Message -> {
                        val botMessage = ChatMessage(
                            text = result.text,
                            isUser = false,
                            memoryType = detectTypeFromResponse(result.text)
                        )
                        _messages.value = _messages.value + botMessage
                    }
                    is ProcessResult.QueryResult -> {
                        handleQueryResult(result)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "⚠️ Something went wrong: ${e.localizedMessage}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun handleQueryResult(result: ProcessResult.QueryResult) {
        viewModelScope.launch {
            result.memories.take(1).collect { memories ->
                val response = if (memories.isEmpty()) {
                    "🔍 No results found."
                } else {
                    val header = when (result.intent) {
                        is QueryIntent.FilterByType -> {
                            when (result.intent.type) {
                                MemoryType.IDEA -> "💡 **Your Ideas** (${memories.size}):"
                                MemoryType.TASK -> "✅ **Pending Tasks** (${memories.size}):"
                                MemoryType.EVENT -> "📅 **Upcoming Events** (${memories.size}):"
                            }
                        }
                        is QueryIntent.Search -> "🔍 **Search Results** (${memories.size}):"
                        is QueryIntent.ShowAll -> "📋 **All Memories** (${memories.size}):"
                        is QueryIntent.Conversational -> "🗣️ **Conversation**:"
                    }

                    val items = memories.take(10).joinToString("\n") { memory ->
                        val emoji = when (memory.type) {
                            MemoryType.IDEA -> "💡"
                            MemoryType.TASK -> if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) "☑️" else "⬜"
                            MemoryType.EVENT -> "📅"
                        }
                        val timeStr = memory.datetime?.let {
                            val sdf = java.text.SimpleDateFormat(
                                "MMM dd", java.util.Locale.getDefault()
                            )
                            " • ${sdf.format(java.util.Date(it))}"
                        } ?: ""
                        "$emoji ${memory.content}$timeStr"
                    }

                    val more = if (memories.size > 10) "\n\n...and ${memories.size - 10} more" else ""
                    "$header\n\n$items$more"
                }

                val botMessage = ChatMessage(text = response, isUser = false)
                _messages.value = _messages.value + botMessage
            }
        }
    }

    private fun detectTypeFromResponse(text: String): MemoryType? {
        return when {
            text.contains("💡") -> MemoryType.IDEA
            text.contains("✅") || text.contains("Task") || text.contains("task") -> MemoryType.TASK
            text.contains("📅") || text.contains("Event") || text.contains("event") -> MemoryType.EVENT
            else -> null
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't release the engine here since it's a singleton shared across the app
    }
}
