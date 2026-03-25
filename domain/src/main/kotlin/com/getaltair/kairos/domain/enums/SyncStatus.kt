package com.getaltair.kairos.domain.enums

sealed class SyncStatus {
    abstract val displayName: String

    data object LocalOnly : SyncStatus() {
        override val displayName: String = "Local only"
    }

    data object Synced : SyncStatus() {
        override val displayName: String = "Synced"
    }

    data object PendingSync : SyncStatus() {
        override val displayName: String = "Pending sync"
    }

    data object PendingDelete : SyncStatus() {
        override val displayName: String = "Pending delete"
    }

    data object Conflict : SyncStatus() {
        override val displayName: String = "Conflict"
    }
}
