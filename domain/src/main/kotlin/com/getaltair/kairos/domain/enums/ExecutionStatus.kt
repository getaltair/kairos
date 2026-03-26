package com.getaltair.kairos.domain.enums

sealed class ExecutionStatus {
    abstract val displayName: String

    data object NotStarted : ExecutionStatus() {
        override val displayName: String = "Not started"
    }

    data object InProgress : ExecutionStatus() {
        override val displayName: String = "In progress"
    }

    data object Paused : ExecutionStatus() {
        override val displayName: String = "Paused"
    }

    data object Completed : ExecutionStatus() {
        override val displayName: String = "Completed"
    }

    data object Abandoned : ExecutionStatus() {
        override val displayName: String = "Abandoned"
    }
}
