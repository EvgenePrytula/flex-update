package com.madappgang.flexupdate.core.types

sealed class UpdateError {
    data object ApiUnavailable : UpdateError()

    data class DownloadFailed(
        val code: Int,
    ) : UpdateError()

    data object InstallFailed : UpdateError()
}
