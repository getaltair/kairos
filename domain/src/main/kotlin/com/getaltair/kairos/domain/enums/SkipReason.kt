package com.getaltair.kairos.domain.enums

sealed class SkipReason {
    abstract val displayName: String

    data object TooTired : SkipReason() {
        override val displayName: String = "Too tired"
    }

    data object NoTime : SkipReason() {
        override val displayName: String = "No time"
    }

    data object NotFeelingWell : SkipReason() {
        override val displayName: String = "Not feeling well"
    }

    data object Traveling : SkipReason() {
        override val displayName: String = "Traveling"
    }

    data object TookDayOff : SkipReason() {
        override val displayName: String = "Took day off"
    }

    data object Other : SkipReason() {
        override val displayName: String = "Other"
    }
}
