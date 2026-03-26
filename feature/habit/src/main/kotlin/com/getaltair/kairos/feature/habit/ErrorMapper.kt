package com.getaltair.kairos.feature.habit

import timber.log.Timber

/**
 * Translates internal validator and repository error messages into
 * user-friendly strings suitable for display in the UI.
 *
 * Matching strategy:
 * - Domain validation errors are matched using substring [contains] checks on the technical message.
 * - Repository-layer errors (e.g. insert/update/delete failures) are matched using
 *   prefix [startsWith] checks on the technical message.
 */
object ErrorMapper {

    fun toUserMessage(technicalMessage: String): String = when {
        technicalMessage.contains("anchorBehavior must not be blank") ->
            "Please describe when you'll do this habit."

        technicalMessage.contains("allowPartialCompletion must be true") ->
            "Something went wrong. Please try again."

        technicalMessage.contains("relapseThresholdDays") ||
            technicalMessage.contains("lapseThresholdDays") ->
            "Something went wrong with the habit settings. Please try again."

        technicalMessage.contains("createdAt") ||
            technicalMessage.contains("pausedAt") ||
            technicalMessage.contains("archivedAt") ->
            "Something went wrong. Please try again."

        technicalMessage.startsWith("Failed to insert habit") ||
            technicalMessage.startsWith("Failed to update habit") ||
            technicalMessage.startsWith("Failed to delete habit") ->
            "Could not save your habit. Please try again."

        else -> {
            Timber.w("ErrorMapper: unmapped error: %s", technicalMessage)
            "Something went wrong. Please try again."
        }
    }
}
