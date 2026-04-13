package `in`.karthiknp.secondbrain.parser

import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.ParsedInput
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Rule-based natural language parser for classifying user input.
 *
 * This is the PRIMARY classification engine. It uses pattern matching
 * and keyword analysis to determine memory type, extract dates/times,
 * and build structured metadata.
 *
 * Design principle: fast, deterministic, and reliable.
 * Target latency: < 50ms per parse.
 */
object RuleBasedParser {

    // ─── Month Mapping ───────────────────────────────────────────────
    private val MONTHS = mapOf(
        "jan" to Calendar.JANUARY, "january" to Calendar.JANUARY,
        "feb" to Calendar.FEBRUARY, "february" to Calendar.FEBRUARY,
        "mar" to Calendar.MARCH, "march" to Calendar.MARCH,
        "apr" to Calendar.APRIL, "april" to Calendar.APRIL,
        "may" to Calendar.MAY,
        "jun" to Calendar.JUNE, "june" to Calendar.JUNE,
        "jul" to Calendar.JULY, "july" to Calendar.JULY,
        "aug" to Calendar.AUGUST, "august" to Calendar.AUGUST,
        "sep" to Calendar.SEPTEMBER, "september" to Calendar.SEPTEMBER,
        "oct" to Calendar.OCTOBER, "october" to Calendar.OCTOBER,
        "nov" to Calendar.NOVEMBER, "november" to Calendar.NOVEMBER,
        "dec" to Calendar.DECEMBER, "december" to Calendar.DECEMBER
    )

    // ─── Regex Patterns ──────────────────────────────────────────────

