package com.example.vtbdepsel.utils

enum class PlacemarkType {
    HIGH,
    MEDIUM,
    LOW,
    PREMIUM
}

data class PlacemarkATM(
    val id: Int,
    val type: PlacemarkType,
)

data class PlacemarkBranch(
    val id: Int,
    val type: PlacemarkType,
)