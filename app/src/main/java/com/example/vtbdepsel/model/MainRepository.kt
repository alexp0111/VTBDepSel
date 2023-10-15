package com.example.vtbdepsel.model

import com.example.vtbdepsel.model.api.ApiService
import com.example.vtbdepsel.model.api.data.ApiATMItem
import com.example.vtbdepsel.model.api.data.ApiBranchItem
import com.example.vtbdepsel.model.api.data.ApiPoint
import com.example.vtbdepsel.model.api.data.CapacityItem
import com.example.vtbdepsel.model.api.data.FunctionItem
import com.example.vtbdepsel.utils.UiState
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Main repository, that works with API
 *
 * implements Api methods with suspension logic
 *  & overlaping with UiState
 * */
class MainRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getDepartments(latitude: Double, longitude: Double): UiState<List<ApiBranchItem>> {
        try {
            val deps = api.getDepartments(latitude, longitude)
            deps?.let {
                return UiState.Success(it)
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }

    suspend fun getAtms(latitude: Double, longitude: Double): UiState<List<ApiATMItem>> {
        try {
            val atms = api.getAtms(latitude, longitude)
            atms?.let {
                return UiState.Success(it)
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }

    suspend fun getCurrentCapacityById(brachId: Int, dayOfWeek: DayOfWeek, hours: Int): UiState<CapacityItem> {
        try {
            val cap = api.getCurrentCapacityById(brachId, dayOfWeek, hours)
            cap?.let {
                return UiState.Success(it.first())
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }

    suspend fun getFunctionsById(brachId: Int): UiState<List<FunctionItem>> {
        try {
            val foos = api.getFunctionsById(brachId)
            foos?.let {
                return UiState.Success(it)
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }

    suspend fun getOptimalPoint(latitude: Double, longitude: Double): UiState<ApiPoint> {
        try {
            val point = api.getOptimalPoint(latitude, longitude)
            point?.let {
                return UiState.Success(it)
            }
        } catch (e: Exception) {
            return UiState.Failure("Something went wrong")
        }
        return UiState.Failure("Something went wrong")
    }
}