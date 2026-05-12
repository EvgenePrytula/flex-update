package com.madappgang.flexupdate.core.types

enum class UpdatePriority(
    internal val level: Int,
) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4),
    URGENT(5),
    ;

    companion object {
        fun fromLevel(level: Int): UpdatePriority = entries.firstOrNull { it.level == level } ?: NONE
    }
}
