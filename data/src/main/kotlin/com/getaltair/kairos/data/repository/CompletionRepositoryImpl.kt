package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.converter.CompletionTypeConverter
import com.getaltair.kairos.data.converter.SkipReasonConverter
import com.getaltair.kairos.data.dao.CompletionDao
import com.getaltair.kairos.data.mapper.CompletionEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Room-backed implementation of [com.getaltair.kairos.domain.repository.CompletionRepository].
 * Delegates persistence to [CompletionDao] and maps between entity and domain layers
 * using [CompletionEntityMapper].
 *
 * Note: [update] uses direct converters ([CompletionTypeConverter], [SkipReasonConverter])
 * rather than the mapper, because the DAO update query accepts individual field parameters.
 */
class CompletionRepositoryImpl(private val completionDao: CompletionDao) :
    com.getaltair.kairos.domain.repository.CompletionRepository {

    private val completionTypeConverter = CompletionTypeConverter()
    private val skipReasonConverter = SkipReasonConverter()

    override suspend fun getForHabitOnDate(habitId: UUID, date: LocalDate): Result<Completion?> = try {
        val entity = withContext(Dispatchers.IO) { completionDao.getForHabitOnDate(habitId, date.toString()) }
        Result.Success(entity?.let { CompletionEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get completion for habitId=%s on date=%s", habitId, date)
        Result.Error("Failed to get completion for habit on date: ${e.message}", cause = e)
    }

    override suspend fun getForDate(date: LocalDate): Result<List<Completion>> = try {
        val entities = withContext(Dispatchers.IO) { completionDao.getForDate(date.toString()) }
        Result.Success(CompletionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get completions for date=%s", date)
        Result.Error("Failed to get completions for date: ${e.message}", cause = e)
    }

    override suspend fun getForDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Completion>> = try {
        val entities =
            withContext(Dispatchers.IO) { completionDao.getForDateRange(startDate.toString(), endDate.toString()) }
        Result.Success(CompletionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get completions for date range start=%s end=%s", startDate, endDate)
        Result.Error("Failed to get completions for date range: ${e.message}", cause = e)
    }

    override suspend fun getForHabitInDateRange(
        habitId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Completion>> = try {
        val entities =
            withContext(Dispatchers.IO) {
                completionDao.getForHabitInRange(habitId, startDate.toString(), endDate.toString())
            }
        Result.Success(CompletionEntityMapper.toDomainList(entities))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get completions for habitId=%s in range start=%s end=%s", habitId, startDate, endDate)
        Result.Error("Failed to get completions for habit in date range: ${e.message}", cause = e)
    }

    override suspend fun insert(completion: Completion): Result<Completion> = try {
        val entity = CompletionEntityMapper.toEntity(completion)
        withContext(Dispatchers.IO) { completionDao.insert(entity) }
        Result.Success(completion)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to insert completion for habitId=%s", completion.habitId)
        Result.Error("Failed to insert completion: ${e.message}", cause = e)
    }

    override suspend fun update(completion: Completion): Result<Completion> = try {
        val type = completionTypeConverter.completionTypeToString(completion.type)
            ?: throw IllegalStateException("Failed to convert: completionType")
        val skipReason = completion.skipReason?.let { skipReasonConverter.skipReasonToString(it) }

        withContext(Dispatchers.IO) {
            completionDao.update(
                id = completion.id,
                completedAt = completion.completedAt.toEpochMilli(),
                type = type,
                partialPercent = completion.partialPercent,
                skipReason = skipReason,
                energyLevel = completion.energyLevel,
                note = completion.note,
                updatedAt = Instant.now().toEpochMilli()
            )
        }
        Result.Success(completion)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update completion id=%s", completion.id)
        Result.Error("Failed to update completion: ${e.message}", cause = e)
    }

    override suspend fun delete(id: UUID): Result<Unit> = try {
        withContext(Dispatchers.IO) { completionDao.delete(id) }
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to delete completion id=%s", id)
        Result.Error("Failed to delete completion: ${e.message}", cause = e)
    }

    override suspend fun getLatestForHabit(habitId: UUID): Result<Completion?> = try {
        val entities = withContext(Dispatchers.IO) { completionDao.getForHabit(habitId) }
        val latest = entities.firstOrNull()
        Result.Success(latest?.let { CompletionEntityMapper.toDomain(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get latest completion for habitId=%s", habitId)
        Result.Error("Failed to get latest completion for habit: ${e.message}", cause = e)
    }

    override suspend fun deleteForHabit(habitId: UUID): Result<Unit> = try {
        withContext(Dispatchers.IO) { completionDao.deleteForHabit(habitId) }
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to delete completions for habitId=%s", habitId)
        Result.Error("Failed to delete completions for habit: ${e.message}", cause = e)
    }
}
