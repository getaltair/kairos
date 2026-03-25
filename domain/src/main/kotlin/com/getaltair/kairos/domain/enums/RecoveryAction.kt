package com.getaltair.kairos.domain.enums

sealed class RecoveryAction {
    abstract val displayName: String

    data object Resume : RecoveryAction() {
        override val displayName: String = "Resume"
    }

    data object Simplify : RecoveryAction() {
        override val displayName: String = "Simplify"
    }

    data object Pause : RecoveryAction() {
        override val displayName: String = "Pause"
    }

    data object Archive : RecoveryAction() {
        override val displayName: String = "Archive"
    }

    data object FreshStart : RecoveryAction() {
        override val displayName: String = "Fresh start"
    }
}
