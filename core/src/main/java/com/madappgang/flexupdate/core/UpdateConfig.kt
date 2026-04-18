package com.madappgang.flexupdate.core

import com.madappgang.flexupdate.core.types.UpdateMode

data class UpdateConfig(
    val mode: UpdateMode = UpdateMode.Auto,
    val stalenessDaysForEscalation: Int = 7,
    val autoInstall: Boolean = false,
)
