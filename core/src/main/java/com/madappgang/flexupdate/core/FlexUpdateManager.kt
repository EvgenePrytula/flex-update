package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.madappgang.flexupdate.core.UpdatePriority.*
import com.madappgang.flexupdate.core.UpdateStrategy.*
import com.madappgang.flexupdate.core.handlers.FlexibleUpdateHandler
import com.madappgang.flexupdate.core.handlers.ImmediateUpdateHandler
import com.madappgang.flexupdate.core.handlers.UpdateHandler


/**
 * Created by Eugene Prytula on 24.04.2025.
 */

class FlexUpdateManager private constructor(
    activity: ComponentActivity,
    updateStrategy: UpdateStrategy
) {

    companion object {
        private const val TAG = "FlexUpdateManager"

        fun from(
            activity: ComponentActivity,
            updateStrategy: UpdateStrategy = Auto
        ): FlexUpdateManager {
            return FlexUpdateManager(activity, updateStrategy)
        }
    }

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private val activityResultLauncher = activity.registerForActivityResult(
        StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Update complete")
        } else {
            Log.d(TAG, "Update canceled or failed")
            if (updateStrategy is Manual && updateStrategy.updatePriority == MEDIUM) {
                activity.finish()
            }
        }
    }

    private val updateHandler: UpdateHandler = when (updateStrategy.resolve(appUpdateManager)) {
        IMMEDIATE -> ImmediateUpdateHandler(activity, appUpdateManager, activityResultLauncher)
        FLEXIBLE -> FlexibleUpdateHandler(activity, appUpdateManager, activityResultLauncher)
        else -> throw IllegalStateException("Unknown update type")
    }

    fun checkForUpdate() = updateHandler.checkForUpdate()
}
