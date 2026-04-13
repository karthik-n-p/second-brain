package `in`.karthiknp.secondbrain.domain.model

/**
 * Represents the result of parsing user input.
 * This is a domain model, independent of the database entity.
 */
data class ParsedInput(
    val type: MemoryType,
    val content: String,
    val datetime: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val confidence: Float = 1.0f
)
