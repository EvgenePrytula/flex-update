package com.madappgang.flexupdate.core.types

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()

    data class InProgress(
        val percent: Int,
    ) : UpdateDownloadState()

    data object Completed : UpdateDownloadState()

    data object Installing : UpdateDownloadState()

    data class Failed(
        val error: UpdateError,
    ) : UpdateDownloadState()
}
