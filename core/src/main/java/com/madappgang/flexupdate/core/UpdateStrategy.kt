package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType
import com.madappgang.flexupdate.core.types.UpdateMode.Auto
import com.madappgang.flexupdate.core.types.UpdateMode.Manual
import com.madappgang.flexupdate.core.types.UpdatePriority
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdatePriority.HIGH
import com.madappgang.flexupdate.core.types.UpdatePriority.LOW
import com.madappgang.flexupdate.core.types.UpdatePriority.NONE

class UpdateStrategy(
    private val config: UpdateConfig,
) {
    fun resolve(
        priority: Int,
        stalenessDays: Int,
    ): Int? {
        val effectivePriority =
            when (val mode = config.mode) {
                is Auto -> UpdatePriority.entries.firstOrNull { it.level == priority } ?: NONE
                is Manual -> mode.minPriority
            }
        return resolveUpdateType(effectivePriority, stalenessDays)
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
