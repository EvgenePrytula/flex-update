package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.madappgang.flexupdate.core.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.UpdateStrategy.Auto
import com.madappgang.flexupdate.core.UpdateStrategy.Manual
import com.madappgang.flexupdate.core.handlers.FlexibleUpdateHandler
import com.madappgang.flexupdate.core.handlers.ImmediateUpdateHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
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

    fun checkForUpdate() {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updateInfoResult = updateStrategy.resolve(appUpdateManager)
                val updateHandler = when (val updateType = updateInfoResult?.updateType) {
                    IMMEDIATE -> ImmediateUpdateHandler(
                        activity,
                        appUpdateManager,
                        activityResultLauncher
                    )

                    FLEXIBLE -> FlexibleUpdateHandler(
                        activity,
                        appUpdateManager,
                        activityResultLauncher
                    )

                    else -> throw IllegalStateException("Unknown update type: $updateType")
                }
                updateHandler.startUpdateFlow(updateInfoResult.appUpdateInfo)

            } catch (e: Exception) {
                Log.e(TAG, e.printStackTrace().toString())
            }
        }
    }
}
