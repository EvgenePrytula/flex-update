package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType
import com.madappgang.flexupdate.core.types.UpdateMode.Auto
import com.madappgang.flexupdate.core.types.UpdateMode.Manual
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdatePriority.HIGH
import com.madappgang.flexupdate.core.types.UpdatePriority.LOW

class UpdateStrategy(
    private val config: UpdateConfig,
) {
    fun resolve(
        priority: Int,
        stalenessDays: Int,
    ): Int? =
        when (val mode = config.mode) {
            is Auto ->
                when {
                    priority >= CRITICAL.level -> AppUpdateType.IMMEDIATE
                    priority >= HIGH.level && stalenessDays >= config.stalenessDaysForEscalation -> AppUpdateType.IMMEDIATE
                    priority >= LOW.level -> AppUpdateType.FLEXIBLE
                    else -> null
                }

            is Manual ->
                when {
                    priority >= mode.minPriority.level -> AppUpdateType.IMMEDIATE
                    else -> null
                }
        }
}
