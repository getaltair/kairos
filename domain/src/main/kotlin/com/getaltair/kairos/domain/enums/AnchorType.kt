package com.getaltair.kairos.domain.enums

sealed class AnchorType {
    abstract val displayName: String

    data object AfterBehavior : AnchorType() {
        override val displayName: String = "After..."
    }

    data object BeforeBehavior : AnchorType() {
        override val displayName: String = "Before..."
    }

    data object AtLocation : AnchorType() {
        override val displayName: String = "At location"
    }

    data object AtTime : AnchorType() {
        override val displayName: String = "At time"
    }
}
