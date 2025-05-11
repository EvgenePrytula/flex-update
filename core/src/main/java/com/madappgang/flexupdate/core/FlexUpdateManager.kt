package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.madappgang.flexupdate.core.handlers.FlexibleUpdateHandler
import com.madappgang.flexupdate.core.handlers.ImmediateUpdateHandler
import com.madappgang.flexupdate.core.types.UpdateInfoResult
import com.madappgang.flexupdate.core.types.UpdatePriority
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
    private val updateStrategy: UpdateStrategy,
    isTesting: Boolean
) {

    companion object {
        private const val TAG = "FlexUpdateManager"

        fun from(
            activity: ComponentActivity,
            updateStrategy: UpdateStrategy = Auto,
            isTesting: Boolean = false
        ): FlexUpdateManager {
            return FlexUpdateManager(activity, updateStrategy, isTesting)
        }
    }

    private val appUpdateManager = if (isTesting) {
        FakeAppUpdateManager(activity).apply {
            setUpdateAvailable(UPDATE_AVAILABLE, IMMEDIATE)
            setUpdatePriority(5)
            userAcceptsUpdate()
        }
    } else {
        AppUpdateManagerFactory.create(activity)
    }

    private var updateType: Int? = null

    private val activityResultLauncher = activity.registerForActivityResult(
        StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Update complete!")
        } else {
            Log.d(TAG, "Update canceled or failed!")
            if (updateType == IMMEDIATE) {
                activity.finish()
            }
        }
    }

    suspend fun checkForUpdate() = withContext(Dispatchers.IO) {
        runCatching {
            val updateInfoResult = appUpdateManager.getUpdateInfoResult(updateStrategy)
            updateType = updateInfoResult?.updateType
            val updateHandler = when (updateType) {
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

            if (updateInfoResult != null) {
                updateHandler.startUpdateFlow(updateInfoResult.appUpdateInfo)
            }
        }
    }
}

private suspend fun AppUpdateManager.getUpdateInfoResult(strategy: UpdateStrategy): UpdateInfoResult? {
    return suspendCancellableCoroutine { continuation ->
        try {
            val info = Tasks.await(appUpdateInfo)

            val updatePriority = when (strategy) {
                Auto -> UpdatePriority.fromPriority(info.updatePriority())
                is Manual -> strategy.updatePriority
            }
            val updateType = updatePriority.getUpdateType()


            if (updateType == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val isUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE
            val isUpdateTypeAllowed = info.isUpdateTypeAllowed(updateType)

            if (isUpdateAvailable && isUpdateTypeAllowed) {
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
