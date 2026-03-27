package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.getaltair.kairos.data.entity.RoutineVariantEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [RoutineVariantEntity] operations.
 * Provides CRUD and specialized queries for routine variant data.
 */
@Dao
interface RoutineVariantDao {

    /**
     * Get all routine variants.
     */
    @Query("SELECT * FROM routine_variants ORDER BY created_at DESC")
    fun getAll(): List<RoutineVariantEntity>

    /**
     * Get a routine variant by ID.
     */
    @Query("SELECT * FROM routine_variants WHERE id = :id")
    fun getById(id: UUID): RoutineVariantEntity?

    /**
     * Get all variants for a specific routine.
     */
    @Query(
        """
        SELECT * FROM routine_variants
        WHERE routine_id = :routineId
        ORDER BY is_default DESC, created_at DESC
    """
    )
    fun getByRoutineId(routineId: UUID): List<RoutineVariantEntity>

    /**
     * Get the default variant for a routine.
     */
    @Query(
        """
        SELECT * FROM routine_variants
        WHERE routine_id = :routineId AND is_default = 1
        LIMIT 1
    """
    )
    fun getDefaultForRoutine(routineId: UUID): RoutineVariantEntity?

    /**
     * Insert a new routine variant.
     */
    @Insert
    fun insert(variant: RoutineVariantEntity)

    /**
     * Insert multiple routine variants.
     */
    @Insert
    fun insertAll(variants: List<RoutineVariantEntity>)

    /**
     * Insert or update a routine variant.
     * If a variant with the same primary key exists, it is updated; otherwise, inserted.
     */
    @Upsert
    suspend fun upsert(entity: RoutineVariantEntity)

    /**
     * Update a routine variant.
     */
    @Query(
        """
        UPDATE routine_variants SET
            name = :name,
            estimated_minutes = :estimatedMinutes,
            is_default = :isDefault,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(id: UUID, name: String, estimatedMinutes: Int, isDefault: Boolean, updatedAt: Long)

    /**
     * Clears all default flags for variants of a routine.
     */
    @Query("UPDATE routine_variants SET is_default = 0 WHERE routine_id = :routineId AND is_default = 1")
    fun clearDefaults(routineId: UUID)

    /**
     * Marks a specific variant as default.
     */
    @Query("UPDATE routine_variants SET is_default = 1 WHERE id = :id")
    fun markAsDefault(id: UUID)

    /**
     * Set a variant as default for its routine.
     * Uses @Transaction to ensure both updates execute atomically.
     */
    @Transaction
    fun setAsDefault(id: UUID, routineId: UUID) {
        clearDefaults(routineId)
        markAsDefault(id)
    }

    /**
     * Delete a routine variant.
     */
    @Query("DELETE FROM routine_variants WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all variants for a routine.
     */
    @Query("DELETE FROM routine_variants WHERE routine_id = :routineId")
    fun deleteByRoutineId(routineId: UUID)

    /**
     * Delete all routine variants.
     */
    @Query("DELETE FROM routine_variants")
    fun deleteAll()

    /**
     * Get routine variants as Flow for reactive updates.
     */
    @Query("SELECT * FROM routine_variants ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<RoutineVariantEntity>>
}
