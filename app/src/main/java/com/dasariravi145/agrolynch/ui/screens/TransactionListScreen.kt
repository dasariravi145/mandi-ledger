package com.dasariravi145.agrolynch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.dasariravi145.agrolynch.util.Formatter
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    Timber.d("TransactionListScreen: Initializing...")
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mandi Ledger (అగ్రో లించ్)") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(
                items = state.transactions,
                key = { it.id }
            ) { transaction ->
                ListItem(
                    headlineContent = { Text(transaction.farmerName) },
                    supportingContent = { Text("${transaction.productName.ifEmpty { transaction.fruitType }} - ${Formatter.formatWeight(transaction.quantity)} units") },
                    trailingContent = { Text("₹${Formatter.formatCurrency(transaction.totalAmount)}") },
                    modifier = Modifier.clickable { 
                        Timber.d("TransactionListScreen: Transaction clicked: ${transaction.id}")
                        onTransactionClick(transaction.id) 
                    }
                )
            }
        }
    }
}
