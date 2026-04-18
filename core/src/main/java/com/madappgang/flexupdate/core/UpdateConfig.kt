package com.madappgang.flexupdate.core

class UpdateConfig private constructor(
    val immediateMinPriority: UpdatePriority,
    val flexibleMinPriority: UpdatePriority,
    val stalenessDaysForEscalation: Int
) {
    class Builder {
        private var immediateMinPriority = UpdatePriority.CRITICAL
        private var flexibleMinPriority = UpdatePriority.MEDIUM
        private var stalenessDaysForEscalation = 7

        fun immediateMinPriority(value: UpdatePriority) = apply { immediateMinPriority = value }
        fun flexibleMinPriority(value: UpdatePriority) = apply { flexibleMinPriority = value }
        fun stalenessDaysForEscalation(value: Int) = apply { stalenessDaysForEscalation = value }

        fun build() = UpdateConfig(immediateMinPriority, flexibleMinPriority, stalenessDaysForEscalation)
    }
}