    // Task patterns
    private val REMIND_PATTERN = Pattern.compile(
        "^remind\\s+me\\s+to\\s+(.+?)(?:\\s+(?:at|by|on|before)\\s+(.+))?$",
        Pattern.CASE_INSENSITIVE
    )
    private val TODO_PATTERN = Pattern.compile(
        "^(?:todo|to-do|to do)[:\\s]+(.+)$",
        Pattern.CASE_INSENSITIVE
    )
    private val TASK_PREFIX_PATTERN = Pattern.compile(
        "^(?:task|do|need to|have to|must|should)[:\\s]+(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    // Event patterns
    private val BIRTHDAY_PATTERN = Pattern.compile(
        "^(.+?)\\s+birthday\\s+(.+)$",
        Pattern.CASE_INSENSITIVE
    )
    private val BIRTHDAY_OF_PATTERN = Pattern.compile(
        "^birthday\\s+(?:of\\s+)?(.+?)\\s+(?:on\\s+|is\\s+)?(.+)$",
        Pattern.CASE_INSENSITIVE
    )
    private val EVENT_ON_PATTERN = Pattern.compile(
        "^(.+?)\\s+(?:on|at)\\s+(.+)$",
        Pattern.CASE_INSENSITIVE
    )
    private val SCHEDULE_PATTERN = Pattern.compile(
        "^(?:schedule|meeting|appointment|event)[:\\s]+(.+?)(?:\\s+(?:at|on|for)\\s+(.+))?$",
        Pattern.CASE_INSENSITIVE
    )

    // Idea patterns
    private val IDEA_PREFIX_PATTERN = Pattern.compile(
        "^(?:idea|thought|concept|note)[:\\s]+(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    // Time patterns
    private val TIME_PATTERN = Pattern.compile(
        "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
        Pattern.CASE_INSENSITIVE
    )
    private val DATE_PATTERN = Pattern.compile(
        "(\\w+)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4}))?",
        Pattern.CASE_INSENSITIVE
    )
    private val RELATIVE_DATE_PATTERN = Pattern.compile(
        "(?:today|tonight|tomorrow|next\\s+\\w+)",
        Pattern.CASE_INSENSITIVE
    )

    // Task keywords (checked when no prefix-pattern matches)
    private val TASK_KEYWORDS = listOf(
        "remind", "deadline", "due", "complete", "finish",
        "submit", "call", "email", "buy", "pick up", "return",
        "pay", "book", "study", "review", "prepare", "fix",
        "clean", "wash", "cook", "send", "check"
    )

    // Event keywords
    private val EVENT_KEYWORDS = listOf(
        "birthday", "anniversary", "meeting", "appointment",
        "event", "party", "wedding", "ceremony", "conference",
        "exam", "interview", "flight", "trip", "vacation"
    )

    // ─── Main Parse Function ─────────────────────────────────────────

    /**
     * Parse raw user input into a structured [ParsedInput].
     * Returns a high-confidence result for clear patterns,
     * lower confidence when falling back to keyword matching.
     */
    fun parse(input: String): ParsedInput {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedInput(MemoryType.IDEA, "", confidence = 0f)
        }

        val tags = extractTags(trimmed)

        // 1. Try explicit prefix patterns (highest confidence)
        tryParseReminder(trimmed)?.let { return it.copy(tags = tags) }
        tryParseTodo(trimmed)?.let { return it.copy(tags = tags) }
        tryParseTaskPrefix(trimmed)?.let { return it.copy(tags = tags) }
        tryParseBirthday(trimmed)?.let { return it.copy(tags = tags) }
        tryParseSchedule(trimmed)?.let { return it.copy(tags = tags) }
        tryParseIdeaPrefix(trimmed)?.let { return it.copy(tags = tags) }

        // 2. Try keyword-based classification (medium confidence)
        tryKeywordClassify(trimmed)?.let { return it.copy(tags = tags) }

        // 3. Default to idea (lowest confidence — candidate for AI fallback)
        return ParsedInput(
            type = MemoryType.IDEA,
            content = trimmed,
            tags = tags,
            confidence = 0.4f
        )
    }

    private fun extractTags(input: String): List<String> {
        val matcher = Pattern.compile("#(\\w+)").matcher(input)
        val tags = mutableListOf<String>()
        while (matcher.find()) {
            matcher.group(1)?.let { tags.add(it.lowercase(Locale.ROOT)) }
        }
        return tags
    }

    // ─── Prefix-based Parsers ────────────────────────────────────────

    private fun tryParseReminder(input: String): ParsedInput? {
        val m = REMIND_PATTERN.matcher(input)
        if (m.matches()) {
            val task = m.group(1)?.trim() ?: return null
            val timeStr = m.group(2)?.trim()
            val datetime = timeStr?.let { parseDateTime(it) }
            return ParsedInput(
                type = MemoryType.TASK,
                content = task,
                datetime = datetime,
                metadata = buildMap {
                    if (timeStr != null) put("rawTime", timeStr)
                },
                confidence = 0.95f
            )
        }
        return null
    }

    private fun tryParseTodo(input: String): ParsedInput? {
        val m = TODO_PATTERN.matcher(input)
        if (m.matches()) {
            val task = m.group(1)?.trim() ?: return null
            return ParsedInput(
                type = MemoryType.TASK,
                content = task,
                confidence = 0.95f
            )
        }
        return null
    }

    private fun tryParseTaskPrefix(input: String): ParsedInput? {
        val m = TASK_PREFIX_PATTERN.matcher(input)
        if (m.matches()) {
            val content = m.group(1)?.trim() ?: return null
            val datetime = extractDateTime(content)
            return ParsedInput(
                type = MemoryType.TASK,
                content = content,
                datetime = datetime,
                confidence = 0.9f
            )
        }
        return null
    }

    private fun tryParseBirthday(input: String): ParsedInput? {
        // Pattern: "Arjun birthday Jan 12"
        val m1 = BIRTHDAY_PATTERN.matcher(input)
        if (m1.matches()) {
            val name = m1.group(1)?.trim() ?: return null
            val dateStr = m1.group(2)?.trim() ?: return null
            val datetime = parseDate(dateStr)
            return ParsedInput(
                type = MemoryType.EVENT,
                content = "$name's birthday",
                datetime = datetime,
                metadata = mapOf("person" to name, "eventType" to "birthday"),
                confidence = 0.95f
            )
        }

        // Pattern: "Birthday of Arjun on Jan 12"
        val m2 = BIRTHDAY_OF_PATTERN.matcher(input)
        if (m2.matches()) {
            val name = m2.group(1)?.trim() ?: return null
            val dateStr = m2.group(2)?.trim() ?: return null
            val datetime = parseDate(dateStr)
            return ParsedInput(
                type = MemoryType.EVENT,
                content = "$name's birthday",
                datetime = datetime,
                metadata = mapOf("person" to name, "eventType" to "birthday"),
                confidence = 0.95f
            )
        }

        return null
    }

    private fun tryParseSchedule(input: String): ParsedInput? {
        val m = SCHEDULE_PATTERN.matcher(input)
        if (m.matches()) {
            val event = m.group(1)?.trim() ?: return null
            val timeStr = m.group(2)?.trim()
            val datetime = timeStr?.let { parseDateTime(it) }
            return ParsedInput(
                type = MemoryType.EVENT,
                content = event,
                datetime = datetime,
                metadata = buildMap {
                    if (timeStr != null) put("rawTime", timeStr)
                    put("eventType", "scheduled")
                },
                confidence = 0.9f
            )
        }
        return null
    }

    private fun tryParseIdeaPrefix(input: String): ParsedInput? {
        val m = IDEA_PREFIX_PATTERN.matcher(input)
        if (m.matches()) {
            val content = m.group(1)?.trim() ?: return null
            return ParsedInput(
                type = MemoryType.IDEA,
                content = content,
                confidence = 0.95f
            )
        }
        return null
    }

    // ─── Keyword-based Classification ────────────────────────────────

    private fun tryKeywordClassify(input: String): ParsedInput? {
        val lower = input.lowercase(Locale.ROOT)

        // Check for birthday keyword specifically
        if (lower.contains("birthday")) {
            val datetime = extractDateTime(input)
            return ParsedInput(
                type = MemoryType.EVENT,
                content = input,
                datetime = datetime,
                metadata = mapOf("eventType" to "birthday"),
                confidence = 0.7f
            )
        }

        // Check for event keywords
        for (keyword in EVENT_KEYWORDS) {
            if (lower.contains(keyword)) {
                val datetime = extractDateTime(input)
                return ParsedInput(
                    type = MemoryType.EVENT,
                    content = input,
                    datetime = datetime,
                    confidence = 0.6f
                )
            }
        }

        // Check for task keywords
        for (keyword in TASK_KEYWORDS) {
            if (lower.contains(keyword)) {
                val datetime = extractDateTime(input)
                return ParsedInput(
                    type = MemoryType.TASK,
                    content = input,
                    datetime = datetime,
                    confidence = 0.6f
                )
            }
        }

        return null
    }

    // ─── Date/Time Extraction ────────────────────────────────────────

    /**
     * Attempt to extract a datetime from anywhere in the text.
     */
    private fun extractDateTime(text: String): Long? {
        return parseDateTime(text) ?: parseDate(text)
    }

    /**
     * Parse a datetime string that may contain both date and time components.
     * Supports:
     * - "9pm", "9:30 PM"
     * - "tomorrow 9pm", "today at 5pm"
     * - "Jan 12 at 9pm"
     */
    fun parseDateTime(text: String): Long? {
        val cal = Calendar.getInstance()

        // Try parsing relative date
        val lower = text.lowercase(Locale.ROOT)
        when {
            lower.contains("tomorrow") -> cal.add(Calendar.DAY_OF_YEAR, 1)
            lower.contains("tonight") -> { /* keep today */ }
            lower.contains("next week") -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            lower.contains("next month") -> cal.add(Calendar.MONTH, 1)
            else -> {
                // Try absolute date
                val dateResult = parseDateIntoCalendar(text, cal)
                if (!dateResult) {
                    // If no date found, check if we have at least a time
                    val timeMatcher = TIME_PATTERN.matcher(text)
                    if (!timeMatcher.find()) return null
                    // Time-only: assume today
                }
            }
        }

        // Try parsing time
        val timeMatcher = TIME_PATTERN.matcher(text)
        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = timeMatcher.group(3)?.lowercase(Locale.ROOT)

            when (ampm) {
                "pm" -> if (hour != 12) hour += 12
                "am" -> if (hour == 12) hour = 0
                else -> {
                    // No AM/PM: assume PM for hours <= 7 (heuristic for evening)
                    if (hour in 1..7) hour += 12
                }
            }

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis
    }

    /**
     * Parse a date-only string (e.g., "Jan 12", "March 5, 2025").
     * Returns epoch millis at midnight of that date.
     */
    fun parseDate(text: String): Long? {
        val cal = Calendar.getInstance()
        return if (parseDateIntoCalendar(text, cal)) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } else {
            null
        }
    }

