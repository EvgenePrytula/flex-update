package com.madappgang.flexupdate.core.types

sealed class DownloadState {
    object Idle : DownloadState()
    data class InProgress(val percent: Int) : DownloadState()
    object Completed : DownloadState()
    object Installing : DownloadState()
    data class Failed(val errorCode: Int) : DownloadState()
}