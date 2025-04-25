package com.madappgang.flexupdate.core

import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.madappgang.flexupdate.core.UpdatePriority.*


/**
 * Created by Eugene Prytula on 24.04.2025.
 */

enum class UpdatePriority(val priority: Int) {
    NONE(0),
    VERY_LOW(1),
    LOW(2),
    MEDIUM(3),
    HIGH(4),
    CRITICAL(5);

    companion object {
        fun fromPriority(priority: Int): UpdatePriority {
            return entries.find { it.priority == priority } ?: NONE
        }
    }
}

internal fun UpdatePriority.getUpdateType(): Int? {
    return when (this) {
        CRITICAL -> IMMEDIATE
        LOW, MEDIUM, HIGH -> FLEXIBLE
        else -> null
    }
}