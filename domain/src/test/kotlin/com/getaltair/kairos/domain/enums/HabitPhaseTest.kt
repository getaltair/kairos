package com.getaltair.kairos.domain.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitPhaseTest {

    // Valid transitions tests

    @Test
    fun `shouldReturnTrue_whenTransitionFromOnboardToForming`() {
        assertTrue(
            HabitPhase.ONBOARD.canTransitionTo(HabitPhase.FORMING),
            "ONBOARD should allow transition to FORMING"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromFormingToMaintaining`() {
        assertTrue(
            HabitPhase.FORMING.canTransitionTo(HabitPhase.MAINTAINING),
            "FORMING should allow transition to MAINTAINING"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromFormingToLapsed`() {
        assertTrue(
            HabitPhase.FORMING.canTransitionTo(HabitPhase.LAPSED),
            "FORMING should allow transition to LAPSED"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromMaintainingToLapsed`() {
        assertTrue(
            HabitPhase.MAINTAINING.canTransitionTo(HabitPhase.LAPSED),
            "MAINTAINING should allow transition to LAPSED"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromLapsedToForming`() {
        assertTrue(
            HabitPhase.LAPSED.canTransitionTo(HabitPhase.FORMING),
            "LAPSED should allow transition to FORMING (quick return)"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromLapsedToRelapsed`() {
        assertTrue(
            HabitPhase.LAPSED.canTransitionTo(HabitPhase.RELAPSED),
            "LAPSED should allow transition to RELAPSED (extended absence)"
        )
    }

    @Test
    fun `shouldReturnTrue_whenTransitionFromRelapsedToForming`() {
        assertTrue(
            HabitPhase.RELAPSED.canTransitionTo(HabitPhase.FORMING),
            "RELAPSED should allow transition to FORMING (fresh start)"
        )
    }

    // Invalid transitions tests

    @Test
    fun `shouldReturnFalse_whenTransitionFromOnboardToMaintaining`() {
        assertFalse(
            HabitPhase.ONBOARD.canTransitionTo(HabitPhase.MAINTAINING),
            "ONBOARD should not allow direct transition to MAINTAINING"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromOnboardToLapsed`() {
        assertFalse(
            HabitPhase.ONBOARD.canTransitionTo(HabitPhase.LAPSED),
            "ONBOARD should not allow direct transition to LAPSED"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromOnboardToRelapsed`() {
        assertFalse(
            HabitPhase.ONBOARD.canTransitionTo(HabitPhase.RELAPSED),
            "ONBOARD should not allow direct transition to RELAPSED"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromFormingToOnboard`() {
        assertFalse(
            HabitPhase.FORMING.canTransitionTo(HabitPhase.ONBOARD),
            "FORMING should not allow transition back to ONBOARD"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromFormingToRelapsed`() {
        assertFalse(
            HabitPhase.FORMING.canTransitionTo(HabitPhase.RELAPSED),
            "FORMING should not allow direct transition to RELAPSED"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromMaintainingToOnboard`() {
        assertFalse(
            HabitPhase.MAINTAINING.canTransitionTo(HabitPhase.ONBOARD),
            "MAINTAINING should not allow transition back to ONBOARD"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromMaintainingToForming`() {
        assertFalse(
            HabitPhase.MAINTAINING.canTransitionTo(HabitPhase.FORMING),
            "MAINTAINING should not allow transition back to FORMING"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromMaintainingToRelapsed`() {
        assertFalse(
            HabitPhase.MAINTAINING.canTransitionTo(HabitPhase.RELAPSED),
            "MAINTAINING should not allow direct transition to RELAPSED"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromLapsedToOnboard`() {
        assertFalse(
            HabitPhase.LAPSED.canTransitionTo(HabitPhase.ONBOARD),
            "LAPSED should not allow transition back to ONBOARD"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromLapsedToMaintaining`() {
        assertFalse(
            HabitPhase.LAPSED.canTransitionTo(HabitPhase.MAINTAINING),
            "LAPSED should not allow transition back to MAINTAINING"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromRelapsedToOnboard`() {
        assertFalse(
            HabitPhase.RELAPSED.canTransitionTo(HabitPhase.ONBOARD),
            "RELAPSED should not allow transition back to ONBOARD"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromRelapsedToMaintaining`() {
        assertFalse(
            HabitPhase.RELAPSED.canTransitionTo(HabitPhase.MAINTAINING),
            "RELAPSED should not allow direct transition to MAINTAINING"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromRelapsedToLapsed`() {
        assertFalse(
            HabitPhase.RELAPSED.canTransitionTo(HabitPhase.LAPSED),
            "RELAPSED should not allow transition to LAPSED"
        )
    }

    // Edge cases tests

    @Test
    fun `shouldReturnFalse_whenTransitionFromOnboardToSameState`() {
        assertFalse(
            HabitPhase.ONBOARD.canTransitionTo(HabitPhase.ONBOARD),
            "ONBOARD should not allow self-transition"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromFormingToSameState`() {
        assertFalse(
            HabitPhase.FORMING.canTransitionTo(HabitPhase.FORMING),
            "FORMING should not allow self-transition"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromMaintainingToSameState`() {
        assertFalse(
            HabitPhase.MAINTAINING.canTransitionTo(HabitPhase.MAINTAINING),
            "MAINTAINING should not allow self-transition"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromLapsedToSameState`() {
        assertFalse(
            HabitPhase.LAPSED.canTransitionTo(HabitPhase.LAPSED),
            "LAPSED should not allow self-transition"
        )
    }

    @Test
    fun `shouldReturnFalse_whenTransitionFromRelapsedToSameState`() {
        assertFalse(
            HabitPhase.RELAPSED.canTransitionTo(HabitPhase.RELAPSED),
            "RELAPSED should not allow self-transition"
        )
    }

    // VALID_TRANSITIONS map tests

    @Test
    fun `VALID_TRANSITIONS_map_containsCorrectKeys`() {
        assertEquals(5, HabitPhase.VALID_TRANSITIONS.keys.size)
        assertTrue(HabitPhase.VALID_TRANSITIONS.containsKey(HabitPhase.ONBOARD))
        assertTrue(HabitPhase.VALID_TRANSITIONS.containsKey(HabitPhase.FORMING))
        assertTrue(HabitPhase.VALID_TRANSITIONS.containsKey(HabitPhase.MAINTAINING))
        assertTrue(HabitPhase.VALID_TRANSITIONS.containsKey(HabitPhase.LAPSED))
        assertTrue(HabitPhase.VALID_TRANSITIONS.containsKey(HabitPhase.RELAPSED))
    }

    @Test
    fun `VALID_TRANSITIONS_map_containsCorrectValues`() {
        assertEquals(listOf(HabitPhase.FORMING), HabitPhase.VALID_TRANSITIONS[HabitPhase.ONBOARD])
        assertEquals(
            listOf(HabitPhase.MAINTAINING, HabitPhase.LAPSED),
            HabitPhase.VALID_TRANSITIONS[HabitPhase.FORMING]
        )
        assertEquals(
            listOf(HabitPhase.LAPSED),
            HabitPhase.VALID_TRANSITIONS[HabitPhase.MAINTAINING]
        )
        assertEquals(
            listOf(HabitPhase.FORMING, HabitPhase.RELAPSED),
            HabitPhase.VALID_TRANSITIONS[HabitPhase.LAPSED]
        )
        assertEquals(listOf(HabitPhase.FORMING), HabitPhase.VALID_TRANSITIONS[HabitPhase.RELAPSED])
    }

    // Additional property tests

    @Test
    fun `All_phases_have_display_names`() {
        assertNotNull(HabitPhase.ONBOARD.displayName)
        assertNotNull(HabitPhase.FORMING.displayName)
        assertNotNull(HabitPhase.MAINTAINING.displayName)
        assertNotNull(HabitPhase.LAPSED.displayName)
        assertNotNull(HabitPhase.RELAPSED.displayName)

        assertEquals("Onboarding", HabitPhase.ONBOARD.displayName)
        assertEquals("Forming", HabitPhase.FORMING.displayName)
        assertEquals("Maintaining", HabitPhase.MAINTAINING.displayName)
        assertEquals("Lapsed", HabitPhase.LAPSED.displayName)
        assertEquals("Relapsed", HabitPhase.RELAPSED.displayName)
    }

    @Test
    fun `ALL_list_containsAllPhases`() {
        assertEquals(5, HabitPhase.ALL.size)
        assertTrue(HabitPhase.ALL.contains(HabitPhase.ONBOARD))
        assertTrue(HabitPhase.ALL.contains(HabitPhase.FORMING))
        assertTrue(HabitPhase.ALL.contains(HabitPhase.MAINTAINING))
        assertTrue(HabitPhase.ALL.contains(HabitPhase.LAPSED))
        assertTrue(HabitPhase.ALL.contains(HabitPhase.RELAPSED))
    }

    @Test
    fun `fromDisplayName_findsCorrectPhase`() {
        assertEquals(HabitPhase.ONBOARD, HabitPhase.fromDisplayName("Onboarding"))
        assertEquals(HabitPhase.FORMING, HabitPhase.fromDisplayName("Forming"))
        assertEquals(HabitPhase.MAINTAINING, HabitPhase.fromDisplayName("Maintaining"))
        assertEquals(HabitPhase.LAPSED, HabitPhase.fromDisplayName("Lapsed"))
        assertEquals(HabitPhase.RELAPSED, HabitPhase.fromDisplayName("Relapsed"))
        assertNull(HabitPhase.fromDisplayName("Invalid"))
    }

    @Test
    fun `fromDisplayName_isCaseInsensitive`() {
        assertEquals(HabitPhase.ONBOARD, HabitPhase.fromDisplayName("ONBOARDING"))
        assertEquals(HabitPhase.FORMING, HabitPhase.fromDisplayName("FORMING"))
        assertEquals(HabitPhase.MAINTAINING, HabitPhase.fromDisplayName("maintaining"))
    }

    @Test
    fun `fromSimpleName_findsCorrectPhase`() {
        assertEquals(HabitPhase.ONBOARD, HabitPhase.fromSimpleName("ONBOARD"))
        assertEquals(HabitPhase.FORMING, HabitPhase.fromSimpleName("FORMING"))
        assertEquals(HabitPhase.MAINTAINING, HabitPhase.fromSimpleName("MAINTAINING"))
        assertEquals(HabitPhase.LAPSED, HabitPhase.fromSimpleName("LAPSED"))
        assertEquals(HabitPhase.RELAPSED, HabitPhase.fromSimpleName("RELAPSED"))
        assertNull(HabitPhase.fromSimpleName("Invalid"))
    }

    @Test
    fun `fromSimpleName_isCaseInsensitive`() {
        assertEquals(HabitPhase.ONBOARD, HabitPhase.fromSimpleName("onboard"))
        assertEquals(HabitPhase.FORMING, HabitPhase.fromSimpleName("Forming"))
        assertEquals(HabitPhase.MAINTAINING, HabitPhase.fromSimpleName("MAINTAINING"))
    }
}
