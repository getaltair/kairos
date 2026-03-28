package com.getaltair.kairos.domain.model

import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RoutineHabit

data class RoutineStep(val routineHabit: RoutineHabit, val habit: Habit)
