package com.madappgang.flexupdate.core.types

sealed class UpdateMode {
    data object Auto : UpdateMode()

    data class Manual(
        val minPriority: UpdatePriority = UpdatePriority.HIGH,
    ) : UpdateMode()
}
