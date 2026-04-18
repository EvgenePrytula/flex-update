package com.madappgang.flexupdate.core

import com.madappgang.flexupdate.core.types.UpdateMode
import com.madappgang.flexupdate.core.types.UpdateMode.Auto
import com.madappgang.flexupdate.core.types.UpdateMode.Manual
import com.madappgang.flexupdate.core.types.UpdatePriority

class UpdateConfig private constructor(
    val mode: UpdateMode,
    val stalenessDaysForEscalation: Int
) {
    class Builder {
        private var mode: UpdateMode = Auto
        private var stalenessDaysForEscalation = 7

        fun auto() = apply { mode = Auto }
        fun manual(minPriority: UpdatePriority) = apply { mode = Manual(minPriority) }
        fun stalenessDaysForEscalation(value: Int) = apply { stalenessDaysForEscalation = value }

        fun build() = UpdateConfig(mode, stalenessDaysForEscalation)
    }
}
