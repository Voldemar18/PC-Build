package com.example.kt_fife.ui.create

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.database.PcBuildEntity
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.data.network.ApiService
import com.example.kt_fife.data.network.CreatePcBuildRequest
import com.example.kt_fife.data.network.PcBuildComponentResponse
import com.example.kt_fife.data.repository.PcBuildRepository
import com.example.kt_fife.domain.models.PcBuildComponent
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ ДОБАВЛЯЕМ UiEvent
sealed class CreatePcBuildEvent {
    data class BuildNameChanged(val name: String) : CreatePcBuildEvent()
    data class IsPublicChanged(val isPublic: Boolean) : CreatePcBuildEvent()
    data class ComponentsChanged(val components: List<PcBuildComponent>) : CreatePcBuildEvent()
    object SaveBuild : CreatePcBuildEvent()
    object ResetSuccess : CreatePcBuildEvent()
    object ErrorDismissed : CreatePcBuildEvent()
}

data class CreatePcBuildUiState(
    val buildId: Long? = null,
    val buildName: String = "",
    val isPublic: Boolean = true,
    val components: List<PcBuildComponent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreatePcBuildViewModel @Inject constructor(
    private val repository: PcBuildRepository,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePcBuildUiState())
    val uiState: StateFlow<CreatePcBuildUiState> = _uiState.asStateFlow()

    // ✅ Единая точка входа для всех событий
    fun onEvent(event: CreatePcBuildEvent) {
        when (event) {
            is CreatePcBuildEvent.BuildNameChanged -> updateBuildName(event.name)
            is CreatePcBuildEvent.IsPublicChanged -> updateIsPublic(event.isPublic)
            is CreatePcBuildEvent.ComponentsChanged -> updateComponents(event.components)
            is CreatePcBuildEvent.SaveBuild -> saveBuild()
            is CreatePcBuildEvent.ResetSuccess -> resetSuccess()
            is CreatePcBuildEvent.ErrorDismissed -> dismissError()
        }
    }

    private fun updateBuildName(name: String) {
        _uiState.value = _uiState.value.copy(buildName = name, error = null)
    }

    private fun updateIsPublic(isPublic: Boolean) {
        _uiState.value = _uiState.value.copy(isPublic = isPublic)
    }

    private fun updateComponents(components: List<PcBuildComponent>) {
        _uiState.value = _uiState.value.copy(components = components)
    }

    private fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun saveBuild() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val accessToken = tokenManager.getAccessToken()
                if (accessToken == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Необходимо авторизоваться"
                    )
                    return@launch
                }

                if (_uiState.value.buildName.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Введите название сборки"
                    )
                    return@launch
                }

                val componentsIds = _uiState.value.components.map { it.productId }
                val request = CreatePcBuildRequest(
                    name = _uiState.value.buildName,
                    isPublic = _uiState.value.isPublic,
                    components = componentsIds
                )

                Log.d("CreatePcBuildViewModel", "Sending create request: $request")
                val response = apiService.createPcBuild(request)

                if (response.isSuccessful && response.body() != null) {
                    val serverBuild = response.body()!!
                    Log.d("CreatePcBuildViewModel", "Build created on server with ID: ${serverBuild.id}")

                    if (componentsIds.isNotEmpty()) {
                        var allSuccess = true
                        for (productId in componentsIds) {
                            try {
                                val addResponse = apiService.addComponentToBuild(serverBuild.id, productId)
                                if (!addResponse.isSuccessful) {
                                    Log.e("CreatePcBuildViewModel", "Failed to add component $productId: ${addResponse.code()}")
                                    allSuccess = false
                                }
                                delay(100)
                            } catch (e: Exception) {
                                Log.e("CreatePcBuildViewModel", "Error adding component $productId", e)
                                allSuccess = false
                            }
                        }

                        delay(1000)

                        val updatedBuildResponse = apiService.getMyPcBuildById(serverBuild.id)
                        if (updatedBuildResponse.isSuccessful && updatedBuildResponse.body() != null) {
                            val updatedBuild = updatedBuildResponse.body()!!
                            val entity = PcBuildEntity(
                                id = updatedBuild.id,
                                name = updatedBuild.name,
                                isPublic = updatedBuild.isPublic,
                                viewsCount = updatedBuild.viewsCount,
                                createdAt = updatedBuild.createdAt,
                                totalPrice = updatedBuild.totalPrice,
                                userId = updatedBuild.userId,
                                userName = updatedBuild.userName,
                                componentsJson = convertComponentsResponseToJson(updatedBuild.components)
                            )
                            repository.insertBuild(entity)
                        } else {
                            val entity = PcBuildEntity(
                                id = serverBuild.id,
                                name = serverBuild.name,
                                isPublic = serverBuild.isPublic,
                                viewsCount = serverBuild.viewsCount,
                                createdAt = serverBuild.createdAt,
                                totalPrice = serverBuild.totalPrice,
                                userId = serverBuild.userId,
                                userName = serverBuild.userName,
                                componentsJson = convertComponentsToJson(_uiState.value.components)
                            )
                            repository.insertBuild(entity)
                        }
                    } else {
                        val entity = PcBuildEntity(
                            id = serverBuild.id,
                            name = serverBuild.name,
                            isPublic = serverBuild.isPublic,
                            viewsCount = serverBuild.viewsCount,
                            createdAt = serverBuild.createdAt,
                            totalPrice = serverBuild.totalPrice,
                            userId = serverBuild.userId,
                            userName = serverBuild.userName,
                            componentsJson = convertComponentsResponseToJson(serverBuild.components)
                        )
                        repository.insertBuild(entity)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        buildId = serverBuild.id
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CreatePcBuildViewModel", "Failed to create build. Code: ${response.code()}, Error: $errorBody")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось создать сборку на сервере (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("CreatePcBuildViewModel", "Error saving build", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка при создании сборки"
                )
            }
        }
    }

    private fun convertComponentsResponseToJson(components: List<PcBuildComponentResponse>): String {
        return try {
            Gson().toJson(components)
        } catch (e: Exception) {
            Log.e("CreatePcBuildViewModel", "Error converting components to JSON", e)
            "[]"
        }
    }

    private fun convertComponentsToJson(components: List<PcBuildComponent>): String {
        return try {
            Gson().toJson(components)
        } catch (e: Exception) {
            "[]"
        }
    }
}