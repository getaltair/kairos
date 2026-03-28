package com.getaltair.kairos.domain.wear

/**
 * Lightweight active routine data transferred to the watch via the Data Layer.
 * Contains current execution state for the routine runner on the watch.
 *
 * This is a pure Kotlin data class with manual JSON serialization,
 * keeping the domain module free of Android or serialization framework dependencies.
 */
data class WearRoutineData(
    val routineId: String,
    val executionId: String,
    val name: String,
    val steps: List<String>,
    val currentStepIndex: Int,
    val status: String,
    val remainingSeconds: Int,
) {
    fun toJson(): String = buildString {
        val stepsJson = "[${steps.joinToString(",") { "\"${escapeJson(it)}\"" }}]"
        append("{")
        append("\"routineId\":\"${escapeJson(routineId)}\",")
        append("\"executionId\":\"${escapeJson(executionId)}\",")
        append("\"name\":\"${escapeJson(name)}\",")
        append("\"steps\":$stepsJson,")
        append("\"currentStepIndex\":$currentStepIndex,")
        append("\"status\":\"${escapeJson(status)}\",")
        append("\"remainingSeconds\":$remainingSeconds")
        append("}")
    }

    companion object {
        fun fromJson(json: String): WearRoutineData? {
            fun extract(key: String): String? = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

            fun extractInt(key: String): Int = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toInt() ?: 0

            val routineId = extract("routineId")
            val executionId = extract("executionId")
            if (routineId.isNullOrBlank() || executionId.isNullOrBlank()) return null

            val stepsMatch = Regex("\"steps\":\\[([^\\]]*)\\]").find(json)?.groupValues?.get(1) ?: ""
            val steps = if (stepsMatch.isBlank()) {
                emptyList()
            } else {
                // Parse quoted strings properly instead of naive split(",") which
                // breaks for step names containing commas.
                val stepList = mutableListOf<String>()
                var inQuote = false
                var current = StringBuilder()
                for (ch in stepsMatch) {
                    when {
                        ch == '"' && !inQuote -> inQuote = true

                        ch == '"' && inQuote -> {
                            stepList.add(current.toString())
                            current = StringBuilder()
                            inQuote = false
                        }

                        inQuote -> current.append(ch)
                        // Skip commas and whitespace outside quotes
                    }
                }
                stepList
            }

            return WearRoutineData(
                routineId = routineId,
                executionId = executionId,
                name = extract("name") ?: "",
                steps = steps,
                currentStepIndex = extractInt("currentStepIndex"),
                status = extract("status") ?: "",
                remainingSeconds = extractInt("remainingSeconds"),
            )
        }
    }
}
