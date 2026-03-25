package com.getaltair.kairos.domain.enums

sealed class RecoveryType {
    abstract val displayName: String

    data object Lapse : RecoveryType() {
        override val displayName: String = "Lapse"
    }

    data object Relapse : RecoveryType() {
        override val displayName: String = "Relapse"
    }
}
