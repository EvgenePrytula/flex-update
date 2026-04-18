package com.madappgang.flexupdate.core.types

sealed class DownloadState {
    data object Idle : DownloadState()

    data class InProgress(
        val percent: Int,
    ) : DownloadState()

    data object Completed : DownloadState()

    data object Installing : DownloadState()

    data class Failed(
        val error: UpdateError,
    ) : DownloadState()
}