    /**
     * Attempts to parse a date from text into the given Calendar.
     * Returns true if a date was found and applied.
     */
    private fun parseDateIntoCalendar(text: String, cal: Calendar): Boolean {
        val matcher = DATE_PATTERN.matcher(text)
        if (matcher.find()) {
            val monthStr = matcher.group(1)?.lowercase(Locale.ROOT) ?: return false
            val day = matcher.group(2)?.toIntOrNull() ?: return false
            val yearStr = matcher.group(3)

            val month = MONTHS[monthStr] ?: return false
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            if (yearStr != null) {
                val year = yearStr.toIntOrNull() ?: return false
                cal.set(Calendar.YEAR, year)
            } else {
                // If the date has already passed this year, assume next year
                val now = Calendar.getInstance()
                cal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                if (cal.before(now)) {
                    cal.add(Calendar.YEAR, 1)
                }
            }
            return true
        }
        return false
    }

    // ─── Query Intent Detection ──────────────────────────────────────

    // Words that signal a retrieval question, not a new memory
    private val QUERY_STARTERS = listOf(
        "show", "list", "get", "find", "search", "display", "fetch",
        "what", "when", "where", "which", "who", "how",
        "tell", "give", "any", "do i have", "is there", "are there",
        "did i", "have i", "can you show", "can you find",
        "recall", "remember"
    )

