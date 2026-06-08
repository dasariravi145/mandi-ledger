package com.dasariravi145.agrolynch.ui.screens.expense

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
import com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalToday by viewModel.totalToday.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ExpenseSummaryCard(totalToday)
            
            if (isLoading && expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(text = stringResource(R.string.recent_expenses), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    items(expenses) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onClick = { selectedExpenseToEdit = expense }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddExpenseDialog(
                onDismiss = { showAddDialog = false },
                onSave = { type, amount, desc ->
                    viewModel.addExpense(type, amount, desc)
                    showAddDialog = false
                }
            )
        }

        if (selectedExpenseToEdit != null) {
            EditExpenseDialog(
                expense = selectedExpenseToEdit!!,
                onDismiss = { selectedExpenseToEdit = null },
                onUpdate = { updated ->
                    viewModel.updateExpense(updated)
                    selectedExpenseToEdit = null
                },
                onDelete = { id ->
                    viewModel.deleteExpense(id)
                    selectedExpenseToEdit = null
                }
            )
        }
    }
}

@Composable
fun ExpenseSummaryCard(total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.today_total_expenses), fontSize = 14.sp)
            Text(text = "₹$total", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun ExpenseItem(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.type, fontWeight = FontWeight.Bold)
                if (expense.description.isNotBlank()) {
                    Text(text = expense.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Text(text = dateFormat.format(Date(expense.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
            Text(text = "₹${expense.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    val expenseTypes = listOf("Transport", "Labor", "Ice", "Packing", "Loading", "Market Fee")
    var selectedType by remember { mutableStateOf(expenseTypes[0]) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        expenseTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { selectedType = type; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₹") }
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onSave(selectedType, amt, description)
                },
                enabled = amount.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onUpdate: (ExpenseEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var description by remember { mutableStateOf(expense.description) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_expense_q)) },
            text = { Text(stringResource(R.string.delete_expense_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(expense.id) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.type) + ": ${expense.type}")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_record), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(expense.copy(
                        amount = amount.toDoubleOrNull() ?: expense.amount,
                        description = description
                    ))
                }
            ) {
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
