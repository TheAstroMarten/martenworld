package com.martenworld.app.llm

/**
 * Minimal Server-Sent Events line parser. Feed it one line at a time; it returns a
 * complete event's data payload when the blank separator line arrives.
 *
 * Only `data:` fields matter for OpenAI-compatible streams. Comments (`:`) and other
 * fields are ignored. Multi-line data is joined with newlines per the SSE spec.
 */
class SseParser {
    private val buffer = StringBuilder()
    private var hasData = false

    /** Returns the data of a completed event, or null if the event is not finished yet. */
    fun feed(line: String): String? {
        if (line.isEmpty()) {
            if (!hasData) return null
            val data = buffer.toString()
            buffer.setLength(0)
            hasData = false
            return data
        }
        if (line.startsWith(":")) return null
        if (line.startsWith("data:")) {
            var value = line.substring(5)
            if (value.startsWith(" ")) value = value.substring(1)
            if (hasData) buffer.append('\n')
            buffer.append(value)
            hasData = true
        }
        return null
    }

    /** Flush a trailing event that was not terminated by a blank line. */
    fun finish(): String? = feed("")

    companion object {
        const val DONE = "[DONE]"
    }
}
