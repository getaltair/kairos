package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.entity.RoutineVariant
import java.util.UUID

/**
 * Repository interface for Routine entity operations.
 * Implemented in data layer with Room database.
 */
interface RoutineRepository {
    /**
     * Gets a routine by its ID with associated habits.
     */
    suspend fun getById(id: UUID): Result<Routine?>

    /**
     * Gets a routine with its associated habits.
     * Returns routine and list of RoutineHabit associations.
     */
    suspend fun getRoutineWithHabits(id: UUID): Result<Pair<Routine, List<RoutineHabit>>?>

    /**
     * Gets all active routines.
     * Active routines are those with status = Active.
     */
    suspend fun getActiveRoutines(): Result<List<Routine>>

    /**
     * Gets all variants for a routine.
     */
    suspend fun getVariantsForRoutine(routineId: UUID): Result<List<RoutineVariant>>

    /**
     * Inserts a new routine.
     * Also inserts associated RoutineHabit entities.
     */
    suspend fun insert(routine: Routine, habitIds: List<UUID>): Result<Routine>

    /**
     * Updates an existing routine.
     * Use copy() to create updated instance.
     */
    suspend fun update(routine: Routine): Result<Routine>

    /**
     * Deletes a routine.
     * Cascade delete is handled in data layer.
     */
    suspend fun delete(id: UUID): Result<Unit>
}
