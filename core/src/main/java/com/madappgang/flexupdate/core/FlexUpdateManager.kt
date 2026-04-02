package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.tasks.await

/**
 * FlexUpdateManager - handles Android app updates via Google Play Core API.
 */
class FlexUpdateManager private constructor(
    private val activity: ComponentActivity,
    private val strategy: UpdateStrategy,
    private val appUpdateManager: AppUpdateManager
) : LifecycleEventObserver {

    private val _updateState = MutableStateFlow<FlexUpdateState>(Idle)
    /** Current update process state */
    val updateState: StateFlow<FlexUpdateState> = _updateState.asStateFlow()

    private var currentUpdateInfo: AppUpdateInfo? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val updateLauncher = activity.registerForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            _updateState.value = FlexUpdateState.Canceled
            val isCritical = (strategy as? Manual)?.updatePriority == CRITICAL ||
                    currentUpdateInfo?.updatePriority() == CRITICAL.priority
            if (isCritical && result.resultCode == RESULT_CANCELED) {
                activity.finish()
            }
        }
    }

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
        activity.lifecycle.addObserver(this)
        appUpdateManager.registerListener(installListener)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> resumeCheck()
            Lifecycle.Event.ON_DESTROY -> cleanup()
            else -> {}
        }
    }

    /**
     * Checks for updates and starts the flow if an update is available.
     */
    fun checkForUpdate() {
        scope.launch {
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
                    appUpdateManager.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(type!!).build())
                } else {
                    _updateState.value = Idle
                }
            } catch (e: Exception) {
                _updateState.value = Error(e)
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

    private fun resumeCheck() {
        scope.launch {
            try {
                val info = appUpdateManager.appUpdateInfo.await()
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
                            appUpdateManager.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(IMMEDIATE).build())
                        }
                    }
                }
            } catch (_: Exception) { /* ignored */ }
        }
    }

    private fun cleanup() {
        appUpdateManager.unregisterListener(installListener)
        scope.cancel()
    }

    companion object {
        fun from(
            activity: ComponentActivity,
            strategy: UpdateStrategy = Auto,
            appUpdateManager: AppUpdateManager? = null
        ): FlexUpdateManager = FlexUpdateManager(activity, strategy, appUpdateManager ?: AppUpdateManagerFactory.create(activity))
    }
}
