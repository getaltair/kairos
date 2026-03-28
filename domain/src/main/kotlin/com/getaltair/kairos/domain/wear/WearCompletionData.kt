package com.getaltair.kairos.domain.wear

/**
 * Lightweight completion data transferred between phone and watch via the Data Layer.
 * Contains only the fields needed for watch-side display.
 *
 * This is a pure Kotlin data class with manual JSON serialization,
 * keeping the domain module free of Android or serialization framework dependencies.
 */
data class WearCompletionData(
    val id: String,
    val habitId: String,
    val date: String,
    val type: String,
    val partialPercent: Int?,
) {
    fun toJson(): String = buildString {
        append("{")
        append("\"id\":\"$id\",")
        append("\"habitId\":\"$habitId\",")
        append("\"date\":\"$date\",")
        append("\"type\":\"$type\",")
        append("\"partialPercent\":${partialPercent ?: "null"}")
        append("}")
    }

    companion object {
        fun fromJson(json: String): WearCompletionData {
            fun extract(key: String): String? = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

            fun extractInt(key: String): Int? = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toInt()

            return WearCompletionData(
                id = extract("id") ?: "",
                habitId = extract("habitId") ?: "",
                date = extract("date") ?: "",
                type = extract("type") ?: "",
                partialPercent = extractInt("partialPercent"),
            )
        }

        fun listFromJson(json: String): List<WearCompletionData> {
            if (json.isBlank() || json == "[]") return emptyList()
            val trimmed = json.trim().removePrefix("[").removeSuffix("]").trim()
            if (trimmed.isEmpty()) return emptyList()
            return splitJsonObjects(trimmed).mapNotNull {
                runCatching { fromJson(it) }.getOrNull()
            }
        }

        fun listToJson(list: List<WearCompletionData>): String = "[${list.joinToString(",") { it.toJson() }}]"

        private fun splitJsonObjects(s: String): List<String> {
            val result = mutableListOf<String>()
            var depth = 0
            var start = 0
            for (i in s.indices) {
                when (s[i]) {
                    '{' -> depth++

                    '}' -> {
                        depth--
                        if (depth == 0) {
                            result.add(s.substring(start, i + 1))
                            start = i + 2
                        }
                    }
                }
            }
            return result
        }
    }
}
