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
import java.time.ZoneId
import java.util.logging.Level
import java.util.logging.Logger
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

    private val logger = Logger.getLogger(DetectLapsesUseCase::class.java.name)

    /**
     * @return List of lapse detections with habit IDs and missed day counts.
     */
    suspend operator fun invoke(): Result<List<LapseDetection>> {
        return try {
            val habitsResult = habitRepository.getActiveHabits()
            if (habitsResult is Result.Error) return Result.Error(habitsResult.message)
            val activeHabits = (habitsResult as Result.Success).value

            val affectedIds = mutableListOf<LapseDetection>()

            for (habit in activeHabits) {
                val consecutiveMissed = countConsecutiveMissed(habit)

                // --- Relapse escalation (REC-4) ---
                // Check relapse first: if already LAPSED and missed >= relapseThreshold, escalate
                if (consecutiveMissed >= habit.relapseThresholdDays && habit.phase is HabitPhase.LAPSED) {
                    val updated = habit.copy(phase = HabitPhase.RELAPSED)
                    val updateResult = habitRepository.update(updated)
                    if (updateResult is Result.Error) {
                        logger.log(
                            Level.SEVERE,
                            "Failed to update habit ${habit.id} to RELAPSED: ${updateResult.message}"
                        )
                        continue
                    }

                    // Escalate the existing pending session type (never create a duplicate)
                    val pendingResult = recoveryRepository.getPendingForHabit(habit.id)
                    if (pendingResult is Result.Error) {
                        logger.log(
                            Level.SEVERE,
                            "Failed to get pending session for habit ${habit.id}: ${pendingResult.message}"
                        )
                        continue
                    }
                    val pending = (pendingResult as Result.Success).value
                    if (pending != null && pending.type is RecoveryType.Lapse) {
                        val escalated = pending.copy(type = RecoveryType.Relapse)
                        val escalateResult = recoveryRepository.update(escalated)
                        if (escalateResult is Result.Error) {
                            logger.log(
                                Level.SEVERE,
                                "Failed to escalate session ${pending.id} to Relapse: ${escalateResult.message}"
                            )
                            continue
                        }
                    }

                    affectedIds.add(LapseDetection(habit.id, consecutiveMissed))
                    continue
                }

                // --- Lapse detection (REC-1) ---
                // Only trigger if not already LAPSED or RELAPSED
                if (consecutiveMissed >= habit.lapseThresholdDays &&
                    habit.phase !is HabitPhase.LAPSED &&
                    habit.phase !is HabitPhase.RELAPSED
                ) {
                    val updated = habit.copy(phase = HabitPhase.LAPSED)
                    val updateResult = habitRepository.update(updated)
                    if (updateResult is Result.Error) {
                        logger.log(
                            Level.SEVERE,
                            "Failed to update habit ${habit.id} to LAPSED: ${updateResult.message}"
                        )
                        continue
                    }

                    // REC-2: Only create a session if none pending
                    val pendingResult = recoveryRepository.getPendingForHabit(habit.id)
                    if (pendingResult is Result.Error) {
                        logger.log(
                            Level.SEVERE,
                            "Failed to get pending session for habit ${habit.id}: ${pendingResult.message}"
                        )
                        continue
                    }
                    val pending = (pendingResult as Result.Success).value
                    if (pending == null) {
                        val session = RecoverySession(
                            habitId = habit.id,
                            type = RecoveryType.Lapse,
                            status = SessionStatus.Pending,
                            blockers = setOf(Blocker.Other)
                        )
                        val insertResult = recoveryRepository.insert(session)
                        if (insertResult is Result.Error) {
                            logger.log(
                                Level.SEVERE,
                                "Failed to insert lapse session for habit ${habit.id}: ${insertResult.message}"
                            )
                            continue
                        }
                    }

                    affectedIds.add(LapseDetection(habit.id, consecutiveMissed))
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
     * Bounded by the habit's creation date to prevent infinite lookback.
     */
    private suspend fun countConsecutiveMissed(habit: Habit): Int {
        var count = 0
        var date = LocalDate.now().minusDays(1)
        val earliestDate = habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()

        while (date >= earliestDate) {
            if (!habit.isDueToday(date)) {
                date = date.minusDays(1)
                continue
            }

            val completionResult = completionRepository.getForHabitOnDate(habit.id, date)
            if (completionResult is Result.Error) {
                logger.log(
                    Level.WARNING,
                    "Failed to check completion for habit ${habit.id} on $date: ${completionResult.message}"
                )
                break
            }

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
