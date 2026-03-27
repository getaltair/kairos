package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Scans active habits for lapse and relapse conditions.
 *
 * For each active habit, counts consecutive missed days backwards from yesterday.
 * - REC-1: Creates a lapse session when consecutiveMissed >= lapseThresholdDays
 * - REC-2: Enforces at most one pending session per habit
 * - REC-4: Escalates Lapse -> Relapse (one-way, never de-escalates)
 */
class DetectLapsesUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository,
    private val recoveryRepository: RecoveryRepository
) {

    /**
     * @return List of habit IDs that were newly detected as lapsed or relapsed.
     */
    suspend operator fun invoke(): Result<List<UUID>> {
        return try {
            val habitsResult = habitRepository.getActiveHabits()
            if (habitsResult is Result.Error) return Result.Error(habitsResult.message)
            val activeHabits = (habitsResult as Result.Success).value

            val affectedIds = mutableListOf<UUID>()

            for (habit in activeHabits) {
                val consecutiveMissed = countConsecutiveMissed(habit)

                // --- Relapse escalation (REC-4) ---
                // Check relapse first: if already LAPSED and missed >= relapseThreshold, escalate
                if (consecutiveMissed >= habit.relapseThresholdDays && habit.phase is HabitPhase.LAPSED) {
                    val updated = habit.copy(phase = HabitPhase.RELAPSED)
                    habitRepository.update(updated)

                    // Escalate the existing pending session type (never create a duplicate)
                    val pendingResult = recoveryRepository.getPendingForHabit(habit.id)
                    if (pendingResult is Result.Success) {
                        val pendingSessions = pendingResult.value
                        val existingSession = pendingSessions.firstOrNull()
                        if (existingSession != null && existingSession.type is RecoveryType.Lapse) {
                            val escalated = existingSession.copy(type = RecoveryType.Relapse)
                            recoveryRepository.update(escalated)
                        }
                    }

                    affectedIds.add(habit.id)
                    continue
                }

                // --- Lapse detection (REC-1) ---
                // Only trigger if not already LAPSED or RELAPSED
                if (consecutiveMissed >= habit.lapseThresholdDays &&
                    habit.phase !is HabitPhase.LAPSED &&
                    habit.phase !is HabitPhase.RELAPSED
                ) {
                    val updated = habit.copy(phase = HabitPhase.LAPSED)
                    habitRepository.update(updated)

                    // REC-2: Only create a session if none pending
                    val pendingResult = recoveryRepository.getPendingForHabit(habit.id)
                    if (pendingResult is Result.Success && pendingResult.value.isEmpty()) {
                        val session = RecoverySession(
                            habitId = habit.id,
                            type = RecoveryType.Lapse,
                            status = SessionStatus.Pending,
                            blockers = listOf(Blocker.Other)
                        )
                        recoveryRepository.insert(session)
                    }

                    affectedIds.add(habit.id)
                }
            }

            Result.Success(affectedIds)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to detect lapses: ${e.message}", cause = e)
        }
    }

    /**
     * Counts consecutive missed days backwards from yesterday for the given habit.
     * A day counts as missed when its completion type is MISSED or when no
     * completion exists at all for a day the habit was due.
     */
    private suspend fun countConsecutiveMissed(habit: Habit): Int {
        var count = 0
        var date = LocalDate.now().minusDays(1)

        while (true) {
            if (!habit.isDueToday(date)) {
                date = date.minusDays(1)
                continue
            }

            val completionResult = completionRepository.getForHabitOnDate(habit.id, date)
            if (completionResult is Result.Error) break

            val completion = (completionResult as Result.Success).value
            if (completion == null || completion.type is CompletionType.Missed) {
                count++
                date = date.minusDays(1)
            } else {
                break
            }
        }

        return count
    }
}
