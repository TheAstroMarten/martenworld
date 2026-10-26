package com.martenworld.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseParserTest {

    private fun parseAll(lines: List<String>): List<String> {
        val parser = SseParser()
        val out = mutableListOf<String>()
        lines.forEach { line -> parser.feed(line)?.let(out::add) }
        parser.finish()?.let(out::add)
        return out
    }

    @Test
    fun `emits data on blank line`() {
        val events = parseAll(listOf("data: {\"a\":1}", "", "data: [DONE]", ""))
        assertEquals(listOf("{\"a\":1}", "[DONE]"), events)
    }

    @Test
    fun `ignores comments and unknown fields`() {
        val events = parseAll(listOf(": keep-alive", "event: message", "id: 7", "data: x", ""))
        assertEquals(listOf("x"), events)
    }

    @Test
    fun `joins multi-line data with newline`() {
        val events = parseAll(listOf("data: line one", "data: line two", ""))
        assertEquals(listOf("line one\nline two"), events)
    }

    @Test
    fun `strips only a single leading space`() {
        val events = parseAll(listOf("data:  two spaces", "", "data:nospace", ""))
        assertEquals(listOf(" two spaces", "nospace"), events)
    }

    @Test
    fun `blank lines without data emit nothing`() {
        val parser = SseParser()
        assertNull(parser.feed(""))
        assertNull(parser.feed(""))
        assertNull(parser.finish())
    }

    @Test
    fun `finish flushes an unterminated event`() {
        val events = parseAll(listOf("data: tail"))
        assertEquals(listOf("tail"), events)
    }
}
