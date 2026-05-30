package com.dasariravi145.agrolynch.ui.screens.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditProductScreen(
    viewModel: ProductViewModel,
    productId: String? = null,
    onBack: () -> Unit
) {
    Timber.d("AddEditProductScreen: Initializing for id: $productId")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fruit") }
    var selectedGrades by remember { mutableStateOf(setOf<String>()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    val categories = listOf("Fruit", "Vegetable")
    val allGrades = listOf("A Grade", "B Grade", "C Grade", "Premium", "Local")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            Timber.d("AddEditProductScreen: Save successful, navigating back")
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(productId, state.products) {
        if (productId != null && !hasInitialized && state.products.isNotEmpty()) {
            val product = state.products.find { it.id == productId }
            product?.let {
                Timber.d("AddEditProductScreen: Initializing with existing product: ${it.name}")
                name = it.name
                category = it.category
                selectedGrades = it.availableGrades.toSet()
                hasInitialized = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Add Product" else "Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (productId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null || productId != null) {
                    val model = imageUri ?: state.products.find { it.id == productId }?.imageUrl
                    AsyncImage(
                        model = model,
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Text("Add Photo")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text("Available Grades")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allGrades.forEach { grade ->
                    FilterChip(
                        selected = selectedGrades.contains(grade),
                        onClick = {
                            selectedGrades = if (selectedGrades.contains(grade)) {
                                selectedGrades - grade
                            } else {
                                selectedGrades + grade
                            }
                        },
                        label = { Text(grade) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    Timber.d("AddEditProductScreen: Save button clicked")
                    if (productId == null) {
                        viewModel.addProduct(name, category, selectedGrades.toList(), imageUri)
                    } else {
                        val existingProduct = state.products.find { it.id == productId }
                        existingProduct?.let {
                            viewModel.updateProduct(
                                it.copy(name = name, category = category, availableGrades = selectedGrades.toList()),
                                imageUri
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && selectedGrades.isNotEmpty() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save Product")
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Product?") },
                text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Timber.d("AddEditProductScreen: Deleting product: $productId")
                            productId?.let { viewModel.deleteProduct(it) }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
