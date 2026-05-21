package com.example.kt_fife.ui.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.database.PcBuildEntity
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.data.repository.PcBuildRepository
import com.example.kt_fife.domain.models.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PcBuildListEvent {
    object LoadData : PcBuildListEvent()
    object Refresh : PcBuildListEvent()
    object Retry : PcBuildListEvent()
    data class OnPcBuildClick(val id: Long) : PcBuildListEvent()
}

@HiltViewModel
class PcBuildListViewModel @Inject constructor(
    private val repository: PcBuildRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<PcBuildEntity>>>(UiState.Empty)
    val uiState: StateFlow<UiState<List<PcBuildEntity>>> = _uiState.asStateFlow()

    private val _hasCache = MutableStateFlow(false)
    val hasCache: StateFlow<Boolean> = _hasCache.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun handleEvent(event: PcBuildListEvent) {
        when (event) {
            is PcBuildListEvent.LoadData -> loadData()
            is PcBuildListEvent.Refresh -> refresh()
            is PcBuildListEvent.Retry -> retry()
            else -> {}
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            if (tokenManager.getAccessToken() == null) {
                Log.w("PcBuildListViewModel", "No access token, showing empty state")
                _uiState.value = UiState.Empty
                return@launch
            }

            Log.d("PcBuildListViewModel", "Loading data...")
            _uiState.value = UiState.Loading

            val cacheSize = repository.getCacheSize()
            _hasCache.value = cacheSize > 0
            Log.d("PcBuildListViewModel", "Cache size: $cacheSize")

            if (cacheSize > 0) {
                repository.getAllPcBuilds()
                    .catch { e ->
                        Log.e("PcBuildListViewModel", "Error loading from DB", e)
                        _uiState.value = UiState.Error(e.message ?: "Database error")
                    }
                    .collect { builds ->
                        if (builds.isNotEmpty()) {
                            Log.d("PcBuildListViewModel", "Loaded ${builds.size} builds from cache")
                            _uiState.value = UiState.Success(builds)
                        } else {
                            Log.d("PcBuildListViewModel", "Cache is empty")
                            _uiState.value = UiState.Empty
                        }
                    }

                refreshFromApi()
            } else {
                loadFromApi()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (tokenManager.getAccessToken() == null) {
                Log.w("PcBuildListViewModel", "Cannot refresh: no access token")
                _uiState.value = UiState.Empty
                return@launch
            }

            Log.d("PcBuildListViewModel", "Manual refresh requested")
            _isRefreshing.value = true
            loadFromApi()
            _isRefreshing.value = false
        }
    }

    private fun retry() {
        viewModelScope.launch {
            if (tokenManager.getAccessToken() == null) {
                Log.w("PcBuildListViewModel", "Cannot retry: no access token")
                _uiState.value = UiState.Empty
                return@launch
            }

            Log.d("PcBuildListViewModel", "Retry loading")
            _uiState.value = UiState.Loading
            loadFromApi()
        }
    }

    private suspend fun loadFromApi() {
        if (tokenManager.getAccessToken() == null) {
            Log.w("PcBuildListViewModel", "No access token, cannot load from API")
            _uiState.value = UiState.Empty
            return
        }

        Log.d("PcBuildListViewModel", "Loading from API...")
        val result = repository.refreshCache()

        if (result.isSuccess) {
            Log.d("PcBuildListViewModel", "API refresh successful")
            repository.getAllPcBuilds()
                .catch { e ->
                    Log.e("PcBuildListViewModel", "Error loading from DB after refresh", e)
                    _uiState.value = UiState.Error(e.message ?: "Database error")
                }
                .collect { builds ->
                    if (builds.isNotEmpty()) {
                        Log.d("PcBuildListViewModel", "Loaded ${builds.size} builds from DB after API refresh")
                        _uiState.value = UiState.Success(builds)
                        _hasCache.value = true
                    } else {
                        Log.d("PcBuildListViewModel", "No builds found after API refresh")
                        _uiState.value = UiState.Empty
                    }
                }
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
            Log.e("PcBuildListViewModel", "API refresh failed: $errorMessage")

            val cacheSize = repository.getCacheSize()
            if (cacheSize > 0) {
                Log.d("PcBuildListViewModel", "Showing cached data due to API error")
                repository.getAllPcBuilds()
                    .catch { e ->
                        _uiState.value = UiState.Error(errorMessage)
                    }
                    .collect { builds ->
                        if (builds.isNotEmpty()) {
                            _uiState.value = UiState.Success(builds)
                        } else {
                            _uiState.value = UiState.Error(errorMessage)
                        }
                    }
            } else {
                _uiState.value = UiState.Error(errorMessage)
            }
        }
    }

    private fun refreshFromApi() {
        viewModelScope.launch {
            if (tokenManager.getAccessToken() == null) {
                Log.w("PcBuildListViewModel", "Cannot refresh from API: no access token")
                return@launch
            }

            Log.d("PcBuildListViewModel", "Background refresh from API")
            val result = repository.refreshCache()

            if (result.isSuccess) {
                Log.d("PcBuildListViewModel", "Background refresh successful")
                if (_uiState.value !is UiState.Error) {
                    repository.getAllPcBuilds()
                        .catch { e ->
                            Log.e("PcBuildListViewModel", "Error loading updated builds", e)
                        }
                        .collect { builds ->
                            if (builds.isNotEmpty()) {
                                Log.d("PcBuildListViewModel", "UI updated with ${builds.size} builds from background refresh")
                                _uiState.value = UiState.Success(builds)
                                _hasCache.value = true
                            }
                        }
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("PcBuildListViewModel", "Background refresh failed: $errorMessage")
            }
        }
    }
}