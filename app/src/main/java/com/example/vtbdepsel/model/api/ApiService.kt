package com.example.vtbdepsel.model.api

import com.example.vtbdepsel.model.api.data.ApiATMItem
import com.example.vtbdepsel.model.api.data.ApiBranchItem
import com.example.vtbdepsel.model.api.data.ApiDepartment
import com.example.vtbdepsel.model.api.data.ApiPoint
import com.example.vtbdepsel.model.api.data.CapacityItem
import com.example.vtbdepsel.model.api.data.FunctionItem
import com.example.vtbdepsel.model.api.data.Ping
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.DayOfWeek

interface ApiService {

    @GET("api/branches/branches/nearby")
    suspend fun getDepartments(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): List<ApiBranchItem>?

    @GET("api/atms/nearby")
    suspend fun getAtms(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): List<ApiATMItem>?

    @GET("api/openhours/{branchId}/byDayAndHour")
    suspend fun getCurrentCapacityById(
        @Path("branchId") branchId: Int,
        @Query("dayOfWeek") dayOfWeek: DayOfWeek,
        @Query("hour") hours: Int,
    ): List<CapacityItem>?

    @GET("branches/{branchId}/functions")
    suspend fun getFunctionsById(
        @Path("branchId") branchId: Int,
    ): List<FunctionItem>?

    @GET("recom_office")
    suspend fun getOptimalPoint(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): ApiPoint?


    companion object {
        const val BASE_URL = "http://65.109.233.182:80/"
    }
}