package com.getaltair.kairos.notification

sealed class ScheduleResult {
    data object Scheduled : ScheduleResult()
    data object ExactAlarmDenied : ScheduleResult()
}
