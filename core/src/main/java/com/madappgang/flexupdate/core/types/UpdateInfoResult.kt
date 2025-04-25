package com.madappgang.flexupdate.core.types

import com.google.android.play.core.appupdate.AppUpdateInfo


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

data class UpdateInfoResult(
    val updateType: Int? = null,
    val appUpdateInfo: AppUpdateInfo
)