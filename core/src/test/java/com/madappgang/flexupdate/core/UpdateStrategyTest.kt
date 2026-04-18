package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType
import com.madappgang.flexupdate.core.types.UpdateMode
import com.madappgang.flexupdate.core.types.UpdatePriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateStrategyTest {

    // ── Auto mode ──────────────────────────────────────────────────────────────

    @Test
    fun `auto - NONE priority returns null`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto))
        assertNull(strategy.resolve(UpdatePriority.NONE.level, staleness = 0))
    }

    @Test
    fun `auto - LOW priority returns FLEXIBLE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto))
        assertEquals(AppUpdateType.FLEXIBLE, strategy.resolve(UpdatePriority.LOW.level, staleness = 0))
    }

    @Test
    fun `auto - MEDIUM priority returns FLEXIBLE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto))
        assertEquals(AppUpdateType.FLEXIBLE, strategy.resolve(UpdatePriority.MEDIUM.level, staleness = 0))
    }

    @Test
    fun `auto - HIGH priority below staleness threshold returns FLEXIBLE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto, stalenessDaysForEscalation = 7))
        assertEquals(AppUpdateType.FLEXIBLE, strategy.resolve(UpdatePriority.HIGH.level, staleness = 6))
    }

    @Test
    fun `auto - HIGH priority at staleness threshold returns IMMEDIATE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto, stalenessDaysForEscalation = 7))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.HIGH.level, staleness = 7))
    }

    @Test
    fun `auto - CRITICAL priority returns IMMEDIATE regardless of staleness`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.CRITICAL.level, staleness = 0))
    }

    @Test
    fun `auto - URGENT priority returns IMMEDIATE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Auto))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.URGENT.level, staleness = 0))
    }

    // ── Manual mode ────────────────────────────────────────────────────────────

    @Test
    fun `manual - priority below minimum returns null`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH)))
        assertNull(strategy.resolve(UpdatePriority.MEDIUM.level, staleness = 0))
    }

    @Test
    fun `manual - priority at minimum returns IMMEDIATE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH)))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.HIGH.level, staleness = 0))
    }

    @Test
    fun `manual - priority above minimum returns IMMEDIATE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH)))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.URGENT.level, staleness = 0))
    }
}

private fun UpdateStrategy.resolve(priority: Int, staleness: Int) = resolve(priority, staleness)
