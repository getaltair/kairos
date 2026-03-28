package com.getaltair.kairos.domain.model

import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineHabit

data class RoutineWithHabits(val routine: Routine, val habits: List<RoutineHabit>)
