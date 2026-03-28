package com.getaltair.kairos.domain.wear

/**
 * Actions the watch can send to the phone via the Data Layer message API.
 * Each subclass represents a distinct user action on the watch.
 *
 * This is a pure Kotlin sealed class with manual JSON serialization,
 * keeping the domain module free of Android or serialization framework dependencies.
 */
sealed class WearAction {

    data class CompleteHabit(val habitId: String, val type: String, val partialPercent: Int? = null) : WearAction()

    data class SkipHabit(val habitId: String, val reason: String? = null) : WearAction()

    data class StartRoutine(val routineId: String) : WearAction()

    data class AdvanceRoutineStep(val executionId: String) : WearAction()

    data class PauseRoutine(val executionId: String) : WearAction()

    data class SkipRoutineStep(val executionId: String) : WearAction() {
        override fun toJson(): String = """{"type":"routine_step_skipped","executionId":"${escapeJson(executionId)}"}"""
    }

    open fun toJson(): String = when (this) {
        is CompleteHabit -> buildString {
            append("{\"type\":\"habit_completed\",")
            append("\"habitId\":\"${escapeJson(habitId)}\",")
            append("\"completionType\":\"${escapeJson(type)}\",")
            append("\"partialPercent\":${partialPercent ?: "null"}}")
        }

        is SkipHabit -> buildString {
            append("{\"type\":\"habit_skipped\",")
            append("\"habitId\":\"${escapeJson(habitId)}\",")
            append("\"reason\":${if (reason != null) "\"${escapeJson(reason)}\"" else "null"}}")
        }

        is StartRoutine ->
            "{\"type\":\"routine_started\",\"routineId\":\"${escapeJson(routineId)}\"}"

        is AdvanceRoutineStep ->
            "{\"type\":\"routine_step_done\",\"executionId\":\"${escapeJson(executionId)}\"}"

        is PauseRoutine ->
            "{\"type\":\"routine_paused\",\"executionId\":\"${escapeJson(executionId)}\"}"

        is SkipRoutineStep ->
            """{"type":"routine_step_skipped","executionId":"${escapeJson(executionId)}"}"""
    }

    companion object {
        private fun extractField(json: String, key: String): String? =
            Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

        fun fromJson(json: String): WearAction? {
            val type = extractField(json, "type") ?: return null

            fun extract(key: String) = extractField(json, key)

            fun extractInt(key: String) = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toInt()

            return when (type) {
                "habit_completed" -> CompleteHabit(
                    habitId = extract("habitId") ?: return null,
                    type = extract("completionType") ?: "FULL",
                    partialPercent = extractInt("partialPercent"),
                )

                "habit_skipped" -> SkipHabit(
                    habitId = extract("habitId") ?: return null,
                    reason = extract("reason"),
                )

                "routine_started" -> StartRoutine(
                    routineId = extract("routineId") ?: return null,
                )

                "routine_step_done" -> AdvanceRoutineStep(
                    executionId = extract("executionId") ?: return null,
                )

                "routine_paused" -> PauseRoutine(
                    executionId = extract("executionId") ?: return null,
                )

                "routine_step_skipped" -> {
                    val executionId = extractField(json, "executionId") ?: return null
                    SkipRoutineStep(executionId)
                }

                else -> null
            }
        }
    }
}
