package com.example.vtbdepsel.utils


/**
 * Classes, that are necessary to handle type division with capacity load
 * */
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