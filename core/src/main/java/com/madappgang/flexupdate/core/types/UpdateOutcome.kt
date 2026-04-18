package com.madappgang.flexupdate.core.types

sealed class UpdateOutcome {
    object NotAvailable : UpdateOutcome()
    object Accepted : UpdateOutcome()
    object Declined : UpdateOutcome()
    object ReadyToInstall : UpdateOutcome()
    data class Failed(val errorCode: Int) : UpdateOutcome()
}