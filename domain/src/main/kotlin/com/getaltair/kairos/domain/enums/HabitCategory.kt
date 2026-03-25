package com.getaltair.kairos.domain.enums

sealed class HabitCategory {
    abstract val displayName: String
    abstract val emoji: String

    data object Morning : HabitCategory() {
        override val displayName: String = "Morning"
        override val emoji: String = "\uD83C\uDF05"
    }

    data object Afternoon : HabitCategory() {
        override val displayName: String = "Afternoon"
        override val emoji: String = "\uD83C\uDF01"
    }

    data object Evening : HabitCategory() {
        override val displayName: String = "Evening"
        override val emoji: String = "\uD83C\uDF08"
    }

    data object Anytime : HabitCategory() {
        override val displayName: String = "Anytime"
        override val emoji: String = "\uD83D\uDDF5"
    }

    data object Departure : HabitCategory() {
        override val displayName: String = "Departure"
        override val emoji: String = "\uD83D\uDEA8"
    }
}
