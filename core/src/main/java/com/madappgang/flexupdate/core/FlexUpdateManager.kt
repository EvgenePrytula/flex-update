package com.madappgang.flexupdate.core

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.types.UpdateDownloadState
import com.madappgang.flexupdate.core.types.UpdateDownloadState.Idle
import com.madappgang.flexupdate.core.types.UpdateDownloadState.Installing
import com.madappgang.flexupdate.core.types.UpdateError
import com.madappgang.flexupdate.core.types.UpdateOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class FlexUpdateManager private constructor(
    activity: ComponentActivity,
    private val config: UpdateConfig,
    managerProvider: FlexUpdateProvider,
) : DefaultLifecycleObserver {
    private val activityRef = WeakReference(activity)
    private val currentActivity get() = activityRef.get()

    private val appUpdateManager: AppUpdateManager = managerProvider.provide(activity)
    private val strategy = UpdateStrategy(config)

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _outcome = MutableSharedFlow<UpdateOutcome>(replay = 1)
    val outcome: SharedFlow<UpdateOutcome> = _outcome.asSharedFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleActivityResult(result.resultCode)
        }

    private val installStateListener =
        InstallStateUpdatedListener { state ->
            handleInstallState(state)
        }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) = resumeIfNeeded()

    override fun onStop(owner: LifecycleOwner) {
        appUpdateManager.unregisterListener(installStateListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activityRef.clear()
        owner.lifecycle.removeObserver(this)
    }

    fun startUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val priority = info.updatePriority()
                val stalenessDays = info.clientVersionStalenessDays() ?: 0
                val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val resolvedType = strategy.resolve(priority, stalenessDays)
                val updateType =
                    resolvedType
                        ?.takeIf { isUpdateAvailable && info.isUpdateTypeAllowed(it) }
                        ?: run {
                            _outcome.tryEmit(UpdateOutcome.NotAvailable)
                            return@addOnSuccessListener
                        }
                launchFlow(info, updateType)
            }.addOnFailureListener { _outcome.tryEmit(UpdateOutcome.Failed(UpdateError.ApiUnavailable)) }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        _downloadState.value = Installing
    }

    private fun resumeIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    launchFlow(info, AppUpdateType.IMMEDIATE)
                }

                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    onDownloadCompleted()
                }
            }
        }
    }

    private fun launchFlow(
        info: AppUpdateInfo,
        updateType: Int,
    ) {
        currentActivity?.takeUnless { it.isFinishing || it.isDestroyed } ?: return
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateListener)
        }
        appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(updateType).build(),
        )
    }

    private fun handleInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _downloadState.value = state.toInProgressState()
            }

            InstallStatus.DOWNLOADED -> {
                appUpdateManager.unregisterListener(installStateListener)
                onDownloadCompleted()
            }

            InstallStatus.INSTALLING -> {
                _downloadState.value = Installing
            }

            InstallStatus.FAILED -> {
                val error = UpdateError.DownloadFailed(state.installErrorCode())
                _downloadState.value = UpdateDownloadState.Failed(error)
                _outcome.tryEmit(UpdateOutcome.Failed(error))
                appUpdateManager.unregisterListener(installStateListener)
            }

            InstallStatus.CANCELED -> {
                appUpdateManager.unregisterListener(installStateListener)
            }

            else -> {
                Unit
            }
        }
    }

    private fun onDownloadCompleted() {
        _downloadState.value = UpdateDownloadState.Completed
        if (config.autoInstall) {
            completeUpdate()
        } else {
            _outcome.tryEmit(UpdateOutcome.ReadyToInstall)
        }
    }

    private fun handleActivityResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                _outcome.tryEmit(UpdateOutcome.Accepted)
            }

            Activity.RESULT_CANCELED -> {
                _outcome.tryEmit(UpdateOutcome.Declined)
            }

            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                _outcome.tryEmit(UpdateOutcome.Failed(UpdateError.InstallFailed))
            }
        }
    }

    class Builder(
        private val activity: ComponentActivity,
    ) {
        private var config = UpdateConfig()
        private var managerProvider: FlexUpdateProvider = DefaultFlexUpdateProvider()

        fun config(config: UpdateConfig) = apply { this.config = config }

        fun managerProvider(provider: FlexUpdateProvider) = apply { managerProvider = provider }

        fun build(): FlexUpdateManager = FlexUpdateManager(activity, config, managerProvider)
    }
}

private fun InstallState.toInProgressState(): UpdateDownloadState.InProgress {
    val percent =
        totalBytesToDownload()
            .takeIf { it > 0 }
            ?.let { ((bytesDownloaded() * 100) / it).toInt() }
            ?: 0
    return UpdateDownloadState.InProgress(percent)
}
