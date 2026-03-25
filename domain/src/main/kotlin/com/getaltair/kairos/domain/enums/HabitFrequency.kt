package com.getaltair.kairos.domain.enums

sealed class HabitFrequency {
    abstract val displayName: String

    data object Daily : HabitFrequency() {
        override val displayName: String = "Daily"
    }

    data object Weekdays : HabitFrequency() {
        override val displayName: String = "Weekdays"
    }

    data object Weekends : HabitFrequency() {
        override val displayName: String = "Weekends"
    }

    data object Custom : HabitFrequency() {
        override val displayName: String = "Custom"
    }
}
