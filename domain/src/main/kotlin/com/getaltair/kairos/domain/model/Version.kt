package com.getaltair.kairos.domain.model

/**
 * Represents an optimistic locking version for conflict detection.
 * Used for sync scenarios to detect concurrent modifications.
 */
data class Version(val value: Long = 0) {
    companion object {
        val ZERO = Version(0L)
    }

    /**
     * Increments version value and returns a new Version instance.
     *
     * @return a new Version with incremented value
     */
    fun increment(): Version = Version(value + 1)

    /**
     * Checks if this version is newer than another version.
     *
     * @param other The version to compare against
     * @return true if this version is newer, false otherwise
     */
    fun isNewerThan(other: Version): Boolean = value > other.value
}
