package `in`.karthiknp.secondbrain.ui.chat

import `in`.karthiknp.secondbrain.domain.model.MemoryType

/**
 * Represents a single message in the chat interface.
 */
data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val memoryType: MemoryType? = null
)
