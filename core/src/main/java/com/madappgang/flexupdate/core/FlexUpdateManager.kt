package com.jackprytula.flex_update

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADED
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.jackprytula.flex_update.UpdatePriority.MEDIUM


/**
 * Created by Eugene Prytula on 24.04.2025.
 */

class FlexUpdateManager private constructor(
    activity: ComponentActivity,
    defaultUpdatePriority: UpdatePriority
) {

    companion object {
        private const val TAG = "FlexUpdateManager"

        fun from(
            activity: ComponentActivity,
            priority: UpdatePriority = MEDIUM
        ): FlexUpdateManager {
            return FlexUpdateManager(activity, priority)
        }
    }

    private val appUpdateManager = AppUpdateManagerFactory.create(activity.applicationContext)
    private var updateType: Int? = defaultUpdatePriority.getUpdateType()

    private val activityResultLauncher =
        activity.registerForActivityResult(StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> Log.d(TAG, "Update flow completed!")
                else -> {
                    Log.d(TAG, "Update flow is failed or canceled!")
                    if (updateType == IMMEDIATE) {
                        activity.finish()
                    }
                }
            }
        }

    private val listener = InstallStateUpdatedListener { state ->
        checkInstallStatus(state.installStatus())
    }

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)

                if (updateType == FLEXIBLE) {
                    appUpdateManager.registerListener(listener)
                }

                appUpdateManager
                    .appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        checkInstallStatus(appUpdateInfo.installStatus())

                        if (appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                            updateType?.let { type ->
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    activityResultLauncher,
                                    AppUpdateOptions.newBuilder(type).build()
                                )
                            }
                        }
                    }
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                if (updateType == FLEXIBLE) {
                    appUpdateManager.unregisterListener(listener)
                }
            }
        })
    }

    private fun checkInstallStatus(installStatus: Int) {
        if (installStatus == DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    fun checkForUpdate() {
        val taskUpdateInfo = appUpdateManager.appUpdateInfo
        taskUpdateInfo.addOnSuccessListener { info ->

            val updatePriority = UpdatePriority.fromPriority(info.updatePriority())
            updateType = updatePriority.getUpdateType()

            if (updateType == null) {
                Log.d(TAG, "No update required for priority: ${info.updatePriority()}")
                return@addOnSuccessListener
            }

            updateType?.let { type ->
                val isAppUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE
                val isUpdateAllowed = info.isUpdateTypeAllowed(type)
                val updateOptions = AppUpdateOptions.newBuilder(type).build()

                if (isAppUpdateAvailable && isUpdateAllowed) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activityResultLauncher,
                        updateOptions
                    )
                }
            }
        }.addOnFailureListener {
            Log.e(TAG, it.toString())
        }
    }
}