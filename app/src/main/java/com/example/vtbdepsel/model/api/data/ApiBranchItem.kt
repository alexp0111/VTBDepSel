package com.example.vtbdepsel.model.api.data


/**
 * API Model of department
 * */
data class ApiBranchItem(
    val address: String?,
    val distance: String?,
    val hasRamp: Boolean?,
    val id: Int,
    val kep: String?,
    val latitude: Double?,
    val longitude: Double?,
    val metroStation: String?,
    val myBranch: String?,
    val officeType: String?,
    val rko: String?,
    val salePointFormat: String?,
    val salePointName: String?,
    val status: String?,
    val suoAvailability: Boolean?,
    var capacity: Int? = null,
    var foos: List<FunctionItem>? = null,
)