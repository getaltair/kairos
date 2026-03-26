package com.getaltair.kairos.domain.enums

sealed class RoutineStatus {
    abstract val displayName: String

    data object Active : RoutineStatus() {
        override val displayName: String = "Active"
    }

    data object Paused : RoutineStatus() {
        override val displayName: String = "Paused"
    }

    data object Archived : RoutineStatus() {
        override val displayName: String = "Archived"
    }
}
