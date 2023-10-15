package com.example.vtbdepsel.model.api.data


/**
 * API Model of ATM
 * */
data class ApiATMItem(
    val address: String,
    val allday: Boolean,
    val id: Int,
    val latitude: Double,
    val longitude: Double
)