package com.getaltair.kairos.domain.enums

sealed class SessionStatus {
    abstract val displayName: String

    data object Pending : SessionStatus() {
        override val displayName: String = "Pending"
    }

    data object Completed : SessionStatus() {
        override val displayName: String = "Completed"
    }

    data object Abandoned : SessionStatus() {
        override val displayName: String = "Abandoned"
    }
}
