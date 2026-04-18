package com.madappgang.flexupdate.core.types

sealed class UpdateOutcome {
    data object NotAvailable : UpdateOutcome()

    data object Accepted : UpdateOutcome()

    data object Declined : UpdateOutcome()

    data object ReadyToInstall : UpdateOutcome()

    data class Failed(
        val error: UpdateError,
    ) : UpdateOutcome()
}