    /**
     * Determines if the user input is a retrieval query rather than a new memory.
     * Returns a query descriptor or null if it's a new memory capture.
     *
     * This is checked FIRST before any parsing so questions aren't accidentally saved.
     */
    fun detectQueryIntent(input: String): QueryIntent? {
        val lower = input.lowercase(Locale.ROOT).trim()
            .removeSuffix("?").removeSuffix(".").trim()

        // ─── 0. Check conversational greetings ──────────────────────
        val conversationalPhrases = listOf("hi", "hello", "hey", "testing", "test", "how are you", "what's up", "who are you")
        if (conversationalPhrases.any { lower == it || lower.startsWith("$it ") }) {
            return QueryIntent.Conversational(input)
        }

        // ─── 1. Check if the input is clearly a question ────────────
        val isQuestion = input.trimEnd().endsWith("?")
        val startsWithQueryWord = QUERY_STARTERS.any { lower.startsWith(it) }

        if (!isQuestion && !startsWithQueryWord) {
            // ─── 2. Check shorthand phrases ─────────────────────────
            return when {
                lower == "pending tasks" || lower == "my tasks" || lower == "tasks" ->
                    QueryIntent.FilterByType(MemoryType.TASK)
                lower == "my ideas" || lower == "ideas" ->
                    QueryIntent.FilterByType(MemoryType.IDEA)
                lower == "my events" || lower == "events" ->
                    QueryIntent.FilterByType(MemoryType.EVENT)
                lower.startsWith("upcoming") ->
                    QueryIntent.FilterByType(MemoryType.EVENT)
                else -> null
            }
        }

        // ─── 3. Classify what the user is asking about ──────────────
        return classifyQueryContent(lower)
    }

    /**
     * Given a normalized query string, figure out what type of data
     * the user is looking for.
     */
    private fun classifyQueryContent(lower: String): QueryIntent {
        return when {
            // Ideas
            lower.contains("idea") || lower.contains("thought") ||
            lower.contains("concept") || lower.contains("note") ->
                QueryIntent.FilterByType(MemoryType.IDEA)

            // Tasks
            lower.contains("task") || lower.contains("todo") ||
            lower.contains("to-do") || lower.contains("to do") ||
            lower.contains("pending") || lower.contains("remind") ||
            lower.contains("deadline") ->
                QueryIntent.FilterByType(MemoryType.TASK)

            // Events
            lower.contains("birthday") || lower.contains("event") ||
            lower.contains("upcoming") || lower.contains("anniversary") ||
            lower.contains("meeting") || lower.contains("appointment") ||
            lower.contains("schedule") || lower.contains("calendar") ->
                QueryIntent.FilterByType(MemoryType.EVENT)

            // Everything
            lower.contains("all") || lower.contains("everything") ||
            lower.contains("saved") || lower.contains("stored") ||
            lower.contains("memory") || lower.contains("memories") ->
                QueryIntent.ShowAll

            // Fallback: try to extract a search term
            else -> {
                val searchTerms = lower
                    .replace(Regex("^(show|list|get|find|what|search|tell|give|display|fetch|do i have|any|recall|remember|when|where|who|which|how|is|are|can you)\\s*"), "")
                    .replace(Regex("^(me|my|about|the|is|are|were|was|i|have|saved|stored)\\s*"), "")
                    .trim()
                if (searchTerms.isNotEmpty() && searchTerms.length > 1) {
                    QueryIntent.Search(searchTerms)
                } else {
                    QueryIntent.ShowAll
                }
            }
        }
    }
}

/**
 * Represents the user's retrieval intent.
 */
sealed class QueryIntent {
    data class FilterByType(val type: MemoryType) : QueryIntent()
    data class Search(val query: String) : QueryIntent()
    data object ShowAll : QueryIntent()
    data class Conversational(val text: String) : QueryIntent()
}

