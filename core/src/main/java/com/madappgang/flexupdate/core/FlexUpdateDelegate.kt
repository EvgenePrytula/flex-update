package com.madappgang.flexupdate.core

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.madappgang.flexupdate.core.types.UpdatePriority.CRITICAL
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual

/**
 * FlexUpdateDelegate - handles Activity-related update logic and lifecycle.
 */
class FlexUpdateDelegate(
    private val activity: ComponentActivity,
    private val manager: FlexUpdateManager,
    private val appUpdateManager: AppUpdateManager
) : DefaultLifecycleObserver {

    private val updateLauncher = activity.registerForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            manager.updateStateToCanceled()
            val isCritical = manager.isCurrentUpdateCritical()
            if (isCritical && result.resultCode == RESULT_CANCELED) {
                activity.finish()
            }
        }
    }

    init {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            throw IllegalStateException("FlexUpdateDelegate must be initialized before Activity is STARTED")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.resumeCheck { info ->
            startUpdate(info, IMMEDIATE)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.cleanup()
    }

    fun startUpdate(info: AppUpdateInfo, type: Int) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(type).build()
            )
        } catch (e: Exception) {
            Log.e("FlexUpdate", "Error starting update flow", e)
        }
    }
}
