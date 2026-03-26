package com.getaltair.kairos.domain.model

import java.util.UUID

/**
 * Aggregated completion statistics for a 7-day window.
 *
 * When [habitId] is non-null the stats describe a single habit and [totalDays]
 * reflects the number of days that habit was actually due. When [habitId] is
 * null the stats are an aggregate across all habits and [totalDays] is fixed
 * at 7 (an approximation).
 *
 * [completionRate] is derived: `(completedCount + partialCount) / totalDays`.
 */
data class WeeklyStats(
    val habitId: UUID?,
    val totalDays: Int,
    val completedCount: Int,
    val partialCount: Int,
    val skippedCount: Int,
    val missedCount: Int
) {
    init {
        require(totalDays in 1..7) { "totalDays must be in 1..7" }
        require(completedCount >= 0) { "completedCount must be non-negative" }
        require(partialCount >= 0) { "partialCount must be non-negative" }
        require(skippedCount >= 0) { "skippedCount must be non-negative" }
        require(missedCount >= 0) { "missedCount must be non-negative" }
    }

    val completionRate: Float
        get() = if (totalDays > 0) (completedCount + partialCount).toFloat() / totalDays else 0f
}
