package com.example.vtbdepsel.model.api.data

/**
 * API Model of item, that responsible for capacity with:
 * @param day
 * @param hours
 * */
data class CapacityItem(
    val branchId: Int,
    val capacity: Int,
    val day: String,
    val hours: String,
    val id: Int
)