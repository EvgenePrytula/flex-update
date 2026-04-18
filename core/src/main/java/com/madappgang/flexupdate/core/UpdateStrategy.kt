package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType

class UpdateStrategy(private val config: UpdateConfig) {

    fun resolve(priority: Int, stalenessDays: Int): Int? = when {
        priority >= config.immediateMinPriority.level -> AppUpdateType.IMMEDIATE
        priority >= config.flexibleMinPriority.level && stalenessDays >= config.stalenessDaysForEscalation -> AppUpdateType.IMMEDIATE
        priority >= config.flexibleMinPriority.level -> AppUpdateType.FLEXIBLE
        else -> null
    }
}
