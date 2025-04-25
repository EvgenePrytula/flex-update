package com.madappgang.flexupdate.core.handlers

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

internal class ImmediateUpdateHandler(
    activity: ComponentActivity,
    private val appUpdateManager: AppUpdateManager,
    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
) : UpdateHandler {

    companion object {
        private const val TAG = "ImmediateUpdateHandler"
    }

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                    if (info.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        startImmediateUpdate(info)
                    }
                }
            }
        })
    }

    override fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val isAvailable = info.updateAvailability() == UPDATE_AVAILABLE
            val isAllowed = info.isUpdateTypeAllowed(IMMEDIATE)

            if (isAvailable && isAllowed) {
                startImmediateUpdate(info)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Check failed: $it")
        }
    }

    private fun startImmediateUpdate(info: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(IMMEDIATE).build()
        )
    }
}

