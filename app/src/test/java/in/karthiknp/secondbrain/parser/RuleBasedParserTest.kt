package `in`.karthiknp.secondbrain.parser

import `in`.karthiknp.secondbrain.domain.model.MemoryType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the RuleBasedParser.
 *
 * These tests verify that the primary classification engine
 * correctly handles all expected input patterns.
 */
class RuleBasedParserTest {

    // ─── Task Classification ─────────────────────────────────────────

    @Test
    fun `remind me pattern should classify as TASK`() {
        val result = RuleBasedParser.parse("Remind me to study Kafka at 9pm")
        assertEquals(MemoryType.TASK, result.type)
        assertEquals("study Kafka", result.content)
        assertNotNull(result.datetime)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `todo pattern should classify as TASK`() {
        val result = RuleBasedParser.parse("todo: finish the report")
        assertEquals(MemoryType.TASK, result.type)
        assertEquals("finish the report", result.content)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `task prefix should classify as TASK`() {
        val result = RuleBasedParser.parse("task: deploy to production")
        assertEquals(MemoryType.TASK, result.type)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `need to pattern should classify as TASK`() {
        val result = RuleBasedParser.parse("need to buy groceries")
        assertEquals(MemoryType.TASK, result.type)
        assertTrue(result.confidence >= 0.9f)
    }

    // ─── Event Classification ────────────────────────────────────────

    @Test
    fun `birthday pattern should classify as EVENT`() {
        val result = RuleBasedParser.parse("Arjun birthday Jan 12")
        assertEquals(MemoryType.EVENT, result.type)
        assertTrue(result.content.contains("Arjun"))
        assertTrue(result.content.contains("birthday"))
        assertNotNull(result.datetime)
        assertTrue(result.confidence >= 0.9f)
        assertEquals("birthday", result.metadata["eventType"])
    }

    @Test
    fun `birthday of pattern should classify as EVENT`() {
        val result = RuleBasedParser.parse("Birthday of Priya on March 5")
        assertEquals(MemoryType.EVENT, result.type)
        assertNotNull(result.datetime)
        assertEquals("Priya", result.metadata["person"])
    }

    @Test
    fun `schedule pattern should classify as EVENT`() {
        val result = RuleBasedParser.parse("Schedule: team standup at 10am")
        assertEquals(MemoryType.EVENT, result.type)
        assertTrue(result.confidence >= 0.9f)
    }

    // ─── Idea Classification ─────────────────────────────────────────

    @Test
    fun `idea prefix should classify as IDEA`() {
        val result = RuleBasedParser.parse("Idea: build a scraping API")
        assertEquals(MemoryType.IDEA, result.type)
        assertEquals("build a scraping API", result.content)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `note prefix should classify as IDEA`() {
        val result = RuleBasedParser.parse("Note: Kotlin coroutines are cool")
        assertEquals(MemoryType.IDEA, result.type)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `ambiguous input should fallback to IDEA with low confidence`() {
        val result = RuleBasedParser.parse("the weather looks nice")
        assertEquals(MemoryType.IDEA, result.type)
        assertTrue(result.confidence < 0.6f)
    }

    // ─── Keyword-based Classification ────────────────────────────────

    @Test
    fun `input with task keyword should classify as TASK`() {
        val result = RuleBasedParser.parse("prepare slides for Monday")
        assertEquals(MemoryType.TASK, result.type)
        assertTrue(result.confidence >= 0.5f)
    }

    @Test
    fun `input with event keyword should classify as EVENT`() {
        val result = RuleBasedParser.parse("conference next month")
        assertEquals(MemoryType.EVENT, result.type)
    }

    // ─── Query Intent Detection (Core Fix) ───────────────────────────

    @Test
    fun `show my ideas should detect query intent`() {
        val intent = RuleBasedParser.detectQueryIntent("Show my ideas")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.IDEA, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `upcoming birthdays should detect event query`() {
        val intent = RuleBasedParser.detectQueryIntent("upcoming birthdays")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.EVENT, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `show all should detect show all intent`() {
        val intent = RuleBasedParser.detectQueryIntent("show all")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.ShowAll)
    }

    @Test
    fun `regular input should not detect query intent`() {
        val intent = RuleBasedParser.detectQueryIntent("Idea: build an API")
        assertNull(intent)
    }

    // ─── Question Mark Detection ─────────────────────────────────────

    @Test
    fun `question with question mark should detect query`() {
        val intent = RuleBasedParser.detectQueryIntent("what did I save?")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.ShowAll)
    }

    @Test
    fun `when is birthday question should detect event query`() {
        val intent = RuleBasedParser.detectQueryIntent("when is Arjun's birthday?")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.EVENT, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `do I have any tasks question should detect task query`() {
        val intent = RuleBasedParser.detectQueryIntent("do I have any tasks?")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.TASK, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `tell me my ideas should detect idea query`() {
        val intent = RuleBasedParser.detectQueryIntent("tell me my ideas")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.IDEA, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `any upcoming events should detect event query`() {
        val intent = RuleBasedParser.detectQueryIntent("any upcoming events?")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.EVENT, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `what are my saved memories should detect show all`() {
        val intent = RuleBasedParser.detectQueryIntent("what are my saved memories?")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.ShowAll)
    }

    @Test
    fun `shorthand my tasks should detect task query`() {
        val intent = RuleBasedParser.detectQueryIntent("my tasks")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.FilterByType)
        assertEquals(MemoryType.TASK, (intent as QueryIntent.FilterByType).type)
    }

    @Test
    fun `find keyword search should extract search term`() {
        val intent = RuleBasedParser.detectQueryIntent("find Kafka")
        assertNotNull(intent)
        assertTrue(intent is QueryIntent.Search)
        assertTrue((intent as QueryIntent.Search).query.contains("kafka"))
    }

    // ─── Date Parsing ────────────────────────────────────────────────

    @Test
    fun `should parse month day format`() {
        val result = RuleBasedParser.parseDate("Jan 12")
        assertNotNull(result)
    }

    @Test
    fun `should parse month day year format`() {
        val result = RuleBasedParser.parseDate("March 5, 2025")
        assertNotNull(result)
    }

    @Test
    fun `should parse time with AM PM`() {
        val result = RuleBasedParser.parseDateTime("9pm")
        assertNotNull(result)
    }

    @Test
    fun `should parse time with colon`() {
        val result = RuleBasedParser.parseDateTime("9:30 PM")
        assertNotNull(result)
    }

    // ─── Edge Cases ──────────────────────────────────────────────────

    @Test
    fun `empty input should return low confidence`() {
        val result = RuleBasedParser.parse("")
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `whitespace-only input should return low confidence`() {
        val result = RuleBasedParser.parse("   ")
        assertEquals(0f, result.confidence)
    }
}
