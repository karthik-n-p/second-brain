package `in`.karthiknp.secondbrain.domain.usecase

import android.content.Context
import com.google.gson.Gson
import `in`.karthiknp.secondbrain.ai.AiResponseGenerator
import `in`.karthiknp.secondbrain.ai.LlamaEngine
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.data.repository.MemoryRepository
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.ParsedInput
import `in`.karthiknp.secondbrain.parser.QueryIntent
import `in`.karthiknp.secondbrain.parser.RuleBasedParser
import `in`.karthiknp.secondbrain.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Primary use case that orchestrates the full input pipeline:
 *
 *   User Input → Rule-based parser → Structured storage → Scheduler (if needed)
 *              → AI Response Generator (if LLM loaded) or Template fallback
 *
 * The AI engine is used for:
 *   1. Generating natural, conversational responses after saving
 *   2. Summarizing query results intelligently
 *   3. Handling freeform conversation when no pattern matches
 */
class ProcessUserInputUseCase(
    private val context: Context,
    private val repository: MemoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val llmEngine: LlamaEngine,
    private val gson: Gson = Gson()
) {

    /**
     * Process a raw user input string.
     * @return A response message to show in the chat UI.
     */
    suspend fun execute(input: String): ProcessResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ProcessResult.Message("Please type something!")
        }

        // 1. Check if this is a query (retrieval intent)
        val queryIntent = RuleBasedParser.detectQueryIntent(trimmed)
        if (queryIntent != null) {
            return handleQuery(trimmed, queryIntent)
        }

        // 2. Parse as a new memory
        var parsed = RuleBasedParser.parse(trimmed)

        // 2.5 AI Fallback for ambiguity
        if (parsed.confidence < 0.6f && llmEngine.isReady) {
            val aiParsed = AiResponseGenerator.fallbackParseWithLlm(trimmed, llmEngine, gson)
            if (aiParsed != null) {
                parsed = aiParsed
            }
        }

        // 3. Store in database
        val entity = MemoryEntity(
            type = parsed.type,
            content = parsed.content,
            datetime = parsed.datetime,
            tags = parsed.tags.joinToString(","),
            metadata = gson.toJson(parsed.metadata),
            isRecurring = parsed.metadata["eventType"] == "birthday"
        )
        val id = repository.insert(entity)

        // 4. Schedule reminder if needed
        if (parsed.datetime != null && parsed.type in listOf(MemoryType.TASK, MemoryType.EVENT)) {
            val savedEntity = entity.copy(id = id)
            alarmScheduler.scheduleReminder(savedEntity)
        }

        // 5. Generate AI response (or fallback to template)
        val response = AiResponseGenerator.generateSaveResponse(context, parsed, llmEngine)
        return ProcessResult.Message(response)
    }

    /**
     * Handle retrieval queries with AI-powered summarization.
     */
    private suspend fun handleQuery(
        originalQuery: String,
        intent: QueryIntent
    ): ProcessResult {
        // Handle conversational separately to avoid treating it like a query flow
        if (intent is QueryIntent.Conversational) {
            if (llmEngine.isReady) {
                val recent = repository.getAllMemories().first().take(5)
                val aiResponse = AiResponseGenerator.generateConversationalResponse(
                    context, intent.text, recent, llmEngine
                )
                if (aiResponse != null) {
                    return ProcessResult.Message(aiResponse)
                }
            }
            return ProcessResult.Message("👋 Hey! I'm your Second Brain. I don't save greetings, but I'm ready to capture your ideas, tasks, and events.")
        }

        val flow: Flow<List<MemoryEntity>> = when (intent) {
            is QueryIntent.FilterByType -> {
                when (intent.type) {
                    MemoryType.TASK -> repository.getPendingTasks()
                    MemoryType.EVENT -> repository.getUpcomingEvents()
                    MemoryType.IDEA -> repository.getMemoriesByType(MemoryType.IDEA)
                }
            }
            is QueryIntent.Search -> repository.searchMemories(intent.query)
            is QueryIntent.ShowAll -> repository.getAllMemories()
            is QueryIntent.Conversational -> throw IllegalStateException("Handled above")
        }

        // If LLM is ready, collect results and generate an AI summary
        if (llmEngine.isReady) {
            try {
                val results = flow.first()
                val aiResponse = AiResponseGenerator.generateQueryResponse(
                    context, originalQuery, results, llmEngine
                )
                return ProcessResult.Message(aiResponse)
            } catch (e: Exception) {
                // Fall through to returning the flow
            }
        }

        // Fallback: return the flow for the ViewModel to format
        return ProcessResult.QueryResult(flow, intent)
    }
}

/**
 * Represents the result of processing user input.
 */
sealed class ProcessResult {
    data class Message(val text: String) : ProcessResult()
    data class QueryResult(
        val memories: Flow<List<MemoryEntity>>,
        val intent: QueryIntent
    ) : ProcessResult()
}
