package com.madappgang.flexupdate.core.handlers

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

    private val defaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startImmediateUpdate(info)
                }
            }
        }
    }

    init {
        activity.lifecycle.addObserver(defaultLifecycleObserver)
    }

    private fun startImmediateUpdate(info: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(IMMEDIATE).build()
        )
    }

    override fun startUpdateFlow(info: AppUpdateInfo) {
        startImmediateUpdate(info)
    }
}

