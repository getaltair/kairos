package com.getaltair.kairos.domain.enums

sealed class HabitStatus {
    abstract val displayName: String

    data object Active : HabitStatus() {
        override val displayName: String = "Active"
    }

    data object Paused : HabitStatus() {
        override val displayName: String = "Paused"
    }

    data object Archived : HabitStatus() {
        override val displayName: String = "Archived"
    }
}
