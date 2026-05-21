package com.example.kt_fife.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kt_fife.data.network.ProductResponse
import com.example.kt_fife.domain.models.PcBuildComponent
import android.util.Log

val componentTypesList = listOf(
    "Процессор",
    "Материнская плата",
    "Оперативная память",
    "Видеокарта",
    "Накопитель",
    "Блок питания",
    "Корпус",
    "Охлаждение"
)

val componentTypeMapping = mapOf(
    "Процессор" to "Процессоры",
    "Материнская плата" to "Материнские платы",
    "Оперативная память" to "Оперативная память",
    "Видеокарта" to "Видеокарты",
    "Накопитель" to "Накопители",
    "Блок питания" to "Блоки питания",
    "Корпус" to "Корпуса",
    "Охлаждение" to "Охлаждение"
)

@Composable
fun EditPcBuildScreen(
    buildId: Long,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EditPcBuildViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProductPicker by remember { mutableStateOf(false) }
    var selectedComponentType by remember { mutableStateOf<String?>(null) }

    // Загружаем сборку через UiEvent
    LaunchedEffect(Unit) {
        Log.d("EditPcBuildScreen", "Loading build with ID: $buildId")
        viewModel.loadBuild(buildId)
    }

    // Следим за успешным сохранением
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSave()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                }

                Text(
                    text = "Edit Build",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = {
                        Log.d("EditPcBuildScreen", "Saving build with ${uiState.components.size} components")
                        viewModel.onEvent(EditPcBuildEvent.SaveBuild)
                    },
                    enabled = uiState.buildName.isNotBlank() && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading build...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadBuild(buildId) }) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCancel) {
                        Text("Go Back")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = uiState.buildName,
                            onValueChange = {
                                viewModel.onEvent(EditPcBuildEvent.BuildNameChanged(it))
                            },
                            label = { Text("Build Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Make build public?")
                            Switch(
                                checked = uiState.isPublic,
                                onCheckedChange = {
                                    viewModel.onEvent(EditPcBuildEvent.IsPublicChanged(it))
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Components (${uiState.components.size} selected)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    itemsIndexed(componentTypesList) { _, componentType ->
                        val serverTypeName = componentTypeMapping[componentType] ?: componentType
                        val existingComponent = uiState.components.find {
                            it.componentType == serverTypeName || it.componentType == componentType
                        }

                        ComponentEditCard(
                            componentType = componentType,
                            component = existingComponent,
                            onEdit = {
                                selectedComponentType = componentType
                                showProductPicker = true
                            },
                            onDelete = {
                                existingComponent?.let {
                                    Log.d("EditPcBuildScreen", "Deleting component: ${it.componentType} - ${it.productName}")
                                    viewModel.onEvent(EditPcBuildEvent.RemoveComponent(it))
                                }
                            }
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Price:",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.totalPrice}р",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductPicker && selectedComponentType != null) {
        val serverTypeName = componentTypeMapping[selectedComponentType!!] ?: selectedComponentType!!
        val currentComponent = uiState.components.find {
            it.componentType == serverTypeName || it.componentType == selectedComponentType
        }

        EditProductPickerDialog(
            componentType = selectedComponentType!!,
            currentComponent = currentComponent,
            onDismiss = {
                showProductPicker = false
                selectedComponentType = null
            },
            onProductSelected = { product ->
                val component = PcBuildComponent(
                    componentType = serverTypeName,
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    quantity = 1
                )

                Log.d("EditPcBuildScreen", "Adding/updating component: ${component.componentType} - ${component.productName}")

                if (currentComponent != null) {
                    viewModel.onEvent(EditPcBuildEvent.RemoveComponent(currentComponent))
                }

                viewModel.onEvent(EditPcBuildEvent.AddComponent(component))
                showProductPicker = false
                selectedComponentType = null
            }
        )
    }
}

// ✅ ComponentEditCard - Компонент для отображения карточки выбора компонента
@Composable
fun ComponentEditCard(
    componentType: String,
    component: PcBuildComponent?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (component != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = componentType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (component != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (component != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = component.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Quantity: ${component.quantity}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        component.price?.let { price ->
                            Text(
                                text = "$price р",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Не выбран",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                if (component != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}

// ✅ EditProductPickerDialog - версия диалога с currentComponent для экрана редактирования
@Composable
fun EditProductPickerDialog(
    componentType: String,
    currentComponent: PcBuildComponent?,
    onDismiss: () -> Unit,
    onProductSelected: (ProductResponse) -> Unit
) {
    val dialogViewModel: com.example.kt_fife.ui.create.ComponentSelectionViewModel = hiltViewModel()
    val state by dialogViewModel.state.collectAsState()

    val serverTypeName = componentTypeMapping[componentType] ?: componentType

    LaunchedEffect(componentType) {
        dialogViewModel.onEvent(com.example.kt_fife.ui.create.ComponentSelectionEvent.LoadProductsByType(serverTypeName))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Выберите $componentType")
                if (currentComponent != null) {
                    Text(
                        text = "Текущий: ${currentComponent.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
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
                                dialogViewModel.onEvent(com.example.kt_fife.ui.create.ComponentSelectionEvent.LoadProductsByType(serverTypeName))
                            }) {
                                Text("Повторить")
                            }
                        }
                    }
                    else -> {
                        LazyColumn {
                            items(state.products) { product ->
                                val isSelected = currentComponent?.productId == product.id
                                ListItem(
                                    headlineContent = { Text(product.name) },
                                    supportingContent = {
                                        Text("${product.price} р")
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Current",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        onProductSelected(product)
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
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