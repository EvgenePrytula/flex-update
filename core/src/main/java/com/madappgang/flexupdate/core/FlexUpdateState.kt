package com.madappgang.flexupdate.core

import com.madappgang.flexupdate.core.types.UpdatePriority

sealed class FlexUpdateState {
    /** Waiting for actions */
    data object Idle : FlexUpdateState()

    /** Checking for updates */
    data object Checking : FlexUpdateState()

    /** Update found, contains priority and update type (Immediate/Flexible) */
    data class UpdateAvailable(
        val priority: UpdatePriority,
        val isImmediate: Boolean
    ) : FlexUpdateState()

    /** Only for Flexible: providing download progress percentage (0-100%) */
    data class Downloading(val progress: Int) : FlexUpdateState()

    /** Only for Flexible: files downloaded, update ready for installation */
    data object Downloaded : FlexUpdateState()

    /** Installation in progress */
    data object Installing : FlexUpdateState()

    /** User refused the update */
    data object Canceled : FlexUpdateState()

    /** Error occurred, contains exception object */
    data class Error(val exception: Throwable) : FlexUpdateState()
}
