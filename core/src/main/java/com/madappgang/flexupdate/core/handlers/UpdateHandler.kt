package com.madappgang.flexupdate.core.handlers

import com.google.android.play.core.appupdate.AppUpdateInfo


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

interface UpdateHandler {
    fun startUpdateFlow(info: AppUpdateInfo)
}