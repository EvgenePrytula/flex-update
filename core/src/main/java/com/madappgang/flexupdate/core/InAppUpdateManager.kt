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
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.types.DownloadState
import com.madappgang.flexupdate.core.types.DownloadState.Idle
import com.madappgang.flexupdate.core.types.DownloadState.Installing
import com.madappgang.flexupdate.core.types.UpdateOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class InAppUpdateManager private constructor(
    activity: AppCompatActivity,
    private val config: UpdateConfig,
    managerProvider: AppUpdateManagerProvider
) : DefaultLifecycleObserver {

    companion object {
        fun create(
            activity: AppCompatActivity,
            config: UpdateConfig = UpdateConfig.Builder().build(),
            managerProvider: AppUpdateManagerProvider = DefaultAppUpdateManagerProvider()
        ): InAppUpdateManager = InAppUpdateManager(activity, config, managerProvider)
    }

    private val activityRef = WeakReference(activity)
    private val activity get() = activityRef.get()

    private val appUpdateManager: AppUpdateManager = managerProvider.provide(activity)

    private val _downloadState = MutableStateFlow<DownloadState>(Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _outcome = MutableSharedFlow<UpdateOutcome>(extraBufferCapacity = 1)
    val outcome: SharedFlow<UpdateOutcome> = _outcome.asSharedFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleActivityResult(result.resultCode)
        }

    private val installStateListener = InstallStateUpdatedListener { state ->
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
                val updateType = UpdateStrategy(config)
                    .resolve(info.updatePriority(), info.clientVersionStalenessDays() ?: 0)
                    ?.takeIf { info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE }
                    ?: run { _outcome.tryEmit(UpdateOutcome.NotAvailable); return@addOnSuccessListener }
                launchFlow(info, updateType)
            }
            .addOnFailureListener { _outcome.tryEmit(UpdateOutcome.Failed(-1)) }
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
                    _outcome.tryEmit(UpdateOutcome.ReadyToInstall)
                }
            }
        }
    }

    private fun launchFlow(info: AppUpdateInfo, updateType: Int) {
        activity?.takeUnless { it.isFinishing || it.isDestroyed } ?: return
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateListener)
        }
        appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(updateType).build()
        )
    }

    private fun handleInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> _downloadState.value = state.toInProgressState()
            InstallStatus.DOWNLOADED -> {
                _downloadState.value = DownloadState.Completed
                _outcome.tryEmit(UpdateOutcome.ReadyToInstall)
            }

            InstallStatus.INSTALLING -> _downloadState.value = Installing
            InstallStatus.FAILED -> {
                val code = state.installErrorCode()
                _downloadState.value = DownloadState.Failed(code)
                _outcome.tryEmit(UpdateOutcome.Failed(code))
                appUpdateManager.unregisterListener(installStateListener)
            }

            InstallStatus.CANCELED -> appUpdateManager.unregisterListener(installStateListener)
            else -> Unit
        }
    }

    private fun handleActivityResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> _outcome.tryEmit(UpdateOutcome.Accepted)
            Activity.RESULT_CANCELED -> _outcome.tryEmit(UpdateOutcome.Declined)
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED ->
                _outcome.tryEmit(UpdateOutcome.Failed(ActivityResult.RESULT_IN_APP_UPDATE_FAILED))

            else -> Unit
        }
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

private fun InstallState.toInProgressState(): DownloadState.InProgress {
    val percent = totalBytesToDownload()
        .takeIf { it > 0 }
        ?.let { ((bytesDownloaded() * 100) / it).toInt() }
        ?: 0
    return DownloadState.InProgress(percent)
}
