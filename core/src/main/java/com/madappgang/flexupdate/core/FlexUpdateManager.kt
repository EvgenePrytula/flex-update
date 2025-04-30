package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.handlers.FlexibleUpdateHandler
import com.madappgang.flexupdate.core.handlers.ImmediateUpdateHandler
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdateStrategy
import com.madappgang.flexupdate.core.types.UpdateStrategy.Auto
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Created by Eugene Prytula on 24.04.2025.
 */

class FlexUpdateManager private constructor(
    private val activity: ComponentActivity,
    private val updateStrategy: UpdateStrategy
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

    private val appUpdateManager = FakeAppUpdateManager(activity).apply {
        setUpdateAvailable(UpdateAvailability.UPDATE_AVAILABLE, IMMEDIATE)
        setUpdatePriority(5)
        userAcceptsUpdate()
    }

    private val activityResultLauncher = activity.registerForActivityResult(
        StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Update complete!")
        } else {
            Log.d(TAG, "Update canceled or failed!")
            val isSkipNotAllowed =
                updateStrategy is Manual && updateStrategy.updatePriority == CRITICAL
            if (isSkipNotAllowed) {
                activity.finish()
            }
        }
    }

    suspend fun checkForUpdate() = withContext(Dispatchers.IO) {
        runCatching {
            val updateInfoResult = updateStrategy.resolve(appUpdateManager)
            val updateHandler = when (val updateType = updateInfoResult?.updateType) {
                IMMEDIATE -> withContext(Dispatchers.Main) {
                    ImmediateUpdateHandler(
                        activity,
                        appUpdateManager,
                        activityResultLauncher
                    )
                }

                FLEXIBLE -> withContext(Dispatchers.Main) {
                    FlexibleUpdateHandler(
                        activity,
                        appUpdateManager,
                        activityResultLauncher
                    )
                }

                else -> throw IllegalStateException("Unknown update type: $updateType")
            }
            updateHandler.startUpdateFlow(updateInfoResult.appUpdateInfo)
        }
    }
}
