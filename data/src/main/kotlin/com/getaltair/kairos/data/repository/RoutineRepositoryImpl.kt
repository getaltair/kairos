package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.converter.HabitCategoryConverter
import com.getaltair.kairos.data.dao.RoutineDao
import com.getaltair.kairos.data.dao.RoutineHabitDao
import com.getaltair.kairos.data.dao.RoutineVariantDao
import com.getaltair.kairos.data.mapper.RoutineEntityMapper
import com.getaltair.kairos.data.mapper.RoutineHabitEntityMapper
import com.getaltair.kairos.data.mapper.RoutineVariantEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.entity.RoutineVariant
import com.getaltair.kairos.domain.model.RoutineWithHabits
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.RoutineRepository
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
 * Room-backed implementation of [RoutineRepository].
 * Delegates persistence to [RoutineDao], [RoutineHabitDao],
 * and [RoutineVariantDao]. Maps between entity and domain layers using entity mappers.
 */
class RoutineRepositoryImpl(
    private val routineDao: RoutineDao,
    private val routineHabitDao: RoutineHabitDao,
    private val routineVariantDao: RoutineVariantDao,
    private val syncTrigger: SyncTrigger,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
) : RoutineRepository {

    private val habitCategoryConverter = HabitCategoryConverter()

    override suspend fun getById(id: UUID): Result<Routine?> = try {
        val entity = withContext(Dispatchers.IO) { routineDao.getById(id) }
        Result.Success(entity?.let { RoutineEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get routine id=%s", id)
        Result.Error("Failed to get routine: ${e.message}", cause = e)
    }

    override suspend fun getRoutineWithHabits(id: UUID): Result<RoutineWithHabits?> = try {
        val routineEntity = withContext(Dispatchers.IO) { routineDao.getById(id) }
        if (routineEntity == null) {
            Result.Success(null)
        } else {
            val habitEntities = withContext(Dispatchers.IO) {
                routineHabitDao.getByRoutineId(id)
            }
            val routine = RoutineEntityMapper.toDomain(routineEntity)
            val habits = RoutineHabitEntityMapper.toDomainList(habitEntities)
            Result.Success(RoutineWithHabits(routine, habits))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get routine with habits id=%s", id)
        Result.Error("Failed to get routine with habits: ${e.message}", cause = e)
    }

    override suspend fun getActiveRoutines(): Result<List<Routine>> = try {
        val entities = withContext(Dispatchers.IO) { routineDao.getActiveRoutines() }
        Result.Success(RoutineEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get active routines")
        Result.Error("Failed to get active routines: ${e.message}", cause = e)
    }

    override suspend fun getVariantsForRoutine(routineId: UUID): Result<List<RoutineVariant>> = try {
        val entities = withContext(Dispatchers.IO) {
            routineVariantDao.getByRoutineId(routineId)
        }
        Result.Success(RoutineVariantEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get variants for routineId=%s", routineId)
        Result.Error("Failed to get variants for routine: ${e.message}", cause = e)
    }

    override suspend fun insert(routine: Routine, habitIds: List<UUID>): Result<Routine> = try {
        val routineEntity = RoutineEntityMapper.toEntity(routine)
        val routineHabits = habitIds.mapIndexed { index, habitId ->
            RoutineHabit(
                routineId = routine.id,
                habitId = habitId,
                orderIndex = index,
            )
        }
        val routineHabitEntities = RoutineHabitEntityMapper.toEntityList(routineHabits)

        withContext(Dispatchers.IO) {
            routineDao.insertWithHabits(routineEntity, routineHabitEntities)
        }

        triggerSync(SyncEntityTypes.ROUTINE, routine.id.toString(), routine)
        routineHabits.forEach { rh ->
            triggerSync(SyncEntityTypes.ROUTINE_HABIT, rh.id.toString(), rh)
        }
        Result.Success(routine)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to insert routine name=%s", routine.name)
        Result.Error("Failed to insert routine: ${e.message}", cause = e)
    }

    override suspend fun update(routine: Routine): Result<Routine> = try {
        val category = habitCategoryConverter.habitCategoryToString(routine.category)
            ?: throw IllegalStateException("Unknown HabitCategory: ${routine.category}")

        withContext(Dispatchers.IO) {
            routineDao.update(
                id = routine.id,
                name = routine.name,
                description = routine.description,
                icon = routine.icon,
                color = routine.color,
                category = category,
                status = routine.status,
                userId = null,
                updatedAt = Instant.now().toEpochMilli(),
            )
        }
        triggerSync(SyncEntityTypes.ROUTINE, routine.id.toString(), routine)
        Result.Success(routine)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update routine id=%s", routine.id)
        Result.Error("Failed to update routine: ${e.message}", cause = e)
    }

    override suspend fun delete(id: UUID): Result<Unit> = try {
        withContext(Dispatchers.IO) { routineDao.delete(id) }
        triggerDeletion(SyncEntityTypes.ROUTINE, id.toString())
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to delete routine id=%s", id)
        Result.Error("Failed to delete routine: ${e.message}", cause = e)
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
                Timber.e(e, "Failed to push routine sync change id=%s", id)
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
                Timber.e(e, "Failed to push routine deletion sync id=%s", id)
            }
        }
    }
}
