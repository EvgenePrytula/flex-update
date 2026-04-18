package com.madappgang.flexupdate.core

class UpdateConfig private constructor(
    val immediateMinPriority: Int,
    val flexibleMinPriority: Int,
    val stalenessDaysForEscalation: Int
) {
    class Builder {
        private var immediateMinPriority = 4
        private var flexibleMinPriority = 2
        private var stalenessDaysForEscalation = 7

        fun immediateMinPriority(value: Int) = apply { immediateMinPriority = value }
        fun flexibleMinPriority(value: Int) = apply { flexibleMinPriority = value }
        fun stalenessDaysForEscalation(value: Int) = apply { stalenessDaysForEscalation = value }

        fun build() = UpdateConfig(immediateMinPriority, flexibleMinPriority, stalenessDaysForEscalation)
    }
}
