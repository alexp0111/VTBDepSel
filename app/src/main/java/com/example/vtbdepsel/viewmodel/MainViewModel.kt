package com.example.vtbdepsel.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtbdepsel.model.MainRepository
import com.example.vtbdepsel.model.api.data.ApiDepartment
import com.example.vtbdepsel.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: MainRepository
) : ViewModel() {

    private var depLoadJob: Job? = null
    private val _depList = MutableLiveData<UiState<List<ApiDepartment>>>()
    val depList: LiveData<UiState<List<ApiDepartment>>>
        get() = _depList

    fun updateDepartments() {
        _depList.value = UiState.Loading
        depLoadJob?.cancel()
        depLoadJob = viewModelScope.plus(Dispatchers.IO).launch {
            val result = repository.getDepartments()
            withContext(Dispatchers.Main) {
                _depList.value = result
            }
        }
    }

    fun ping() {
        viewModelScope.plus(Dispatchers.IO).launch {
            val pong = repository.ping()
            Log.d("VIEW_MODEL", pong)
        }
    }
}