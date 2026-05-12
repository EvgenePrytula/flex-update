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
import com.google.android.play.core.install.InstallException
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

    private val appUpdateManager: AppUpdateManager = managerProvider.provide(activity.applicationContext)
    private val strategy = UpdateStrategy(config)

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _outcome = MutableSharedFlow<UpdateOutcome>(replay = 1)
    val outcome: SharedFlow<UpdateOutcome> = _outcome.asSharedFlow()

    private var listenerRegistered = false

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
        unregisterInstallStateListener()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activityRef.clear()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun startUpdate() {
        _downloadState.value = Idle
        _outcome.resetReplayCache()

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info -> handleUpdateInfo(info) }
            .addOnFailureListener { _outcome.tryEmit(UpdateOutcome.Failed(UpdateError.ApiUnavailable)) }
    }

    fun completeUpdate() {
        _downloadState.value = Installing
        appUpdateManager
            .completeUpdate()
            .addOnFailureListener { error ->
                val code = (error as? InstallException)?.errorCode ?: 0
                val installError = UpdateError.InstallFailed(code)
                _downloadState.value = UpdateDownloadState.Failed(installError)
                _outcome.tryEmit(UpdateOutcome.Failed(installError))
            }
    }

    private fun handleUpdateInfo(info: AppUpdateInfo) {
        val earlyOutcome = info.earlyOutcomeOrNull()
        if (earlyOutcome != null) {
            _outcome.tryEmit(earlyOutcome)
            return
        }
        val updateType = info.resolveAllowedUpdateType()
        if (updateType == null) {
            _outcome.tryEmit(UpdateOutcome.NotAvailable)
            return
        }
        launchFlow(info, updateType)
    }

    private fun AppUpdateInfo.earlyOutcomeOrNull(): UpdateOutcome? =
        when (updateAvailability()) {
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> UpdateOutcome.NotAvailable
            UpdateAvailability.UNKNOWN -> UpdateOutcome.Failed(UpdateError.ApiUnavailable)
            else -> null
        }

    private fun AppUpdateInfo.resolveAllowedUpdateType(): Int? {
        val stalenessDays = clientVersionStalenessDays() ?: 0
        return strategy
            .resolve(updatePriority(), stalenessDays)
            ?.takeIf { isUpdateTypeAllowed(it) }
    }

    private fun resumeIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                launchFlow(info, AppUpdateType.IMMEDIATE)
                return@addOnSuccessListener
            }
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> onDownloadCompleted()

                InstallStatus.PENDING -> registerInstallStateListener()

                InstallStatus.DOWNLOADING -> {
                    _downloadState.value = info.toInProgressState()
                    registerInstallStateListener()
                }

                InstallStatus.INSTALLING -> {
                    _downloadState.value = Installing
                    registerInstallStateListener()
                }

                else -> Unit
            }
        }
    }

    private fun launchFlow(
        info: AppUpdateInfo,
        updateType: Int,
    ) {
        currentActivity?.takeUnless { it.isFinishing || it.isDestroyed } ?: return
        if (updateType == AppUpdateType.FLEXIBLE) {
            registerInstallStateListener()
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
                unregisterInstallStateListener()
                onDownloadCompleted()
            }

            InstallStatus.INSTALLING -> {
                _downloadState.value = Installing
            }

            InstallStatus.FAILED -> {
                val error = UpdateError.DownloadFailed(state.installErrorCode())
                _downloadState.value = UpdateDownloadState.Failed(error)
                _outcome.tryEmit(UpdateOutcome.Failed(error))
                unregisterInstallStateListener()
            }

            InstallStatus.CANCELED -> {
                unregisterInstallStateListener()
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
            Activity.RESULT_OK -> _outcome.tryEmit(UpdateOutcome.Accepted)
            Activity.RESULT_CANCELED -> _outcome.tryEmit(UpdateOutcome.Declined)
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED ->
                _outcome.tryEmit(UpdateOutcome.Failed(UpdateError.InstallFailed()))
            else -> _outcome.tryEmit(UpdateOutcome.Failed(UpdateError.InstallFailed(resultCode)))
        }
    }

    private fun registerInstallStateListener() {
        if (listenerRegistered) return
        appUpdateManager.registerListener(installStateListener)
        listenerRegistered = true
    }

    private fun unregisterInstallStateListener() {
        if (!listenerRegistered) return
        appUpdateManager.unregisterListener(installStateListener)
        listenerRegistered = false
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

private fun InstallState.toInProgressState(): UpdateDownloadState.InProgress = progressOf(bytesDownloaded(), totalBytesToDownload())

private fun AppUpdateInfo.toInProgressState(): UpdateDownloadState.InProgress = progressOf(bytesDownloaded(), totalBytesToDownload())

private fun progressOf(
    downloaded: Long,
    total: Long,
): UpdateDownloadState.InProgress {
    val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
    return UpdateDownloadState.InProgress(percent)
}
