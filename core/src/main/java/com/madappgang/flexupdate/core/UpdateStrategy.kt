package com.madappgang.flexupdate.core

import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.madappgang.flexupdate.core.UpdatePriority.MEDIUM


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

sealed class UpdateStrategy {

    data class Manual(
        val updatePriority: UpdatePriority = MEDIUM
    ) : UpdateStrategy() {
        override fun resolve(appUpdateManager: AppUpdateManager): Int? {
            return updatePriority.getUpdateType()
        }
    }

    data object Auto : UpdateStrategy() {
        override fun resolve(appUpdateManager: AppUpdateManager): Int? {
            return try {
                val info = Tasks.await(appUpdateManager.appUpdateInfo)
                val priority = UpdatePriority.fromPriority(info.updatePriority())
                priority.getUpdateType()
            } catch (e: Exception) {
                null
            }
        }
    }

    abstract fun resolve(appUpdateManager: AppUpdateManager): Int?
}
