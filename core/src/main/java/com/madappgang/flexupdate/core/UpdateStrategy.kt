package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType

class UpdateStrategy(private val config: UpdateConfig) {

    fun resolve(priority: Int, stalenessDays: Int): Int? = when {
        priority >= config.immediateMinPriority -> AppUpdateType.IMMEDIATE
        priority >= config.flexibleMinPriority && stalenessDays >= config.stalenessDaysForEscalation -> AppUpdateType.IMMEDIATE
        priority >= config.flexibleMinPriority -> AppUpdateType.FLEXIBLE
        else -> null
    }
}
