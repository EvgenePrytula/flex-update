package com.madappgang.flexupdate.core.types

sealed class UpdateError {
    data object ApiUnavailable : UpdateError()

    data class DownloadFailed(
        val code: Int,
    ) : UpdateError()

    data class InstallFailed(
        val code: Int = 0,
    ) : UpdateError()
}
