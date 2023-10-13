package com.example.vtbdepsel.model.api

import com.example.vtbdepsel.model.api.data.ApiDepartment
import com.example.vtbdepsel.model.api.data.Ping
import retrofit2.http.GET

interface ApiService {

    @GET("deps")
    suspend fun getDepartments(): List<ApiDepartment>?

    @GET("ping")
    suspend fun ping(): Ping

    companion object {
        const val BASE_URL = "http://65.109.233.182:8080/"
    }
}