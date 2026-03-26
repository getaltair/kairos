package com.getaltair.kairos.domain.enums

sealed class Blocker {
    abstract val displayName: String

    data object NoEnergy : Blocker() {
        override val displayName: String = "No energy"
    }

    data object PainPhysical : Blocker() {
        override val displayName: String = "Physical pain"
    }

    data object PainMental : Blocker() {
        override val displayName: String = "Mental pain"
    }

    data object TooBusy : Blocker() {
        override val displayName: String = "Too busy"
    }

    data object FamilyEmergency : Blocker() {
        override val displayName: String = "Family emergency"
    }

    data object WorkEmergency : Blocker() {
        override val displayName: String = "Work emergency"
    }

    data object Sick : Blocker() {
        override val displayName: String = "Sick"
    }

    data object Weather : Blocker() {
        override val displayName: String = "Weather"
    }

    data object EquipmentFailure : Blocker() {
        override val displayName: String = "Equipment failure"
    }

    data object Other : Blocker() {
        override val displayName: String = "Other"
    }
}
