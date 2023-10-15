package com.example.vtbdepsel.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtbdepsel.model.MainRepository
import com.example.vtbdepsel.model.api.data.ApiATMItem
import com.example.vtbdepsel.model.api.data.ApiBranchItem
import com.example.vtbdepsel.model.api.data.ApiPoint
import com.example.vtbdepsel.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Main ViewModel, that hold two actual states of departments and ATM's
 *
 * Also responsible for optimal point request
 * */
@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository
) : ViewModel() {

    private var depLoadJob: Job? = null
    private var atmLoadJob: Job? = null
    private var searchPointJob: Job? = null


    private val _depList = MutableLiveData<UiState<List<ApiBranchItem>>>()
    val depList: LiveData<UiState<List<ApiBranchItem>>>
        get() = _depList

    private val _atmList = MutableLiveData<UiState<List<ApiATMItem>>>()
    val atmList: LiveData<UiState<List<ApiATMItem>>>
        get() = _atmList

    fun updateDepartments(latitude: Double, longitude: Double) {
        _depList.value = UiState.Loading
        depLoadJob?.cancel()
        depLoadJob = viewModelScope.plus(Dispatchers.IO).launch {
            val result = repository.getDepartments(latitude, longitude)
            if (result is UiState.Success) {
                result.data.forEach {
                    val cap = repository.getCurrentCapacityById(
                        it.id,
                        // LocalDateTime.now().dayOfWeek,
                        DayOfWeek.MONDAY,
                        // LocalDateTime.now().hour
                        16
                    )
                    val foos = repository.getFunctionsById(239)
                    if (cap is UiState.Success && foos is UiState.Success) {
                        it.capacity = cap.data.capacity
                        it.foos = foos.data
                    }
                    Log.d("VIEW_MODEL", it.address.toString())
                }
            }
            withContext(Dispatchers.Main) {
                _depList.value = result
            }
        }
    }

    fun updateAtms(latitude: Double, longitude: Double) {
        _atmList.value = UiState.Loading
        atmLoadJob?.cancel()
        atmLoadJob = viewModelScope.plus(Dispatchers.IO).launch {
            val result = repository.getAtms(latitude, longitude)
            if (result is UiState.Success) {
                result.data.forEach {
                    Log.d("VIEW_MODEL_2", it.address.toString())
                }
            }
            withContext(Dispatchers.Main) {
                _atmList.value = result
            }
        }
    }

    fun getPoint(latitude: Double, longitude: Double, result: (ApiPoint) -> Unit) {
        searchPointJob?.cancel()
        searchPointJob = viewModelScope.plus(Dispatchers.IO).launch {
            val curRes = repository.getOptimalPoint(latitude, longitude)
            withContext(Dispatchers.Main) {
                if (curRes is UiState.Success) {
                    result.invoke(curRes.data)
                }
            }
        }
    }
}