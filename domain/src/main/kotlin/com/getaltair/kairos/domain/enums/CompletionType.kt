package com.getaltair.kairos.domain.enums

sealed class CompletionType {
    abstract val displayName: String

    data object Full : CompletionType() {
        override val displayName: String = "Done"
    }

    data object Partial : CompletionType() {
        override val displayName: String = "Partial"
    }

    data object Skipped : CompletionType() {
        override val displayName: String = "Skipped"
    }

    data object Missed : CompletionType() {
        override val displayName: String = "Missed"
    }
}
