package com.madappgang.flexupdate.core.handlers

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADED
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

internal class FlexibleUpdateHandler(
    private val activity: ComponentActivity,
    private val appUpdateManager: AppUpdateManager,
    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
) : UpdateHandler {

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    private val defaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            appUpdateManager.registerListener(listener)

            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startFlexibleUpdate(info)
                }
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            appUpdateManager.unregisterListener(listener)
        }
    }

    init {
        activity.lifecycle.addObserver(defaultLifecycleObserver)
    }

    private fun startFlexibleUpdate(info: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(FLEXIBLE).build()
        )
    }

    override fun startUpdateFlow(info: AppUpdateInfo) {
        startFlexibleUpdate(info)
    }
}

