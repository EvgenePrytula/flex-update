package com.madappgang.flexupdate.core.types

sealed class UpdateOutcome {
    data object NotAvailable : UpdateOutcome()

    data object Accepted : UpdateOutcome()

    data class Declined(
        val mandatory: Boolean,
    ) : UpdateOutcome()

    data object ReadyToInstall : UpdateOutcome()

    data class Failed(
        val error: UpdateError,
    ) : UpdateOutcome()
}
