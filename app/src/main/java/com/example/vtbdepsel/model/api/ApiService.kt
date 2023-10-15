package com.example.vtbdepsel.model.api

import com.example.vtbdepsel.model.api.data.ApiATMItem
import com.example.vtbdepsel.model.api.data.ApiBranchItem
import com.example.vtbdepsel.model.api.data.ApiPoint
import com.example.vtbdepsel.model.api.data.CapacityItem
import com.example.vtbdepsel.model.api.data.FunctionItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.DayOfWeek


/**
 * Api Service that holds integration between mobile app & backend
 * */
interface ApiService {

    /**
     * @return list of near by departments
     * */
    @GET("api/branches/branches/nearby")
    suspend fun getDepartments(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): List<ApiBranchItem>?

    /**
     * @return list of near by ATM's
     * */
    @GET("api/atms/nearby")
    suspend fun getAtms(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): List<ApiATMItem>?

    /**
     * @return capacity list
     * */
    @GET("api/openhours/{branchId}/byDayAndHour")
    suspend fun getCurrentCapacityById(
        @Path("branchId") branchId: Int,
        @Query("dayOfWeek") dayOfWeek: DayOfWeek,
        @Query("hour") hours: Int,
    ): List<CapacityItem>?


    /**
     * @return list of functions by dep's id
     * */
    @GET("branches/{branchId}/functions")
    suspend fun getFunctionsById(
        @Path("branchId") branchId: Int,
    ): List<FunctionItem>?

    /**
     * @return optimal point for user
     * */
    @GET("recom_office")
    suspend fun getOptimalPoint(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): ApiPoint?


    companion object {
        const val BASE_URL = "http://65.109.233.182:80/"
    }
}