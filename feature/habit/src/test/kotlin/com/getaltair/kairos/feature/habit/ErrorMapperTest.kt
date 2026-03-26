package com.getaltair.kairos.feature.habit

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `anchorBehavior blank message maps to anchor prompt`() {
        val result = ErrorMapper.toUserMessage("anchorBehavior must not be blank")

        assertEquals("Please describe when you'll do this habit.", result)
    }

    @Test
    fun `anchorBehavior blank message embedded in longer string maps to anchor prompt`() {
        val result = ErrorMapper.toUserMessage("Validation failed: anchorBehavior must not be blank for habit")

        assertEquals("Please describe when you'll do this habit.", result)
    }

    @Test
    fun `allowPartialCompletion message maps to generic error`() {
        val result = ErrorMapper.toUserMessage("allowPartialCompletion must be true")

        assertEquals("Something went wrong. Please try again.", result)
    }

    @Test
    fun `relapseThresholdDays message maps to settings error`() {
        val result = ErrorMapper.toUserMessage("relapseThresholdDays must be positive")

        assertEquals("Something went wrong with the habit settings. Please try again.", result)
    }

    @Test
    fun `lapseThresholdDays message maps to settings error`() {
        val result = ErrorMapper.toUserMessage("lapseThresholdDays must be at least 1")

        assertEquals("Something went wrong with the habit settings. Please try again.", result)
    }

    @Test
    fun `createdAt timestamp message maps to generic error`() {
        val result = ErrorMapper.toUserMessage("createdAt must not be in the future")

        assertEquals("Something went wrong. Please try again.", result)
    }

    @Test
    fun `pausedAt timestamp message maps to generic error`() {
        val result = ErrorMapper.toUserMessage("pausedAt is invalid")

        assertEquals("Something went wrong. Please try again.", result)
    }

    @Test
    fun `archivedAt timestamp message maps to generic error`() {
        val result = ErrorMapper.toUserMessage("archivedAt must be after createdAt")

        assertEquals("Something went wrong. Please try again.", result)
    }

    @Test
    fun `failed to insert habit maps to save error`() {
        val result = ErrorMapper.toUserMessage("Failed to insert habit: constraint violation")

        assertEquals("Could not save your habit. Please try again.", result)
    }

    @Test
    fun `failed to update habit maps to save error`() {
        val result = ErrorMapper.toUserMessage("Failed to update habit: row not found")

        assertEquals("Could not save your habit. Please try again.", result)
    }

    @Test
    fun `failed to delete habit maps to save error`() {
        val result = ErrorMapper.toUserMessage("Failed to delete habit: database locked")

        assertEquals("Could not save your habit. Please try again.", result)
    }

    @Test
    fun `unknown message maps to default fallback`() {
        val result = ErrorMapper.toUserMessage("Some completely unknown error")

        assertEquals("Something went wrong. Please try again.", result)
    }

    @Test
    fun `empty message maps to default fallback`() {
        val result = ErrorMapper.toUserMessage("")

        assertEquals("Something went wrong. Please try again.", result)
    }
}
