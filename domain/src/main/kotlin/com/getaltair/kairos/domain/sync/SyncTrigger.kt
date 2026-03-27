package com.getaltair.kairos.domain.sync

/**
 * Abstraction for triggering Firestore sync operations from the data layer.
 *
 * Repositories call these methods after successful local Room writes.
 * The implementation (in the sync module) resolves the entity type,
 * converts the domain entity to a Firestore-compatible map, and pushes
 * the change via [com.getaltair.kairos.sync.SyncManager].
 *
 * All calls are fire-and-forget: callers should launch these in a
 * non-blocking scope so that local operations are never blocked by sync.
 */
interface SyncTrigger {

    /**
     * Pushes a local entity change to Firestore.
     *
     * @param userId     The owning user's ID.
     * @param entityType Canonical entity type name (e.g. "HABIT", "COMPLETION").
     * @param id         The entity's primary key as a string.
     * @param entity     The domain entity object. The implementation casts this
     *                   to the correct type and builds the Firestore map.
     */
    suspend fun triggerPush(userId: String, entityType: String, id: String, entity: Any)

    /**
     * Pushes a local deletion to Firestore.
     *
     * @param userId     The owning user's ID.
     * @param entityType Canonical entity type name (e.g. "HABIT", "COMPLETION").
     * @param id         The entity's primary key as a string.
     */
    suspend fun triggerDeletion(userId: String, entityType: String, id: String)
}
