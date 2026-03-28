package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.RoutineExecutionDao
import com.getaltair.kairos.data.mapper.RoutineExecutionEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import com.getaltair.kairos.domain.sync.SyncEntityTypes
import com.getaltair.kairos.domain.sync.SyncTrigger
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Room-backed implementation of [RoutineExecutionRepository].
 * Delegates persistence to [RoutineExecutionDao] and maps between entity and domain layers
 * using [RoutineExecutionEntityMapper].
 */
class RoutineExecutionRepositoryImpl(
    private val routineExecutionDao: RoutineExecutionDao,
    private val syncTrigger: SyncTrigger,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
) : RoutineExecutionRepository {

    override suspend fun getById(id: UUID): Result<RoutineExecution?> = try {
        val entity = withContext(Dispatchers.IO) { routineExecutionDao.getById(id) }
        Result.Success(entity?.let { RoutineExecutionEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get routine execution id=%s", id)
        Result.Error("Failed to get routine execution: ${e.message}", cause = e)
    }

    override suspend fun getActiveForRoutine(routineId: UUID): Result<RoutineExecution?> = try {
        val entity = withContext(Dispatchers.IO) {
            routineExecutionDao.getActiveForRoutine(routineId)
        }
        Result.Success(entity?.let { RoutineExecutionEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get active execution for routineId=%s", routineId)
        Result.Error("Failed to get active execution: ${e.message}", cause = e)
    }

    override suspend fun insert(execution: RoutineExecution): Result<RoutineExecution> = try {
        val entity = RoutineExecutionEntityMapper.toEntity(execution)
        withContext(Dispatchers.IO) { routineExecutionDao.insert(entity) }
        triggerSync(SyncEntityTypes.ROUTINE_EXECUTION, execution.id.toString(), execution)
        Result.Success(execution)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to insert routine execution for routineId=%s", execution.routineId)
        Result.Error("Failed to insert routine execution: ${e.message}", cause = e)
    }

    override suspend fun update(execution: RoutineExecution): Result<RoutineExecution> = try {
        withContext(Dispatchers.IO) {
            routineExecutionDao.update(
                id = execution.id,
                completedAt = execution.completedAt?.toEpochMilli(),
                status = execution.status,
                currentStepIndex = execution.currentStepIndex,
                currentStepRemainingSeconds = execution.currentStepRemainingSeconds,
                totalPausedSeconds = execution.totalPausedSeconds,
                updatedAt = Instant.now().toEpochMilli(),
            )
        }
        triggerSync(SyncEntityTypes.ROUTINE_EXECUTION, execution.id.toString(), execution)
        Result.Success(execution)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update routine execution id=%s", execution.id)
        Result.Error("Failed to update routine execution: ${e.message}", cause = e)
    }

    /**
     * Fire-and-forget sync push. Runs in a non-blocking scope so that the
     * local Room operation is never delayed by Firestore.
     */
    private fun triggerSync(entityType: String, id: String, entity: Any) {
        val userId = authRepository.getCurrentUserId() ?: run {
            Timber.d("Skipping sync push: user not signed in")
            return
        }
        syncScope.launch(Dispatchers.IO) {
            try {
                syncTrigger.triggerPush(userId, entityType, id, entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to push routine execution sync change id=%s", id)
            }
        }
    }
}
