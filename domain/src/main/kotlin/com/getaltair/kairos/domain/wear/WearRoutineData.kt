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
        val stepsJson = "[${steps.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"
        append("{")
        append("\"routineId\":\"$routineId\",")
        append("\"executionId\":\"$executionId\",")
        append("\"name\":\"${name.replace("\"", "\\\"")}\",")
        append("\"steps\":$stepsJson,")
        append("\"currentStepIndex\":$currentStepIndex,")
        append("\"status\":\"$status\",")
        append("\"remainingSeconds\":$remainingSeconds")
        append("}")
    }

    companion object {
        fun fromJson(json: String): WearRoutineData {
            fun extract(key: String): String? = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

            fun extractInt(key: String): Int = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toInt() ?: 0

            val stepsMatch = Regex("\"steps\":\\[([^\\]]*)\\]").find(json)?.groupValues?.get(1) ?: ""
            val steps = if (stepsMatch.isBlank()) {
                emptyList()
            } else {
                stepsMatch.split(",").map { it.trim().removePrefix("\"").removeSuffix("\"") }
            }

            return WearRoutineData(
                routineId = extract("routineId") ?: "",
                executionId = extract("executionId") ?: "",
                name = extract("name") ?: "",
                steps = steps,
                currentStepIndex = extractInt("currentStepIndex"),
                status = extract("status") ?: "",
                remainingSeconds = extractInt("remainingSeconds"),
            )
        }
    }
}
