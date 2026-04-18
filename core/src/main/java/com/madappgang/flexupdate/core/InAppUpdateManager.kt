package com.madappgang.flexupdate.core

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.DownloadState.Idle
import com.madappgang.flexupdate.core.DownloadState.Installing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class InAppUpdateManager private constructor(
    activity: AppCompatActivity,
    private val config: UpdateConfig,
    managerProvider: AppUpdateManagerProvider
) : DefaultLifecycleObserver {

    private val activityRef = WeakReference(activity)
    private var onOutcome: ((UpdateOutcome) -> Unit)? = null
    private val appUpdateManager: AppUpdateManager = managerProvider.provide(activity)

    private val _downloadState = MutableStateFlow<DownloadState>(Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleActivityResult(result.resultCode)
        }

    private val installStateListener: InstallStateUpdatedListener =
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val total: Long = state.totalBytesToDownload()
                    val downloaded: Long = state.bytesDownloaded()
                    val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _downloadState.value = DownloadState.InProgress(percent)
                }

                InstallStatus.DOWNLOADED -> {
                    _downloadState.value = DownloadState.Completed
                    invokeOutcome(UpdateOutcome.ReadyToInstall)
                }

                InstallStatus.INSTALLING -> {
                    _downloadState.value = Installing
                }

                InstallStatus.FAILED -> {
                    val code = state.installErrorCode()
                    _downloadState.value = DownloadState.Failed(code)
                    invokeOutcome(UpdateOutcome.Failed(code))
                    appUpdateManager.unregisterListener(installStateListener)
                }

                InstallStatus.CANCELED -> {
                    appUpdateManager.unregisterListener(installStateListener)
                }

                else -> Unit
            }
        }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) = resumeIfNeeded()

    override fun onStop(owner: LifecycleOwner) {
        appUpdateManager.unregisterListener(installStateListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onOutcome = null
        activityRef.clear()
        owner.lifecycle.removeObserver(this)
    }

    fun startUpdate(onOutcome: (UpdateOutcome) -> Unit) {
        this.onOutcome = onOutcome
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val priority = info.updatePriority()
                val staleness = info.clientVersionStalenessDays() ?: 0
                val updateType = UpdateStrategy(config).resolve(priority, staleness)

                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE || updateType == null) {
                    invokeOutcome(UpdateOutcome.NotAvailable)
                    return@addOnSuccessListener
                }
                launchFlow(info, updateType)
            }
            .addOnFailureListener {
                invokeOutcome(UpdateOutcome.Failed(-1))
            }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        _downloadState.value = Installing
    }

    private fun resumeIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    launchFlow(info, AppUpdateType.IMMEDIATE)

                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    _downloadState.value = DownloadState.Completed
                    invokeOutcome(UpdateOutcome.ReadyToInstall)
                }
            }
        }
    }

    private fun launchFlow(info: AppUpdateInfo, updateType: Int) {
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateListener)
        }
        appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(updateType).build()
        )
    }

    private fun handleActivityResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> invokeOutcome(UpdateOutcome.Accepted)
            Activity.RESULT_CANCELED -> invokeOutcome(UpdateOutcome.Declined)
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED ->
                invokeOutcome(UpdateOutcome.Failed(ActivityResult.RESULT_IN_APP_UPDATE_FAILED))
        }
    }

    private fun invokeOutcome(outcome: UpdateOutcome) {
        onOutcome?.invoke(outcome)
    }

    class Builder(private val activity: AppCompatActivity) {
        private var config = UpdateConfig.Builder().build()
        private var managerProvider: AppUpdateManagerProvider = DefaultAppUpdateManagerProvider()

        fun config(config: UpdateConfig) = apply { this.config = config }
        fun managerProvider(provider: AppUpdateManagerProvider) =
            apply { managerProvider = provider }

        fun build(): InAppUpdateManager = InAppUpdateManager(activity, config, managerProvider)
    }
}
