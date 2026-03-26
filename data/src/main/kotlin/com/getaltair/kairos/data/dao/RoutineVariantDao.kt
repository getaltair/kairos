package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
     * Set a variant as default for its routine.
     * Uses @Transaction to ensure both updates execute atomically.
     * First clears all defaults for the routine, then sets the specified variant as default.
     */
    @Transaction
    @Query(
        """
        UPDATE routine_variants SET is_default = 0
        WHERE routine_id = :routineId AND is_default = 1;
        UPDATE routine_variants SET is_default = 1
        WHERE id = :id
    """
    )
    fun setAsDefault(id: UUID, routineId: UUID)

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
