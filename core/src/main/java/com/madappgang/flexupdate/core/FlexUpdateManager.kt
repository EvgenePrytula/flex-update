package com.madappgang.flexupdate.core

import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.FlexUpdateState.Checking
import com.madappgang.flexupdate.core.FlexUpdateState.Downloaded
import com.madappgang.flexupdate.core.FlexUpdateState.Error
import com.madappgang.flexupdate.core.FlexUpdateState.Idle
import com.madappgang.flexupdate.core.FlexUpdateState.UpdateAvailable
import com.madappgang.flexupdate.core.types.UpdatePriority
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdateStrategy
import com.madappgang.flexupdate.core.types.UpdateStrategy.Auto
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual
import com.madappgang.flexupdate.core.types.getUpdateType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * FlexUpdateManager - handles Android app updates via Google Play Core API.
 */
class FlexUpdateManager(
    private val appUpdateManager: AppUpdateManager,
    private val strategy: UpdateStrategy = Auto
) {

    private val _updateState = MutableStateFlow<FlexUpdateState>(Idle)
    /** Current update process state */
    val updateState: StateFlow<FlexUpdateState> = _updateState.asStateFlow()

    private var currentUpdateInfo: AppUpdateInfo? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val downloaded = state.bytesDownloaded()
                val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
                _updateState.value = FlexUpdateState.Downloading(progress)
            }
            InstallStatus.DOWNLOADED -> _updateState.value = Downloaded
            InstallStatus.INSTALLING -> _updateState.value = FlexUpdateState.Installing
            InstallStatus.FAILED -> _updateState.value = Error(Exception("Install failed"))
            InstallStatus.CANCELED -> _updateState.value = FlexUpdateState.Canceled
        }
    }

    init {
        appUpdateManager.registerListener(installListener)
    }

    /**
     * Checks for updates and starts the flow if an update is available.
     * @param onUpdateAvailable Callback with info and type to start the update flow.
     */
    fun checkForUpdate(onUpdateAvailable: (AppUpdateInfo, Int) -> Unit) {
        scope.launch {
            mutex.withLock {
                if (_updateState.value is Checking) return@launch
                _updateState.value = Checking
                try {
                    val info = appUpdateManager.appUpdateInfo.await()
                    currentUpdateInfo = info
                    val priority = when (strategy) {
                        Auto -> UpdatePriority.fromPriority(info.updatePriority())
                        is Manual -> strategy.updatePriority
                    }
                    val type = priority.getUpdateType()
                    val isAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    val isAllowed = type != null && info.isUpdateTypeAllowed(type)

                    if (isAvailable && isAllowed) {
                        _updateState.value = UpdateAvailable(priority, type == IMMEDIATE)
                        onUpdateAvailable(info, type)
                    } else {
                        _updateState.value = Idle
                    }
                } catch (e: Exception) {
                    Log.e("FlexUpdate", "Error checking update", e)
                    _updateState.value = Error(e)
                }
            }
        }
    }

    /**
     * Completes flexible update by restarting the app.
     */
    fun completeUpdate() {
        if (_updateState.value is Downloaded) {
            appUpdateManager.completeUpdate()
        }
    }

    /**
     * Resumes the update flow if it was interrupted.
     * @param onImmediateUpdateResume Callback to resume immediate update flow.
     */
    fun resumeCheck(onImmediateUpdateResume: (AppUpdateInfo) -> Unit = {}) {
        scope.launch {
            try {
                val info = appUpdateManager.appUpdateInfo.await()
                currentUpdateInfo = info
                when (info.installStatus()) {
                    InstallStatus.DOWNLOADED -> _updateState.value = Downloaded
                    InstallStatus.DOWNLOADING -> {
                        val total = info.totalBytesToDownload()
                        val downloaded = info.bytesDownloaded()
                        val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        _updateState.value = FlexUpdateState.Downloading(progress)
                    }
                    InstallStatus.INSTALLING -> _updateState.value = FlexUpdateState.Installing
                    else -> {
                        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                            onImmediateUpdateResume(info)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FlexUpdate", "Error resuming update check", e)
            }
        }
    }

    internal fun updateStateToCanceled() {
        _updateState.value = FlexUpdateState.Canceled
    }

    internal fun isCurrentUpdateCritical(): Boolean {
        return (strategy as? Manual)?.updatePriority == CRITICAL ||
                currentUpdateInfo?.updatePriority() == CRITICAL.priority
    }

    /**
     * Cleans up resources. Should be called when the manager is no longer needed.
     */
    fun cleanup() {
        appUpdateManager.unregisterListener(installListener)
        scope.cancel()
    }

    companion object {
        @Deprecated(
            "Use direct constructor with AppUpdateManager instead or Provide separate DI. FlexUpdateDelegate will handle activity.",
            ReplaceWith("FlexUpdateManager(appUpdateManager, strategy)")
        )
        fun from(
            @Suppress("UNUSED_PARAMETER") activity: ComponentActivity,
            strategy: UpdateStrategy = Auto,
            appUpdateManager: AppUpdateManager? = null
        ): FlexUpdateManager = FlexUpdateManager(
            appUpdateManager ?: AppUpdateManagerFactory.create(activity.applicationContext),
            strategy
        )
    }
}
