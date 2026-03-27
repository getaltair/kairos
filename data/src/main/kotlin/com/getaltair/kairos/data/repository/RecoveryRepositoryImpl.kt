package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.converter.RecoveryActionConverter
import com.getaltair.kairos.data.converter.RecoveryTypeConverter
import com.getaltair.kairos.data.dao.RecoverySessionDao
import com.getaltair.kairos.data.mapper.RecoverySessionEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.repository.AuthRepository
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
 * Room-backed implementation of [com.getaltair.kairos.domain.repository.RecoveryRepository].
 * Delegates persistence to [RecoverySessionDao] and maps between entity and domain layers
 * using [RecoverySessionEntityMapper].
 *
 * Note: [update] uses a direct converter ([RecoveryActionConverter]) rather than the mapper,
 * because the DAO update query accepts individual field parameters.
 */
class RecoveryRepositoryImpl(
    private val dao: RecoverySessionDao,
    private val syncTrigger: SyncTrigger,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
) : com.getaltair.kairos.domain.repository.RecoveryRepository {

    private val recoveryActionConverter = RecoveryActionConverter()
    private val recoveryTypeConverter = RecoveryTypeConverter()

    override suspend fun getPendingForHabit(habitId: UUID): Result<List<RecoverySession>> = try {
        val entities = withContext(Dispatchers.IO) { dao.getPendingForHabit(habitId) }
        Result.Success(RecoverySessionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get pending recovery sessions for habitId=%s", habitId)
        Result.Error("Failed to get pending recovery sessions: ${e.message}", cause = e)
    }

    override suspend fun getAllPending(): Result<List<RecoverySession>> = try {
        val entities = withContext(Dispatchers.IO) { dao.getPendingSessions() }
        Result.Success(RecoverySessionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get all pending recovery sessions")
        Result.Error("Failed to get all pending recovery sessions: ${e.message}", cause = e)
    }

    override suspend fun getAllForHabit(habitId: UUID): Result<List<RecoverySession>> = try {
        val entities = withContext(Dispatchers.IO) { dao.getByHabit(habitId) }
        Result.Success(RecoverySessionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get recovery sessions for habitId=%s", habitId)
        Result.Error("Failed to get recovery sessions for habit: ${e.message}", cause = e)
    }

    override suspend fun getById(id: UUID): Result<RecoverySession?> = try {
        val entity = withContext(Dispatchers.IO) { dao.getById(id) }
        Result.Success(entity?.let { RecoverySessionEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get recovery session id=%s", id)
        Result.Error("Failed to get recovery session: ${e.message}", cause = e)
    }

    override suspend fun insert(session: RecoverySession): Result<RecoverySession> = try {
        val entity = RecoverySessionEntityMapper.toEntity(session)
        withContext(Dispatchers.IO) { dao.insert(entity) }
        triggerSync(SyncEntityTypes.RECOVERY_SESSION, session.id.toString(), session)
        Result.Success(session)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to insert recovery session for habitId=%s", session.habitId)
        Result.Error("Failed to insert recovery session: ${e.message}", cause = e)
    }

    override suspend fun update(session: RecoverySession): Result<RecoverySession> = try {
        val action = session.action?.let { recoveryActionConverter.recoveryActionToString(it) }
        val type = recoveryTypeConverter.recoveryTypeToString(session.type) ?: "Lapse"

        withContext(Dispatchers.IO) {
            dao.update(
                id = session.id,
                type = type,
                status = session.status,
                completedAt = session.completedAt?.toEpochMilli(),
                action = action,
                notes = session.notes,
                updatedAt = Instant.now().toEpochMilli()
            )
        }
        triggerSync(SyncEntityTypes.RECOVERY_SESSION, session.id.toString(), session)
        Result.Success(session)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update recovery session id=%s", session.id)
        Result.Error("Failed to update recovery session: ${e.message}", cause = e)
    }

    override suspend fun delete(id: UUID): Result<Unit> = try {
        withContext(Dispatchers.IO) { dao.delete(id) }
        triggerDeletion(SyncEntityTypes.RECOVERY_SESSION, id.toString())
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to delete recovery session id=%s", id)
        Result.Error("Failed to delete recovery session: ${e.message}", cause = e)
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
                Timber.e(e, "Failed to push recovery session sync change id=%s", id)
            }
        }
    }

    /**
     * Fire-and-forget sync deletion. Same non-blocking semantics as [triggerSync].
     */
    private fun triggerDeletion(entityType: String, id: String) {
        val userId = authRepository.getCurrentUserId() ?: run {
            Timber.d("Skipping sync push: user not signed in")
            return
        }
        syncScope.launch(Dispatchers.IO) {
            try {
                syncTrigger.triggerDeletion(userId, entityType, id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to push recovery session deletion sync id=%s", id)
            }
        }
    }
}
