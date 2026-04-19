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
    fun `manual - NONE minPriority returns null`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.NONE)))
        assertNull(strategy.resolve(UpdatePriority.URGENT.level, staleness = 0))
    }

    @Test
    fun `manual - LOW minPriority returns FLEXIBLE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.LOW)))
        assertEquals(AppUpdateType.FLEXIBLE, strategy.resolve(UpdatePriority.NONE.level, staleness = 0))
    }

    @Test
    fun `manual - HIGH minPriority below staleness threshold returns FLEXIBLE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH), stalenessDaysForEscalation = 7))
        assertEquals(AppUpdateType.FLEXIBLE, strategy.resolve(UpdatePriority.NONE.level, staleness = 6))
    }

    @Test
    fun `manual - HIGH minPriority at staleness threshold returns IMMEDIATE`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH), stalenessDaysForEscalation = 7))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.NONE.level, staleness = 7))
    }

    @Test
    fun `manual - CRITICAL minPriority returns IMMEDIATE regardless of staleness`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.CRITICAL)))
        assertEquals(AppUpdateType.IMMEDIATE, strategy.resolve(UpdatePriority.NONE.level, staleness = 0))
    }

    @Test
    fun `manual - store priority is ignored`() {
        val strategy = UpdateStrategy(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.LOW)))
        assertEquals(
            strategy.resolve(UpdatePriority.NONE.level, staleness = 0),
            strategy.resolve(UpdatePriority.URGENT.level, staleness = 0),
        )
    }
}

private fun UpdateStrategy.resolve(
    priority: Int,
    staleness: Int,
) = resolve(priority, staleness)
