package com.madappgang.flexupdate.core

import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

sealed class UpdateStrategy {
    data class Manual(
        val updatePriority: UpdatePriority
    ) : UpdateStrategy() {
        override suspend fun resolve(appUpdateManager: AppUpdateManager): UpdateInfoResult? {
            return suspendCancellableCoroutine { continuation ->
                val updateType = updatePriority.getUpdateType()

                if (updateType == null) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                try {
                    val info = Tasks.await(appUpdateManager.appUpdateInfo)

                    val isUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE
                    val isUpdateTypeAllowed = info.isUpdateTypeAllowed(updateType)

                    if (isUpdateAvailable && isUpdateTypeAllowed) {
                        continuation.resume(
                            UpdateInfoResult(
                                updateType = updateType,
                                appUpdateInfo = info
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }

                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }
    }

    data object Auto : UpdateStrategy() {
        override suspend fun resolve(appUpdateManager: AppUpdateManager): UpdateInfoResult? {
            return suspendCancellableCoroutine { continuation ->
                try {
                    val info = Tasks.await(appUpdateManager.appUpdateInfo)

                    val priority = UpdatePriority.fromPriority(info.updatePriority())
                    val updateType = priority.getUpdateType()

                    if (updateType == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val isUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE
                    val isUpdateTypeAllowed = info.isUpdateTypeAllowed(updateType)

                    if (isUpdateAvailable && isUpdateTypeAllowed) {
                        continuation.resume(
                            UpdateInfoResult(
                                updateType = updateType,
                                appUpdateInfo = info
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }

                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }
    }

    abstract suspend fun resolve(appUpdateManager: AppUpdateManager): UpdateInfoResult?
}
