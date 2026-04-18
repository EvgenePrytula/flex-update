package com.madappgang.flexupdate.core

sealed class UpdateOutcome {
    object NotAvailable : UpdateOutcome()
    object Accepted : UpdateOutcome()
    object Declined : UpdateOutcome()
    object ReadyToInstall : UpdateOutcome()
    data class Failed(val errorCode: Int) : UpdateOutcome()
}
