package com.example.kt_fife.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.database.PcBuildEntity
import com.example.kt_fife.data.repository.PcBuildRepository
import com.example.kt_fife.domain.models.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ ДОБАВЛЯЕМ UiEvent
sealed class PcBuildDetailEvent {
    object LoadBuild : PcBuildDetailEvent()
    object Retry : PcBuildDetailEvent()
}

@HiltViewModel
class PcBuildDetailViewModel @Inject constructor(
    private val repository: PcBuildRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val buildId: Long = savedStateHandle["buildId"] ?: -1L

    private val _uiState = MutableStateFlow<UiState<PcBuildEntity>>(UiState.Loading)
    val uiState: StateFlow<UiState<PcBuildEntity>> = _uiState.asStateFlow()

    init {
        loadBuild()
    }

    fun onEvent(event: PcBuildDetailEvent) {
        when (event) {
            is PcBuildDetailEvent.LoadBuild -> loadBuild()
            is PcBuildDetailEvent.Retry -> loadBuild()
        }
    }

    fun loadBuild() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val build = repository.getPcBuildById(buildId)
                if (build != null) {
                    _uiState.value = UiState.Success(build)
                } else {
                    _uiState.value = UiState.Error("Build not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error loading build")
            }
        }
    }
}