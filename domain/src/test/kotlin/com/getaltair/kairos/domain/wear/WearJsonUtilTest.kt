package com.getaltair.kairos.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearJsonUtilTest {

    // -- escapeJson --

    @Test
    fun `escapeJson leaves plain text unchanged`() {
        assertEquals("hello world", escapeJson("hello world"))
    }

    @Test
    fun `escapeJson escapes double quotes`() {
        assertEquals("""He said \"hi\"""", escapeJson("""He said "hi""""))
    }

    @Test
    fun `escapeJson escapes backslashes`() {
        assertEquals("""C:\\Users\\me""", escapeJson("""C:\Users\me"""))
    }

    @Test
    fun `escapeJson escapes newlines and tabs`() {
        assertEquals("line1\\nline2\\ttab", escapeJson("line1\nline2\ttab"))
    }

    @Test
    fun `escapeJson escapes carriage return`() {
        assertEquals("a\\rb", escapeJson("a\rb"))
    }

    @Test
    fun `escapeJson handles combined special characters`() {
        val input = "quote:\" backslash:\\ newline:\n tab:\t"
        val escaped = escapeJson(input)
        assertEquals("quote:\\\" backslash:\\\\ newline:\\n tab:\\t", escaped)
    }

    @Test
    fun `escapeJson handles empty string`() {
        assertEquals("", escapeJson(""))
    }

    // -- splitJsonObjects --

    @Test
    fun `splitJsonObjects splits two objects`() {
        val input = """{"a":1},{"b":2}"""
        val result = splitJsonObjects(input)
        assertEquals(2, result.size)
        assertEquals("""{"a":1}""", result[0])
        assertEquals("""{"b":2}""", result[1])
    }

    @Test
    fun `splitJsonObjects handles nested braces`() {
        val input = """{"a":{"x":1}},{"b":2}"""
        val result = splitJsonObjects(input)
        assertEquals(2, result.size)
        assertEquals("""{"a":{"x":1}}""", result[0])
        assertEquals("""{"b":2}""", result[1])
    }

    @Test
    fun `splitJsonObjects returns empty list for empty string`() {
        assertTrue(splitJsonObjects("").isEmpty())
    }

    @Test
    fun `splitJsonObjects returns empty list for whitespace`() {
        assertTrue(splitJsonObjects("   ").isEmpty())
    }

    @Test
    fun `splitJsonObjects handles single object`() {
        val input = """{"key":"value"}"""
        val result = splitJsonObjects(input)
        assertEquals(1, result.size)
        assertEquals(input, result[0])
    }

    @Test
    fun `splitJsonObjects ignores leading and trailing whitespace around objects`() {
        val input = """  {"a":1} , {"b":2}  """
        val result = splitJsonObjects(input)
        assertEquals(2, result.size)
        assertEquals("""{"a":1}""", result[0])
        assertEquals("""{"b":2}""", result[1])
    }

    @Test
    fun `splitJsonObjects handles deeply nested braces`() {
        val input = """{"a":{"b":{"c":1}}}"""
        val result = splitJsonObjects(input)
        assertEquals(1, result.size)
        assertEquals(input, result[0])
    }
}
