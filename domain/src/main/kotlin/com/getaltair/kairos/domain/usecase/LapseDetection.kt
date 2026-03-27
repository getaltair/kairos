package com.getaltair.kairos.domain.usecase

import java.util.UUID

data class LapseDetection(val habitId: UUID, val consecutiveMissedDays: Int)
