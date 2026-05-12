package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType
import com.madappgang.flexupdate.core.types.UpdateMode.Auto
import com.madappgang.flexupdate.core.types.UpdateMode.Manual
import com.madappgang.flexupdate.core.types.UpdatePriority
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdatePriority.HIGH
import com.madappgang.flexupdate.core.types.UpdatePriority.LOW

class UpdateStrategy(
    private val config: UpdateConfig,
) {
    fun resolve(
        priority: Int,
        stalenessDays: Int,
    ): Int? = resolveUpdateType(effectivePriorityFor(priority), stalenessDays)

    private fun effectivePriorityFor(playPriority: Int): UpdatePriority =
        when (val mode = config.mode) {
            is Auto -> UpdatePriority.fromLevel(playPriority)
            is Manual -> mode.minPriority
        }

    private fun resolveUpdateType(
        priority: UpdatePriority,
        stalenessDays: Int,
    ): Int? =
        when {
            priority >= CRITICAL -> AppUpdateType.IMMEDIATE
            priority >= HIGH && stalenessDays >= config.stalenessDaysForEscalation -> AppUpdateType.IMMEDIATE
            priority >= LOW -> AppUpdateType.FLEXIBLE
            else -> null
        }
}
