package com.dasariravi145.agrolynch.ui.screens.marketrate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.data.local.entity.MarketRateEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketRateScreen(
    viewModel: MarketRateViewModel,
    onBackClick: () -> Unit
) {
    val rates by viewModel.currentRates.collectAsState()
    val products by viewModel.products.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRateToEdit by remember { mutableStateOf<MarketRateEntity?>(null) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_rates)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_rate))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.date_label, dateFormat.format(Date(selectedDate))),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading && rates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (rates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_rates_today))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rates) { rate ->
                        MarketRateItem(
                            rate = rate,
                            onClick = { selectedRateToEdit = rate },
                            onDelete = { viewModel.deleteRate(rate.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRateDialog(
                products = products,
                onDismiss = { showAddDialog = false },
                onSave = { prodId, name, grade, min, max ->
                    viewModel.saveRate(prodId, name, grade, min, max)
                    showAddDialog = false
                }
            )
        }

        if (selectedRateToEdit != null) {
            EditRateDialog(
                rate = selectedRateToEdit!!,
                onDismiss = { selectedRateToEdit = null },
                onSave = { updatedRate ->
                    viewModel.updateRate(updatedRate)
                    selectedRateToEdit = null
                }
            )
        }
    }
}

@Composable
fun MarketRateItem(
    rate: MarketRateEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rate.productName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.grade_label) + ": ${rate.grade}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${Formatter.formatCurrency(rate.minRate)} - ₹${Formatter.formatCurrency(rate.maxRate)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRateDialog(
    rate: MarketRateEntity,
    onDismiss: () -> Unit,
    onSave: (MarketRateEntity) -> Unit
) {
    var minRate by remember { mutableStateOf(rate.minRate.toString()) }
    var maxRate by remember { mutableStateOf(rate.maxRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_market_rate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.product_label) + ": ${rate.productName} (${rate.grade})")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minRate,
                        onValueChange = { minRate = it },
                        label = { Text(stringResource(R.string.min_rate)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = maxRate,
                        onValueChange = { maxRate = it },
                        label = { Text(stringResource(R.string.max_rate)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(rate.copy(
                        minRate = minRate.toDoubleOrNull() ?: rate.minRate,
                        maxRate = maxRate.toDoubleOrNull() ?: rate.maxRate
                    ))
                },
                enabled = minRate.isNotEmpty() && maxRate.isNotEmpty()
            ) {
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRateDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedGrade by remember { mutableStateOf("") }
    var minRate by remember { mutableStateOf("") }
    var maxRate by remember { mutableStateOf("") }
    var expandedProduct by remember { mutableStateOf(false) }
    var expandedGrade by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_market_rate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedProduct,
                    onExpandedChange = { expandedProduct = !expandedProduct }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: stringResource(R.string.select_product),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProduct) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProduct,
                        onDismissRequest = { expandedProduct = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    selectedProduct = product
                                    selectedGrade = ""
                                    expandedProduct = false
                                }
                            )
                        }
                    }
                }

                if (selectedProduct != null) {
                    ExposedDropdownMenuBox(
                        expanded = expandedGrade,
                        onExpandedChange = { expandedGrade = !expandedGrade }
                    ) {
                        OutlinedTextField(
                            value = if (selectedGrade.isEmpty()) stringResource(R.string.select_grade) else selectedGrade,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGrade) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGrade,
                            onDismissRequest = { expandedGrade = false }
                        ) {
                            selectedProduct?.availableGrades?.forEach { grade ->
                                DropdownMenuItem(
                                    text = { Text(grade) },
                                    onClick = {
                                        selectedGrade = grade
                                        expandedGrade = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minRate,
                        onValueChange = { minRate = it },
                        label = { Text(stringResource(R.string.min_rate)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = maxRate,
                        onValueChange = { maxRate = it },
                        label = { Text(stringResource(R.string.max_rate)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minRate.toDoubleOrNull() ?: 0.0
                    val max = maxRate.toDoubleOrNull() ?: 0.0
                    selectedProduct?.let {
                        onSave(it.id, it.name, selectedGrade, min, max)
                    }
                },
                enabled = selectedProduct != null && selectedGrade.isNotEmpty() && minRate.isNotEmpty() && maxRate.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
