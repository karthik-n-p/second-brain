package `in`.karthiknp.secondbrain.ai

import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.ParsedInput
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds contextual prompts for the LLM and generates natural responses.
 *
 * This is the intelligence layer that turns structured data
 * (parsed inputs, stored memories, query results) into natural
 * conversational responses using the on-device LLM.
 *
 * When the LLM is NOT available, it falls back to smart template responses.
 *
 * Prompt format: Qwen2.5 chat template (<|im_start|> / <|im_end|>)
 */
object AiResponseGenerator {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)

    // ─── System Prompt ──────────────────────────────────────────────

    private const val SYSTEM_PROMPT = "You are Second Brain, a friendly and concise personal assistant running on-device. You help users manage their ideas, tasks, and events. Keep responses short (1-3 sentences), warm, and helpful. Use relevant emojis sparingly. Never mention being an AI or LLM."

    /**
     * Wraps a prompt in Qwen2.5's chat template format.
     */
    private fun buildChatPrompt(userMessage: String): String {
        return "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
    }

    // ─── Save Responses ─────────────────────────────────────────────

    /**
     * Generate a response after saving a new memory.
     */
    suspend fun generateSaveResponse(
        context: Context,
        parsed: ParsedInput,
        engine: LlamaEngine
    ): String {
        if (!engine.isReady) {
            return fallbackSaveResponse(parsed)
        }

        val prompt = buildSavePrompt(parsed)
        val response = engine.generateResponse(prompt)
        return response?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackSaveResponse(parsed)
    }

    private fun buildSavePrompt(parsed: ParsedInput): String {
        val typeLabel = when (parsed.type) {
            MemoryType.IDEA -> "idea"
            MemoryType.TASK -> "task"
            MemoryType.EVENT -> "event"
        }

        val timeInfo = parsed.datetime?.let {
            " scheduled for ${dateTimeFormat.format(Date(it))}"
        } ?: ""

        return buildChatPrompt(
            "I just saved a new $typeLabel: \"${parsed.content}\"$timeInfo.\n\nAcknowledge this naturally in 1-2 sentences. Be encouraging for ideas, action-oriented for tasks, and helpful for events."
        )
    }

    private fun fallbackSaveResponse(parsed: ParsedInput): String {
        val timeStr = parsed.datetime?.let {
            "\n⏰ ${dateTimeFormat.format(Date(it))}"
        } ?: ""

        return when (parsed.type) {
            MemoryType.IDEA -> "💡 Great idea! I've saved: \"${parsed.content}\"$timeStr"
            MemoryType.TASK -> "✅ Task locked in: \"${parsed.content}\"$timeStr"
            MemoryType.EVENT -> "📅 Event noted: \"${parsed.content}\"$timeStr"
        }
    }

    // ─── Query Responses ────────────────────────────────────────────

    /**
     * Generate a response for a query that returned results.
     */
    suspend fun generateQueryResponse(
        context: Context,
        queryText: String,
        results: List<MemoryEntity>,
        engine: LlamaEngine
    ): String {
        if (results.isEmpty()) {
            return generateEmptyQueryResponse(context, queryText, engine)
        }

        if (!engine.isReady) {
            return fallbackQueryResponse(results)
        }

        val prompt = buildQueryPrompt(queryText, results)
        val response = engine.generateResponse(prompt)
        return response?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackQueryResponse(results)
    }

    private suspend fun generateEmptyQueryResponse(
        context: Context,
        queryText: String,
        engine: LlamaEngine
    ): String {
        if (!engine.isReady) {
            return "🔍 I couldn't find anything matching \"$queryText\". Try saving something first!"
        }

        val prompt = buildChatPrompt(
            "I asked: \"$queryText\" but you found no matching memories. Respond naturally in 1 sentence, suggesting I can save information by typing it."
        )

        return engine.generateResponse(prompt)?.trim()?.takeIf { it.isNotEmpty() }
            ?: "🔍 Nothing found for \"$queryText\". Try saving something first!"
    }

    private fun buildQueryPrompt(queryText: String, results: List<MemoryEntity>): String {
        val memorySummary = results.take(10).joinToString("\n") { memory ->
            val type = when (memory.type) {
                MemoryType.IDEA -> "💡 Idea"
                MemoryType.TASK -> if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) "✅ Done" else "📋 Task"
                MemoryType.EVENT -> "📅 Event"
            }
            val time = memory.datetime?.let { " (${dateFormat.format(Date(it))})" } ?: ""
            "- $type: ${memory.content}$time"
        }

        return buildChatPrompt(
            "I asked: \"$queryText\"\n\nHere are my stored memories:\n$memorySummary\n\nSummarize these results naturally in 2-3 sentences. List the key items."
        )
    }

    private fun fallbackQueryResponse(results: List<MemoryEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("📚 Here's what I found (${results.size} item${if (results.size != 1) "s" else ""}):\n")

        results.take(10).forEach { memory ->
            val emoji = when (memory.type) {
                MemoryType.IDEA -> "💡"
                MemoryType.TASK -> if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) "✅" else "📋"
                MemoryType.EVENT -> "📅"
            }
            val time = memory.datetime?.let { " • ${dateFormat.format(Date(it))}" } ?: ""
            sb.appendLine("$emoji ${memory.content}$time")
        }

        if (results.size > 10) {
            sb.appendLine("\n...and ${results.size - 10} more")
        }

        return sb.toString().trim()
    }

    // ─── Conversational / Freeform ──────────────────────────────────

    /**
     * Generate a freeform conversational response when the input
     * doesn't match any save/query pattern.
     */
    suspend fun generateConversationalResponse(
        context: Context,
        userInput: String,
        recentMemories: List<MemoryEntity>,
        engine: LlamaEngine
    ): String? {
        if (!engine.isReady) return null

        val contextBlock = if (recentMemories.isNotEmpty()) {
            val memSummary = recentMemories.take(5).joinToString("\n") {
                "- ${it.type.name}: ${it.content}"
            }
            "\n\nMy recent memories:\n$memSummary"
        } else ""

        val prompt = buildChatPrompt(
            "$userInput$contextBlock\n\nRespond helpfully in 1-2 sentences. If I seem to want to save something, tell me I can prefix with \"idea:\", \"task:\", or \"remind me to\"."
        )

        return engine.generateResponse(prompt)?.trim()?.takeIf { it.isNotEmpty() }
    }

    // ─── AI Safety Net / Fallback Parser ────────────────────────────

    /**
     * When RuleBasedParser yields a low confidence score,
     * use the LLM to extract intent, dates, and tags using a strict JSON schema.
     */
    suspend fun fallbackParseWithLlm(
        input: String,
        engine: LlamaEngine,
        gson: com.google.gson.Gson = com.google.gson.Gson()
    ): ParsedInput? {
        if (!engine.isReady) return null

        val systemInstruction = """
            You are a strict data extraction tool.
            Analyze the user's input and extract:
            1. 'type': One of "IDEA", "TASK", or "EVENT".
            2. 'content': The clean description.
            3. 'tags': A JSON array of string tags representing key entities/categories.
            
            Return ONLY a valid JSON object. No markdown formatting, no explanations.
            Format: {"type": "TASK", "content": "buy milk", "tags": ["shopping"]}
        """.trimIndent()

        val prompt = "<|im_start|>system\n$systemInstruction<|im_end|>\n<|im_start|>user\n$input<|im_end|>\n<|im_start|>assistant\n{"

        val rawResponse = engine.generateResponse(prompt)?.trim() ?: return null
        val jsonStr = "{$rawResponse".replace("```json", "").replace("```", "").trim()
        
        return try {
            val element = gson.fromJson(jsonStr, com.google.gson.JsonObject::class.java)
            val typeStr = element.get("type")?.asString ?: "IDEA"
            val content = element.get("content")?.asString ?: input
            
            val tags = mutableListOf<String>()
            val tagsArray = element.getAsJsonArray("tags")
            if (tagsArray != null) {
                for (tagElement in tagsArray) {
                    tags.add(tagElement.asString.lowercase())
                }
            }

            ParsedInput(
                type = MemoryType.valueOf(typeStr.uppercase(Locale.ROOT)),
                content = content,
                tags = tags,
                confidence = 0.8f // High confidence since AI successfully extracted structured data
            )
        } catch (e: Exception) {
            null
        }
    }
}
