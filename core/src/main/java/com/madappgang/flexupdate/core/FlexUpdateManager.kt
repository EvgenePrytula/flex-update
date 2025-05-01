package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.madappgang.flexupdate.core.handlers.FlexibleUpdateHandler
import com.madappgang.flexupdate.core.handlers.ImmediateUpdateHandler
import com.madappgang.flexupdate.core.types.UpdateInfoResult
import com.madappgang.flexupdate.core.types.UpdatePriority
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdateStrategy
import com.madappgang.flexupdate.core.types.UpdateStrategy.Auto
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual
import com.madappgang.flexupdate.core.types.getUpdateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


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

    suspend fun checkForUpdate() = withContext(Dispatchers.IO) {
        runCatching {
            val updateInfoResult = appUpdateManager.getUpdateInfoResult(updateStrategy)
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

private suspend fun AppUpdateManager.getUpdateInfoResult(strategy: UpdateStrategy): UpdateInfoResult? {
    return suspendCancellableCoroutine { continuation ->
        try {
            val info = Tasks.await(appUpdateInfo)

            val updateType = when (strategy) {
                Auto -> {
                    val priority = UpdatePriority.fromPriority(info.updatePriority())
                    priority.getUpdateType()
                }

                is Manual -> strategy.updatePriority.getUpdateType()
            }


            if (updateType == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val isUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE

            if (isUpdateAvailable) {
                continuation.resume(
                    UpdateInfoResult(
                        updateType = updateType,
                        appUpdateInfo = info
                    )
                )
            } else {
                continuation.resume(null)
            }

        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
