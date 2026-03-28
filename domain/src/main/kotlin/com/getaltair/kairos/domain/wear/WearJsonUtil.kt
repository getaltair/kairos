package com.getaltair.kairos.domain.wear

// Shared JSON utilities for Wear Data Layer serialization.
// All hand-rolled JSON in the domain wear layer should use these functions
// to ensure consistent escaping and object splitting.

/** Escapes a string for safe embedding in a hand-rolled JSON string value. */
fun escapeJson(s: String): String = s.replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

/**
 * Splits a JSON array body (the content between the outer `[` and `]`) into
 * individual top-level JSON object strings, respecting nested braces.
 */
fun splitJsonObjects(s: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = -1
    for (i in s.indices) {
        when (s[i]) {
            '{' -> {
                if (depth == 0) start = i
                depth++
            }

            '}' -> {
                depth--
                if (depth == 0 && start != -1) {
                    result.add(s.substring(start, i + 1))
                    start = -1
                }
            }
        }
    }
    return result
}
