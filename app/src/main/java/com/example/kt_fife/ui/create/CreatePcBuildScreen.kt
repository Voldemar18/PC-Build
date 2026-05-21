package com.example.kt_fife.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kt_fife.data.network.ProductResponse
import com.example.kt_fife.domain.models.PcBuildComponent

// ✅ ProductPickerDialog должен быть ОПРЕДЕЛЕН ДО CreatePcBuildScreen
@Composable
fun ProductPickerDialog(
    componentType: String,
    onDismiss: () -> Unit,
    onProductSelected: (ProductResponse) -> Unit
) {
    val dialogViewModel: ComponentSelectionViewModel = hiltViewModel()
    val state by dialogViewModel.state.collectAsState()

    LaunchedEffect(componentType) {
        dialogViewModel.onEvent(ComponentSelectionEvent.LoadProductsByType(componentType))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите $componentType") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(state.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                dialogViewModel.onEvent(ComponentSelectionEvent.LoadProductsByType(componentType))
                            }) {
                                Text("Повторить")
                            }
                        }
                    }
                    else -> {
                        LazyColumn {
                            items(state.products) { product ->
                                ListItem(
                                    headlineContent = { Text(product.name) },
                                    supportingContent = {
                                        Text("${product.price} р")
                                    },
                                    modifier = Modifier.clickable {
                                        onProductSelected(product)
                                        onDismiss()
                                    }
                                )
                                Divider()
                            }

                            if (state.products.isEmpty()) {
                                item {
                                    Text(
                                        text = "Нет доступных компонентов",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun CreatePcBuildScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CreatePcBuildViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var buildName by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var selectedComponents by remember { mutableStateOf<Map<String, ProductResponse>>(emptyMap()) }
    var showProductPicker by remember { mutableStateOf(false) }
    var selectedComponentType by remember { mutableStateOf<String?>(null) }

    // Следим за успешным сохранением
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.onEvent(CreatePcBuildEvent.ResetSuccess)
            onSave()
        }
    }

    val componentTypes = listOf(
        "Процессор", "Материнская плата", "Оперативная память", "Видеокарта",
        "Накопитель", "Блок питания", "Корпус", "Охлаждение"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }

                    Text(
                        text = "Создание сборки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = {
                            viewModel.onEvent(CreatePcBuildEvent.BuildNameChanged(buildName))
                            viewModel.onEvent(CreatePcBuildEvent.IsPublicChanged(isPublic))
                            viewModel.onEvent(
                                CreatePcBuildEvent.ComponentsChanged(
                                    selectedComponents.map { (type, product) ->
                                        PcBuildComponent(
                                            componentType = type,
                                            productId = product.id,
                                            productName = product.name,
                                            price = product.price,
                                            quantity = 1
                                        )
                                    }
                                )
                            )
                            viewModel.onEvent(CreatePcBuildEvent.SaveBuild)
                        },
                        enabled = buildName.isNotBlank() && selectedComponents.size == componentTypes.size && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = buildName,
                        onValueChange = { buildName = it },
                        label = { Text("Название сборки") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.error != null && buildName.isBlank()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Опубликовать сборку")
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it }
                        )
                    }
                }

                item {
                    Text(
                        text = "Компоненты",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                items(componentTypes) { componentType ->
                    val selectedProduct = selectedComponents[componentType]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedComponentType = componentType
                                showProductPicker = true
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedProduct != null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = componentType,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (selectedProduct != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = selectedProduct.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "${selectedProduct.price} р",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "Не выбран",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (selectedProduct != null) {
                                IconButton(
                                    onClick = {
                                        selectedComponents = selectedComponents - componentType
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    val selectedCount = selectedComponents.size
                    val totalCount = componentTypes.size
                    val isComplete = selectedCount == totalCount

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isComplete) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Выбрано компонентов:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$selectedCount из $totalCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                val errorMessage = uiState.error
                if (errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMessage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                IconButton(
                                    onClick = { viewModel.onEvent(CreatePcBuildEvent.ErrorDismissed) }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductPicker && selectedComponentType != null) {
        ProductPickerDialog(
            componentType = selectedComponentType!!,
            onDismiss = {
                showProductPicker = false
                selectedComponentType = null
            },
            onProductSelected = { product: ProductResponse ->  // ✅ Явно указан тип
                selectedComponents = selectedComponents + (selectedComponentType!! to product)
                showProductPicker = false
                selectedComponentType = null
            }
        )
    }
}