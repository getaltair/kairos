package com.getaltair.kairos.notification

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationIdStrategyTest {

    @Test
    fun `reminderId returns non-negative masked hashCode`() {
        val habitId = UUID.randomUUID()
        assertEquals(habitId.hashCode() and 0x7FFFFFFF, NotificationIdStrategy.reminderId(habitId))
    }

    @Test
    fun `followUpId for followUp 1 equals masked hashCode plus 1001`() {
        val habitId = UUID.randomUUID()
        assertEquals((habitId.hashCode() and 0x7FFFFFFF) + 1001, NotificationIdStrategy.followUpId(habitId, 1))
    }

    @Test
    fun `followUpId for followUp 2 equals masked hashCode plus 1002`() {
        val habitId = UUID.randomUUID()
        assertEquals((habitId.hashCode() and 0x7FFFFFFF) + 1002, NotificationIdStrategy.followUpId(habitId, 2))
    }

    @Test
    fun `followUpId for followUp 3 equals masked hashCode plus 1003`() {
        val habitId = UUID.randomUUID()
        assertEquals((habitId.hashCode() and 0x7FFFFFFF) + 1003, NotificationIdStrategy.followUpId(habitId, 3))
    }

    @Test
    fun `ROUTINE_TIMER_ID is 9001`() {
        assertEquals(9001, NotificationIdStrategy.ROUTINE_TIMER_ID)
    }

    @Test
    fun `SYNC_STATUS_ID is 9002`() {
        assertEquals(9002, NotificationIdStrategy.SYNC_STATUS_ID)
    }

    @Test
    fun `different habit IDs produce different reminder IDs`() {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        assertNotEquals(
            NotificationIdStrategy.reminderId(habitId1),
            NotificationIdStrategy.reminderId(habitId2)
        )
    }

    @Test
    fun `snoozedId equals masked hashCode plus 500`() {
        val habitId = UUID.randomUUID()
        assertEquals((habitId.hashCode() and 0x7FFFFFFF) + 500, NotificationIdStrategy.snoozedId(habitId))
    }
}
