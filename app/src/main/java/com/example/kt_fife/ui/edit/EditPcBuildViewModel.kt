package com.example.kt_fife.ui.edit

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
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ ДОБАВЛЯЕМ UiEvent
sealed class EditPcBuildEvent {
    object LoadBuild : EditPcBuildEvent()
    data class BuildNameChanged(val name: String) : EditPcBuildEvent()
    data class IsPublicChanged(val isPublic: Boolean) : EditPcBuildEvent()
    data class AddComponent(val component: PcBuildComponent) : EditPcBuildEvent()
    data class RemoveComponent(val component: PcBuildComponent) : EditPcBuildEvent()
    data class ChangeComponent(val oldComponent: PcBuildComponent, val newComponent: PcBuildComponent) : EditPcBuildEvent()
    data class ChangeComponentById(
        val oldComponentId: Long,
        val newComponentId: Long,
        val newComponentName: String,
        val newComponentType: String,
        val newComponentPrice: Double
    ) : EditPcBuildEvent()
    object SaveBuild : EditPcBuildEvent()
    object ErrorDismissed : EditPcBuildEvent()
}

data class EditPcBuildUiState(
    val buildId: Long = -1L,
    val buildName: String = "",
    val isPublic: Boolean = true,
    val components: List<PcBuildComponent> = emptyList(),
    val totalPrice: Double = 0.0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUpdatingComponent: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class EditPcBuildViewModel @Inject constructor(
    private val apiService: ApiService,
    private val repository: PcBuildRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPcBuildUiState())
    val uiState: StateFlow<EditPcBuildUiState> = _uiState.asStateFlow()

    private var isComponentOperationInProgress = false
    private var currentBuildId: Long = -1L

    // ✅ Единая точка входа для всех событий
    fun onEvent(event: EditPcBuildEvent) {
        when (event) {
            is EditPcBuildEvent.LoadBuild -> loadBuild(currentBuildId)
            is EditPcBuildEvent.BuildNameChanged -> updateBuildName(event.name)
            is EditPcBuildEvent.IsPublicChanged -> updateIsPublic(event.isPublic)
            is EditPcBuildEvent.AddComponent -> addComponent(event.component)
            is EditPcBuildEvent.RemoveComponent -> removeComponent(event.component)
            is EditPcBuildEvent.ChangeComponent -> changeComponent(event.oldComponent, event.newComponent)
            is EditPcBuildEvent.ChangeComponentById -> changeComponentById(
                event.oldComponentId,
                event.newComponentId,
                event.newComponentName,
                event.newComponentType,
                event.newComponentPrice
            )
            is EditPcBuildEvent.SaveBuild -> saveBuild {
                _uiState.value = _uiState.value.copy(saveSuccess = true)
            }
            is EditPcBuildEvent.ErrorDismissed -> dismissError()
        }
    }

    // Загружаем сборку (вызывается из UI)
    fun loadBuild(buildId: Long) {
        currentBuildId = buildId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (tokenManager.getAccessToken() == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Необходимо авторизоваться"
                )
                return@launch
            }

            try {
                Log.d("EditPcBuildViewModel", "Loading build from server with ID: $buildId")
                val response = apiService.getMyPcBuildById(buildId)

                if (response.isSuccessful && response.body() != null) {
                    val build = response.body()!!
                    Log.d("EditPcBuildViewModel", "Build loaded successfully from server")

                    val components = build.components.map { component ->
                        PcBuildComponent(
                            componentType = component.componentType,
                            productId = component.productId,
                            productName = component.productName,
                            price = component.price,
                            quantity = component.quantity
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        buildId = build.id,
                        buildName = build.name,
                        isPublic = build.isPublic,
                        components = components,
                        totalPrice = build.totalPrice ?: calculateTotalPrice(components),
                        isLoading = false,
                        error = null
                    )
                } else {
                    // Пытаемся загрузить из локальной БД
                    val localBuild = repository.getPcBuildById(buildId)
                    if (localBuild != null) {
                        val components = parseComponents(localBuild.componentsJson)
                        _uiState.value = _uiState.value.copy(
                            buildId = localBuild.id,
                            buildName = localBuild.name,
                            isPublic = localBuild.isPublic,
                            components = components,
                            totalPrice = localBuild.totalPrice ?: calculateTotalPrice(components),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Сборка не найдена"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditPcBuildViewModel", "Error loading build", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки сборки"
                )
            }
        }
    }

    private fun updateBuildName(name: String) {
        _uiState.value = _uiState.value.copy(buildName = name, error = null)
    }

    private fun updateIsPublic(isPublic: Boolean) {
        _uiState.value = _uiState.value.copy(isPublic = isPublic)
    }

    private fun changeComponent(oldComponent: PcBuildComponent, newComponent: PcBuildComponent) {
        if (isComponentOperationInProgress) {
            _uiState.value = _uiState.value.copy(
                error = "Подождите, предыдущая операция еще выполняется..."
            )
            return
        }

        viewModelScope.launch {
            isComponentOperationInProgress = true
            _uiState.value = _uiState.value.copy(isUpdatingComponent = true, error = null)

            try {
                val buildId = _uiState.value.buildId
                if (buildId == -1L) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingComponent = false,
                        error = "Невалидный ID сборки"
                    )
                    return@launch
                }

                val removeResponse = apiService.removeComponentFromBuild(buildId, oldComponent.productId)
                if (!removeResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingComponent = false,
                        error = "Не удалось удалить компонент (${removeResponse.code()})"
                    )
                    return@launch
                }

                val addResponse = apiService.addComponentToBuild(buildId, newComponent.productId)
                if (!addResponse.isSuccessful) {
                    // Пытаемся восстановить старый компонент
                    apiService.addComponentToBuild(buildId, oldComponent.productId)
                    _uiState.value = _uiState.value.copy(
                        isUpdatingComponent = false,
                        error = "Не удалось изменить компонент"
                    )
                    return@launch
                }

                val updatedComponents = _uiState.value.components
                    .filter { it.productId != oldComponent.productId } + newComponent

                _uiState.value = _uiState.value.copy(
                    components = updatedComponents,
                    totalPrice = calculateTotalPrice(updatedComponents),
                    isUpdatingComponent = false,
                    error = null
                )

                loadBuild(buildId)

            } catch (e: Exception) {
                Log.e("EditPcBuildViewModel", "Error changing component", e)
                _uiState.value = _uiState.value.copy(
                    isUpdatingComponent = false,
                    error = "Ошибка при изменении компонента: ${e.message}"
                )
            } finally {
                isComponentOperationInProgress = false
            }
        }
    }

    private fun changeComponentById(
        oldComponentId: Long,
        newComponentId: Long,
        newComponentName: String,
        newComponentType: String,
        newComponentPrice: Double
    ) {
        val oldComponent = _uiState.value.components.find { it.productId == oldComponentId }
        if (oldComponent == null) {
            _uiState.value = _uiState.value.copy(error = "Старый компонент не найден")
            return
        }

        val newComponent = PcBuildComponent(
            componentType = newComponentType,
            productId = newComponentId,
            productName = newComponentName,
            price = newComponentPrice,
            quantity = 1
        )

        changeComponent(oldComponent, newComponent)
    }

    private fun addComponent(component: PcBuildComponent) {
        if (isComponentOperationInProgress) {
            _uiState.value = _uiState.value.copy(
                error = "Подождите, предыдущая операция еще выполняется..."
            )
            return
        }

        val newComponents = _uiState.value.components + component
        _uiState.value = _uiState.value.copy(
            components = newComponents,
            totalPrice = calculateTotalPrice(newComponents)
        )
        addComponentToServer(component.productId, component)
    }

    private fun addComponentToServer(productId: Long, component: PcBuildComponent) {
        viewModelScope.launch {
            if (isComponentOperationInProgress) return@launch

            isComponentOperationInProgress = true
            _uiState.value = _uiState.value.copy(isUpdatingComponent = true, error = null)

            try {
                val buildId = _uiState.value.buildId
                if (buildId == -1L) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingComponent = false,
                        error = "Невалидный ID сборки"
                    )
                    return@launch
                }

                val response = apiService.addComponentToBuild(buildId, productId)
                if (!response.isSuccessful) {
                    val componentsWithoutNew = _uiState.value.components.filter { it.productId != productId }
                    _uiState.value = _uiState.value.copy(
                        components = componentsWithoutNew,
                        totalPrice = calculateTotalPrice(componentsWithoutNew),
                        isUpdatingComponent = false,
                        error = "Не удалось добавить компонент на сервер (${response.code()})"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isUpdatingComponent = false, error = null)
                    loadBuild(buildId)
                }
            } catch (e: Exception) {
                Log.e("EditPcBuildViewModel", "Error adding component", e)
                val componentsWithoutNew = _uiState.value.components.filter { it.productId != productId }
                _uiState.value = _uiState.value.copy(
                    components = componentsWithoutNew,
                    totalPrice = calculateTotalPrice(componentsWithoutNew),
                    isUpdatingComponent = false,
                    error = "Ошибка сети при добавлении компонента: ${e.message}"
                )
            } finally {
                isComponentOperationInProgress = false
            }
        }
    }

    private fun removeComponent(component: PcBuildComponent) {
        if (isComponentOperationInProgress) {
            _uiState.value = _uiState.value.copy(
                error = "Подождите, предыдущая операция еще выполняется..."
            )
            return
        }

        val newComponents = _uiState.value.components - component
        _uiState.value = _uiState.value.copy(
            components = newComponents,
            totalPrice = calculateTotalPrice(newComponents)
        )
        removeComponentFromServer(component.productId, component)
    }

    private fun removeComponentFromServer(productId: Long, component: PcBuildComponent) {
        viewModelScope.launch {
            if (isComponentOperationInProgress) return@launch

            isComponentOperationInProgress = true
            _uiState.value = _uiState.value.copy(isUpdatingComponent = true, error = null)

            try {
                val buildId = _uiState.value.buildId
                if (buildId == -1L) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingComponent = false,
                        error = "Невалидный ID сборки"
                    )
                    return@launch
                }

                val response = apiService.removeComponentFromBuild(buildId, productId)
                if (!response.isSuccessful) {
                    val restoredComponents = _uiState.value.components + component
                    _uiState.value = _uiState.value.copy(
                        components = restoredComponents,
                        totalPrice = calculateTotalPrice(restoredComponents),
                        isUpdatingComponent = false,
                        error = "Не удалось удалить компонент на сервере (${response.code()})"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isUpdatingComponent = false, error = null)
                    loadBuild(buildId)
                }
            } catch (e: Exception) {
                Log.e("EditPcBuildViewModel", "Error removing component", e)
                val restoredComponents = _uiState.value.components + component
                _uiState.value = _uiState.value.copy(
                    components = restoredComponents,
                    totalPrice = calculateTotalPrice(restoredComponents),
                    isUpdatingComponent = false,
                    error = "Ошибка сети при удалении компонента: ${e.message}"
                )
            } finally {
                isComponentOperationInProgress = false
            }
        }
    }

    private fun saveBuild(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                if (tokenManager.getAccessToken() == null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Необходимо авторизоваться для сохранения"
                    )
                    return@launch
                }

                if (_uiState.value.buildName.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Введите название сборки"
                    )
                    return@launch
                }

                val request = CreatePcBuildRequest(
                    name = _uiState.value.buildName,
                    isPublic = _uiState.value.isPublic,
                    components = _uiState.value.components.map { it.productId }
                )

                val response = if (_uiState.value.buildId == -1L) {
                    apiService.createPcBuild(request)
                } else {
                    apiService.updatePcBuild(_uiState.value.buildId, request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val build = response.body()!!
                    Log.d("EditPcBuildViewModel", "Build saved successfully with ID: ${build.id}")

                    val entity = PcBuildEntity(
                        id = build.id,
                        name = build.name,
                        isPublic = build.isPublic,
                        viewsCount = build.viewsCount,
                        createdAt = build.createdAt,
                        totalPrice = build.totalPrice,
                        userId = build.userId,
                        userName = build.userName,
                        componentsJson = convertComponentsToJson(build.components)
                    )
                    repository.insertBuild(entity)

                    _uiState.value = _uiState.value.copy(isSaving = false, error = null)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = when (response.code()) {
                            401 -> "Ошибка авторизации. Пожалуйста, войдите заново."
                            404 -> "Сборка не найдена на сервере"
                            400 -> "Некорректные данные сборки"
                            500 -> "Ошибка сервера. Попробуйте позже."
                            else -> "Не удалось сохранить сборку: ${response.code()}"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("EditPcBuildViewModel", "Error saving build", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Ошибка сохранения сборки"
                )
            }
        }
    }

    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun parseComponents(json: String): List<PcBuildComponent> {
        return try {
            if (json.isEmpty() || json == "[]") {
                emptyList()
            } else {
                val type = object : TypeToken<List<PcBuildComponent>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("EditPcBuildViewModel", "Error parsing components JSON", e)
            emptyList()
        }
    }

    private fun convertComponentsToJson(components: List<PcBuildComponentResponse>): String {
        return try {
            Gson().toJson(components)
        } catch (e: Exception) {
            Log.e("EditPcBuildViewModel", "Error converting components to JSON", e)
            "[]"
        }
    }

    private fun calculateTotalPrice(components: List<PcBuildComponent>): Double {
        return components.sumOf { (it.price ?: 0.0) * it.quantity }
    }
}