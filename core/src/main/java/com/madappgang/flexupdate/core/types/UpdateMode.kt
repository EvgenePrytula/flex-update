package com.madappgang.flexupdate.core.types

sealed class UpdateMode {
    object Auto : UpdateMode()
    data class Manual(val minPriority: UpdatePriority) : UpdateMode()
}