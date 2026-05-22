package com.example.kt_fife.ui.create

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.database.PcBuildEntity
import com.example.kt_fife.data.network.ProductResponse
import com.example.kt_fife.data.repository.ProductRepository
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

sealed class ComponentSelectionEvent {
    data class LoadBuildForEdit(val buildId: Long) : ComponentSelectionEvent()
    object LoadComponentTypes : ComponentSelectionEvent()
    data class LoadProductsByType(val componentType: String) : ComponentSelectionEvent()
    data class SelectProduct(val product: ProductResponse) : ComponentSelectionEvent()
    object NextStep : ComponentSelectionEvent()
    object PreviousStep : ComponentSelectionEvent()
    data class UpdateComponent(val typeName: String, val product: ProductResponse) : ComponentSelectionEvent()
    object Retry : ComponentSelectionEvent()
}

data class ComponentSelectionState(
    val componentTypes: List<ComponentTypeItem> = emptyList(),
    val currentStep: Int = 0,
    val selectedComponents: Map<String, ProductResponse> = emptyMap(),
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

data class ComponentTypeItem(
    val id: Long,
    val name: String,
    val step: Int,
    val isRequired: Boolean
)

@HiltViewModel
class ComponentSelectionViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val pcBuildRepository: PcBuildRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ComponentSelectionState())
    val state: StateFlow<ComponentSelectionState> = _state.asStateFlow()

    fun onEvent(event: ComponentSelectionEvent) {
        when (event) {
            is ComponentSelectionEvent.LoadBuildForEdit -> loadBuildForEdit(event.buildId)
            is ComponentSelectionEvent.LoadComponentTypes -> loadComponentTypes()
            is ComponentSelectionEvent.LoadProductsByType -> loadProductsByTypeImpl(event.componentType)
            is ComponentSelectionEvent.SelectProduct -> selectProductImpl(event.product)
            is ComponentSelectionEvent.NextStep -> nextStepImpl()
            is ComponentSelectionEvent.PreviousStep -> previousStepImpl()
            is ComponentSelectionEvent.UpdateComponent -> updateComponentImpl(event.typeName, event.product)
            is ComponentSelectionEvent.Retry -> retryImpl()
        }
    }

    fun getSelectedComponents(): List<PcBuildComponent> {
        return _state.value.selectedComponents.values.map { product ->
            PcBuildComponent(
                componentType = findComponentTypeName(product.categoryId),
                productId = product.id,
                productName = product.name,
                price = product.price,
                quantity = 1
            )
        }
    }

    private fun loadBuildForEdit(buildId: Long) {
        viewModelScope.launch {
            val build = pcBuildRepository.getPcBuildById(buildId)
            if (build != null) {
                val components = parseComponents(build.componentsJson)
                val selectedMap = mutableMapOf<String, ProductResponse>()
                for (component in components) {
                    val productResult = productRepository.getProductById(component.productId)
                    productResult.getOrNull()?.let { product ->
                        selectedMap[component.componentType] = product
                    }
                }
                _state.value = _state.value.copy(selectedComponents = selectedMap)
            }
            loadComponentTypes()
        }
    }

    private fun loadComponentTypes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = productRepository.getComponentTypesOrdered()
                if (result.isSuccess) {
                    val types = result.getOrNull()!!.mapIndexed { index, type ->
                        ComponentTypeItem(
                            id = type.id,
                            name = type.name,
                            step = index,
                            isRequired = type.isRequired
                        )
                    }
                    _state.value = _state.value.copy(
                        componentTypes = types,
                        isLoading = false,
                        currentStep = 0
                    )
                    loadProductsForCurrentStep()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load component types"
                    )
                }
            } catch (e: Exception) {
                Log.e("ComponentSelectionVM", "Error loading component types", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading component types"
                )
            }
        }
    }

    private fun loadProductsForCurrentStep() {
        val currentType = _state.value.componentTypes.getOrNull(_state.value.currentStep)
        if (currentType != null) {
            loadProductsByTypeImpl(currentType.name)
        }
    }

    private fun loadProductsByTypeImpl(componentType: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = productRepository.getProductsByComponentType(componentType, size = 50)
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        products = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load products"
                    )
                }
            } catch (e: Exception) {
                Log.e("ComponentSelectionVM", "Error loading products", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading products"
                )
            }
        }
    }

    private fun selectProductImpl(product: ProductResponse) {
        val currentType = _state.value.componentTypes.getOrNull(_state.value.currentStep) ?: return

        val newSelected = _state.value.selectedComponents.toMutableMap()
        newSelected[currentType.name] = product
        _state.value = _state.value.copy(selectedComponents = newSelected)

        nextStepImpl()
    }

    private fun nextStepImpl() {
        val nextIndex = _state.value.currentStep + 1
        if (nextIndex < _state.value.componentTypes.size) {
            _state.value = _state.value.copy(currentStep = nextIndex)
            loadProductsForCurrentStep()
        } else {
            _state.value = _state.value.copy(isComplete = true)
        }
    }

    private fun previousStepImpl() {
        val prevIndex = _state.value.currentStep - 1
        if (prevIndex >= 0) {
            _state.value = _state.value.copy(currentStep = prevIndex)
            loadProductsForCurrentStep()
        }
    }

    private fun updateComponentImpl(typeName: String, product: ProductResponse) {
        val newSelected = _state.value.selectedComponents.toMutableMap()
        newSelected[typeName] = product
        _state.value = _state.value.copy(selectedComponents = newSelected)
    }

    private fun retryImpl() {
        loadComponentTypes()
    }

    private fun findComponentTypeName(categoryId: Long): String {
        return _state.value.componentTypes.find { it.id == categoryId }?.name ?: "Неизвестно"
    }

    private fun parseComponents(json: String): List<PcBuildComponent> {
        return try {
            if (json.isEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<PcBuildComponent>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("ComponentSelectionVM", "Error parsing components", e)
            emptyList()
        }
    }
}