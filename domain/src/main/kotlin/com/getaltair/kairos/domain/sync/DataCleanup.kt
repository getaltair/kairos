package com.getaltair.kairos.domain.sync

/**
 * Abstraction for data cleanup operations required during account deletion.
 *
 * Encapsulates both cloud (Firestore) and local (Room) data removal so the
 * domain layer can orchestrate account deletion without depending on concrete
 * infrastructure classes.
 *
 * Implemented in the app module where both the sync engine
 * and the local database are accessible.
 */
interface DataCleanup {

    /**
     * Deletes all Firestore cloud data for the given user, including all
     * subcollections and the user document itself.
     *
     * @param userId The Firebase Auth user ID whose data should be removed.
     * @throws Exception if any Firestore operation fails.
     */
    suspend fun deleteCloudData(userId: String)

    /**
     * Clears all local database tables, removing all persisted data.
     */
    suspend fun clearLocalData()
}
