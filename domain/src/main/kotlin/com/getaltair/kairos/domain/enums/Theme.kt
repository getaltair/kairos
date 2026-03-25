package com.getaltair.kairos.domain.enums

sealed class Theme {
    abstract val displayName: String

    data object System : Theme() {
        override val displayName: String = "System"
    }

    data object Light : Theme() {
        override val displayName: String = "Light"
    }

    data object Dark : Theme() {
        override val displayName: String = "Dark"
    }
}
