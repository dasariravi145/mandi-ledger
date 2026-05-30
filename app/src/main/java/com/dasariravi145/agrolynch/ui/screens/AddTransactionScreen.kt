package com.dasariravi145.agrolynch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.ui.components.AgroButton
import com.dasariravi145.agrolynch.ui.components.AgroTextField
import timber.log.Timber
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    transactionId: String? = null,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    Timber.d("AddTransactionScreen: Initializing for id: $transactionId")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    
    var selectedFarmer by remember { mutableStateOf<FarmerEntity?>(null) }
    var farmerSearchText by remember { mutableStateOf("") }
    
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetable") }
    var gradesInput by remember { mutableStateOf("A Grade, B Grade") }
    
    var grossWeight by remember { mutableStateOf("") }
    var emptyBoxWeight by remember { mutableStateOf("") }
    var boxCount by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var hasInitialized by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val netWeight = (grossWeight.toDoubleOrNull() ?: 0.0) - (emptyBoxWeight.toDoubleOrNull() ?: 0.0)
    val totalAmount = netWeight * (pricePerUnit.toDoubleOrNull() ?: 0.0)

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            onBack()
        }
    }

    LaunchedEffect(transactionId, state.transactions, farmers) {
        if (transactionId != null && !hasInitialized && state.transactions.isNotEmpty() && farmers.isNotEmpty()) {
            val transaction = state.transactions.find { it.id == transactionId }
            transaction?.let { t ->
                val f = farmers.find { it.id == t.farmerId }
                if (f != null) {
                    selectedFarmer = f
                    farmerSearchText = f.name
                } else {
                    farmerSearchText = t.farmerName
                }
                
                productName = t.productName
                grossWeight = t.grossWeight.toString()
                emptyBoxWeight = t.emptyBoxWeight.toString()
                boxCount = t.boxCount.toString()
                pricePerUnit = t.pricePerUnit.toString()
                notes = t.notes
                
                val prod = products.find { it.name == t.productName }
                if (prod != null) {
                    category = prod.category
                    gradesInput = prod.availableGrades.joinToString(", ")
                }
                hasInitialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "New Arrival / కొత్త రాక" else "Edit Arrival / సవరించండి") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transactionId != null) {
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
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("General Details", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            
            FarmerSearchableDropdown(
                value = farmerSearchText,
                onValueChange = { 
                    farmerSearchText = it 
                    if (selectedFarmer?.name != it) selectedFarmer = null
                },
                farmers = farmers,
                onFarmerSelected = {
                    selectedFarmer = it
                    farmerSearchText = it.name
                }
            )

            Box {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    label = { Text("Category / వర్గం") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryMenu = true },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { showCategoryMenu = true })
                
                DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                    DropdownMenuItem(text = { Text("Fruits") }, onClick = { category = "Fruit"; showCategoryMenu = false })
                    DropdownMenuItem(text = { Text("Vegetables") }, onClick = { category = "Vegetable"; showCategoryMenu = false })
                }
            }

            AgroTextField(
                value = productName,
                onValueChange = { name ->
                    productName = name
                    val existing = products.find { it.name.equals(name, ignoreCase = true) }
                    if (existing != null) {
                        category = existing.category
                        gradesInput = existing.availableGrades.joinToString(", ")
                    }
                },
                label = "Product Name / వస్తువు పేరు"
            )

            AgroTextField(
                value = gradesInput,
                onValueChange = { gradesInput = it },
                label = "Available Grades"
            )

            Divider()
            Text("Weight & Price", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgroTextField(
                    value = grossWeight,
                    onValueChange = { grossWeight = it },
                    label = "Gross Wt",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                AgroTextField(
                    value = emptyBoxWeight,
                    onValueChange = { emptyBoxWeight = it },
                    label = "Box Wt",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = String.format(Locale.US, "%.2f", netWeight),
                    onValueChange = {},
                    label = { Text("Net Weight") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    enabled = false
                )
                AgroTextField(
                    value = boxCount,
                    onValueChange = { boxCount = it },
                    label = "Boxes",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            AgroTextField(
                value = pricePerUnit,
                onValueChange = { pricePerUnit = it },
                label = "Price Per Unit ₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Amount", fontWeight = FontWeight.Medium)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            AgroTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes / గమనికలు"
            )

            Spacer(modifier = Modifier.height(24.dp))

            AgroButton(
                text = "Save Stock Entry / సేవ్ చేయండి",
                onClick = {
                    if (farmerSearchText.isNotBlank() && productName.isNotBlank() && netWeight > 0) {
                        val transaction = TransactionEntity(
                            id = transactionId ?: UUID.randomUUID().toString(),
                            farmerId = selectedFarmer?.id ?: "",
                            farmerName = farmerSearchText,
                            productName = productName,
                            grossWeight = grossWeight.toDoubleOrNull() ?: 0.0,
                            emptyBoxWeight = emptyBoxWeight.toDoubleOrNull() ?: 0.0,
                            netWeight = netWeight,
                            quantity = netWeight,
                            boxCount = boxCount.toIntOrNull() ?: 0,
                            pricePerUnit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                            totalAmount = totalAmount,
                            notes = notes,
                            date = System.currentTimeMillis()
                        )
                        
                        val grades = gradesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        if (transactionId == null) {
                            viewModel.addTransaction(transaction, category, grades)
                        } else {
                            viewModel.updateTransaction(transaction)
                        }
                    }
                },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Entry?") },
                text = { Text("Are you sure you want to delete this stock entry?") },
                confirmButton = {
                    TextButton(onClick = {
                        transactionId?.let { viewModel.deleteTransaction(it) }
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun FarmerSearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    farmers: List<FarmerEntity>,
    onFarmerSelected: (FarmerEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = farmers.filter { it.name.contains(value, ignoreCase = true) }

    Column {
        Text("Farmer / రైతు", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it); expanded = true },
                placeholder = { Text("Search or Enter Farmer...") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp)
            )
            if (expanded && filtered.isNotEmpty() && value.isNotEmpty()) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    filtered.take(5).forEach { farmer ->
                        DropdownMenuItem(
                            text = { Text("${farmer.name} (${farmer.village})") },
                            onClick = { onFarmerSelected(farmer); expanded = false }
                        )
                    }
                }
            }
        }
    }
}
