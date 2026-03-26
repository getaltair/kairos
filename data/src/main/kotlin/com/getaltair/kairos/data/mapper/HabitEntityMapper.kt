package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.converter.AnchorTypeConverter
import com.getaltair.kairos.data.converter.DayOfWeekListConverter
import com.getaltair.kairos.data.converter.HabitCategoryConverter
import com.getaltair.kairos.data.converter.HabitFrequencyConverter
import com.getaltair.kairos.data.converter.HabitPhaseConverter
import com.getaltair.kairos.data.converter.HabitStatusConverter
import com.getaltair.kairos.data.converter.JsonListConverter
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.entity.Habit
import java.time.Instant

/**
 * Bidirectional mapper between [HabitEntity] and [Habit].
 */
object HabitEntityMapper {

    private val anchorTypeConverter = AnchorTypeConverter()
    private val habitCategoryConverter = HabitCategoryConverter()
    private val habitFrequencyConverter = HabitFrequencyConverter()
    private val habitPhaseConverter = HabitPhaseConverter()
    private val habitStatusConverter = HabitStatusConverter()
    private val dayOfWeekListConverter = DayOfWeekListConverter()
    private val jsonListConverter = JsonListConverter()

    /**
     * Converts [HabitEntity] to domain [Habit].
     */
    fun toDomain(entity: HabitEntity): Habit = Habit(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        icon = entity.icon,
        color = entity.color,
        anchorBehavior = entity.anchorBehavior,
        anchorType = anchorTypeConverter.stringToAnchorType(entity.anchorType)
            ?: throw IllegalArgumentException("Invalid anchor type: ${entity.anchorType}"),
        timeWindowStart = entity.timeWindowStart,
        timeWindowEnd = entity.timeWindowEnd,
        category = habitCategoryConverter.stringToHabitCategory(entity.category)
            ?: throw IllegalArgumentException("Invalid habit category: ${entity.category}"),
        frequency = habitFrequencyConverter.stringToHabitFrequency(entity.frequency)
            ?: throw IllegalArgumentException("Invalid habit frequency: ${entity.frequency}"),
        activeDays = dayOfWeekListConverter.stringToDayOfWeekSet(entity.activeDays),
        estimatedSeconds = entity.estimatedSeconds,
        microVersion = entity.microVersion,
        allowPartialCompletion = entity.allowPartialCompletion,
        subtasks = jsonListConverter.stringToStringList(entity.subtasks),
        phase = habitPhaseConverter.stringToHabitPhase(entity.phase)
            ?: throw IllegalArgumentException("Invalid habit phase: ${entity.phase}"),
        status = habitStatusConverter.stringToHabitStatus(entity.status)
            ?: throw IllegalArgumentException("Invalid habit status: ${entity.status}"),
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt),
        pausedAt = entity.pausedAt?.let { Instant.ofEpochMilli(it) },
        archivedAt = entity.archivedAt?.let { Instant.ofEpochMilli(it) },
        lapseThresholdDays = entity.lapseThresholdDays,
        relapseThresholdDays = entity.relapseThresholdDays
    )

    /**
     * Converts domain [Habit] to [HabitEntity].
     */
    fun toEntity(domain: Habit): HabitEntity {
        val anchorType = anchorTypeConverter.anchorTypeToString(domain.anchorType)
            ?: "AfterBehavior"
        val habitCategory = habitCategoryConverter.habitCategoryToString(domain.category)
            ?: "Morning"
        val habitFrequency = habitFrequencyConverter.habitFrequencyToString(domain.frequency)
            ?: "Daily"
        val habitPhase = habitPhaseConverter.habitPhaseToString(domain.phase)
            ?: "ONBOARD"
        val habitStatus = habitStatusConverter.habitStatusToString(domain.status)
            ?: "Active"

        return HabitEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            icon = domain.icon,
            color = domain.color,
            anchorBehavior = domain.anchorBehavior,
            anchorType = anchorType,
            timeWindowStart = domain.timeWindowStart,
            timeWindowEnd = domain.timeWindowEnd,
            category = habitCategory,
            frequency = habitFrequency,
            activeDays = dayOfWeekListConverter.dayOfWeekSetToString(domain.activeDays),
            estimatedSeconds = domain.estimatedSeconds,
            microVersion = domain.microVersion,
            allowPartialCompletion = domain.allowPartialCompletion,
            subtasks = jsonListConverter.stringListToString(domain.subtasks),
            phase = habitPhase,
            status = habitStatus,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
            pausedAt = domain.pausedAt?.toEpochMilli(),
            archivedAt = domain.archivedAt?.toEpochMilli(),
            lapseThresholdDays = domain.lapseThresholdDays,
            relapseThresholdDays = domain.relapseThresholdDays
        )
    }

    /**
     * Converts a list of [HabitEntity] to domain [List<Habit>].
     */
    fun toDomainList(entities: List<HabitEntity>): List<Habit> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [Habit] to [List<HabitEntity>].
     */
    fun toEntityList(domains: List<Habit>): List<HabitEntity> = domains.map { toEntity(it) }
}
