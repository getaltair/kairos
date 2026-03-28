package com.getaltair.kairos.domain.wear

/**
 * Actions the watch can send to the phone via the Data Layer message API.
 * Each subclass represents a distinct user action on the watch.
 *
 * This is a pure Kotlin sealed class with manual JSON serialization,
 * keeping the domain module free of Android or serialization framework dependencies.
 */
sealed class WearAction {

    data class CompleteHabit(val habitId: String, val type: String, val partialPercent: Int? = null,) : WearAction()

    data class SkipHabit(val habitId: String, val reason: String? = null,) : WearAction()

    data class StartRoutine(val routineId: String) : WearAction()

    data class AdvanceRoutineStep(val executionId: String) : WearAction()

    data class PauseRoutine(val executionId: String) : WearAction()

    fun toJson(): String = when (this) {
        is CompleteHabit -> buildString {
            append("{\"type\":\"habit_completed\",")
            append("\"habitId\":\"$habitId\",")
            append("\"completionType\":\"$type\",")
            append("\"partialPercent\":${partialPercent ?: "null"}}")
        }

        is SkipHabit -> buildString {
            append("{\"type\":\"habit_skipped\",")
            append("\"habitId\":\"$habitId\",")
            append("\"reason\":${if (reason != null) "\"$reason\"" else "null"}}")
        }

        is StartRoutine ->
            "{\"type\":\"routine_started\",\"routineId\":\"$routineId\"}"

        is AdvanceRoutineStep ->
            "{\"type\":\"routine_step_done\",\"executionId\":\"$executionId\"}"

        is PauseRoutine ->
            "{\"type\":\"routine_paused\",\"executionId\":\"$executionId\"}"
    }

    companion object {
        fun fromJson(json: String): WearAction? {
            val type = Regex("\"type\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)
                ?: return null

            fun extract(key: String) = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

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

                else -> null
            }
        }
    }
}
