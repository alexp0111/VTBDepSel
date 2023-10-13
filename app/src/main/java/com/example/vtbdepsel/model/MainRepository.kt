package com.example.vtbdepsel.model

import android.util.Log
import com.example.vtbdepsel.model.api.ApiService
import com.example.vtbdepsel.model.api.data.ApiDepartment
import com.example.vtbdepsel.utils.UiState
import javax.inject.Inject


class MainRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getDepartments(): UiState<List<ApiDepartment>> {
        try {
            val deps = api.getDepartments()
            deps?.let {
                return UiState.Success(it)
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }

    suspend fun ping(): String {
        val v = api.ping()
        Log.d("REPO", v.response)
        return "-1"
    }
}