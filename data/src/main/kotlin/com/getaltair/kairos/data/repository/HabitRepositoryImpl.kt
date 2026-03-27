package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.converter.AnchorTypeConverter
import com.getaltair.kairos.data.converter.DayOfWeekListConverter
import com.getaltair.kairos.data.converter.HabitFrequencyConverter
import com.getaltair.kairos.data.converter.HabitPhaseConverter
import com.getaltair.kairos.data.converter.HabitStatusConverter
import com.getaltair.kairos.data.converter.JsonListConverter
import com.getaltair.kairos.data.dao.HabitDao
import com.getaltair.kairos.data.mapper.HabitEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncEntityTypes
import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.domain.util.HabitScheduleUtil
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Room-backed implementation of [com.getaltair.kairos.domain.repository.HabitRepository].
 * Delegates persistence to [HabitDao] and maps between entity and domain layers
 * using [HabitEntityMapper].
 *
 * Note: [update] uses direct converters ([AnchorTypeConverter], [HabitFrequencyConverter],
 * [HabitPhaseConverter], [DayOfWeekListConverter], [JsonListConverter]) rather than the mapper,
 * because the DAO update query accepts individual field parameters.
 */
class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val syncTrigger: SyncTrigger,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
) : com.getaltair.kairos.domain.repository.HabitRepository {

    private val anchorTypeConverter = AnchorTypeConverter()
    private val frequencyConverter = HabitFrequencyConverter()
    private val phaseConverter = HabitPhaseConverter()
    private val statusConverter = HabitStatusConverter()
    private val dayOfWeekListConverter = DayOfWeekListConverter()
    private val jsonListConverter = JsonListConverter()

    override suspend fun getById(id: UUID): Result<Habit> = try {
        val entity = withContext(Dispatchers.IO) { habitDao.getById(id) }
        if (entity != null) {
            Result.Success(HabitEntityMapper.toDomain(entity))
        } else {
            Result.Error("Habit not found with id: $id")
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get habit id=%s", id)
        Result.Error("Failed to get habit: ${e.message}", cause = e)
    }

    override suspend fun getActiveHabits(): Result<List<Habit>> = try {
        val entities = withContext(Dispatchers.IO) { habitDao.getActiveHabits() }
        Result.Success(HabitEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get active habits")
        Result.Error("Failed to get active habits: ${e.message}", cause = e)
    }

    override suspend fun getHabitsForDate(date: LocalDate): Result<List<Habit>> = try {
        val entities = withContext(Dispatchers.IO) { habitDao.getActiveHabits() }
        val habits = HabitEntityMapper.toDomainList(entities)
        val dayOfWeek = date.dayOfWeek
        val filtered = habits.filter { habit -> HabitScheduleUtil.isDueOnDate(habit, dayOfWeek) }
        Result.Success(filtered)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get habits for date=%s", date)
        Result.Error("Failed to get habits for date: ${e.message}", cause = e)
    }

    override suspend fun getByStatus(status: HabitStatus): Result<List<Habit>> = try {
        val statusString = statusConverter.habitStatusToString(status)
            ?: throw IllegalStateException("Failed to convert: status")
        val entities = withContext(Dispatchers.IO) { habitDao.getByStatus(statusString) }
        Result.Success(HabitEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get habits by status=%s", status)
        Result.Error("Failed to get habits by status: ${e.message}", cause = e)
    }

    override suspend fun getByCategory(category: HabitCategory): Result<List<Habit>> = try {
        val entities = withContext(Dispatchers.IO) { habitDao.getByCategory(category) }
        Result.Success(HabitEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get habits by category=%s", category)
        Result.Error("Failed to get habits by category: ${e.message}", cause = e)
    }

    // TODO: Use per-habit lapse_threshold_days instead of hardcoded default
    override suspend fun getLapsedHabits(): Result<List<Habit>> = try {
        val entities = withContext(Dispatchers.IO) { habitDao.getLapsedHabits(DEFAULT_LAPSE_THRESHOLD_DAYS) }
        Result.Success(HabitEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get lapsed habits")
        Result.Error("Failed to get lapsed habits: ${e.message}", cause = e)
    }

    override suspend fun insert(habit: Habit): Result<Habit> = try {
        val entity = HabitEntityMapper.toEntity(habit)
        withContext(Dispatchers.IO) { habitDao.insert(entity) }
        triggerSync(SyncEntityTypes.HABIT, habit.id.toString(), habit)
        Result.Success(habit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to insert habit name=%s", habit.name)
        Result.Error("Failed to insert habit: ${e.message}", cause = e)
    }

    override suspend fun update(habit: Habit): Result<Habit> = try {
        val anchorType = anchorTypeConverter.anchorTypeToString(habit.anchorType)
            ?: throw IllegalStateException("Failed to convert: anchorType")
        val frequency = frequencyConverter.habitFrequencyToString(habit.frequency)
            ?: throw IllegalStateException("Failed to convert: frequency")
        val phase = phaseConverter.habitPhaseToString(habit.phase)
            ?: throw IllegalStateException("Failed to convert: phase")
        val activeDays = dayOfWeekListConverter.dayOfWeekSetToString(habit.activeDays)
        val subtasks = jsonListConverter.stringListToString(habit.subtasks)

        withContext(Dispatchers.IO) {
            habitDao.update(
                id = habit.id,
                name = habit.name,
                description = habit.description,
                icon = habit.icon,
                color = habit.color,
                anchorBehavior = habit.anchorBehavior,
                anchorType = anchorType,
                timeWindowStart = habit.timeWindowStart,
                timeWindowEnd = habit.timeWindowEnd,
                category = habit.category,
                frequency = frequency,
                activeDays = activeDays,
                estimatedSeconds = habit.estimatedSeconds,
                microVersion = habit.microVersion,
                allowPartialCompletion = habit.allowPartialCompletion,
                subtasks = subtasks,
                phase = phase,
                status = habit.status,
                pausedAt = habit.pausedAt?.toEpochMilli(),
                archivedAt = habit.archivedAt?.toEpochMilli(),
                lapseThresholdDays = habit.lapseThresholdDays,
                relapseThresholdDays = habit.relapseThresholdDays,
                updatedAt = Instant.now().toEpochMilli()
            )
        }
        triggerSync(SyncEntityTypes.HABIT, habit.id.toString(), habit)
        Result.Success(habit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update habit id=%s", habit.id)
        Result.Error("Failed to update habit: ${e.message}", cause = e)
    }

    override suspend fun delete(id: UUID): Result<Unit> = try {
        withContext(Dispatchers.IO) { habitDao.delete(id) }
        triggerDeletion(SyncEntityTypes.HABIT, id.toString())
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to delete habit id=%s", id)
        Result.Error("Failed to delete habit: ${e.message}", cause = e)
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
                Timber.e(e, "Failed to push habit sync change id=%s", id)
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
                Timber.e(e, "Failed to push habit deletion sync id=%s", id)
            }
        }
    }

    companion object {
        private const val DEFAULT_LAPSE_THRESHOLD_DAYS = 3
    }
}
