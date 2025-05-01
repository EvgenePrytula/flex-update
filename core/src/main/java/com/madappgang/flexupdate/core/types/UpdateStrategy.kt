package com.madappgang.flexupdate.core.types


/**
 * Created by Eugene Prytula on 25.04.2025.
 */

sealed class UpdateStrategy {
    data class Manual(
        val updatePriority: UpdatePriority
    ) : UpdateStrategy()

    data object Auto : UpdateStrategy()
}
