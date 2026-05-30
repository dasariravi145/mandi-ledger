package com.dasariravi145.agrolynch.ui.screens.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBuyerScreen(
    viewModel: BuyerViewModel,
    buyerId: String? = null,
    onBack: () -> Unit
) {
    Timber.d("AddEditBuyerScreen: Initializing for id: $buyerId")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            Timber.d("AddEditBuyerScreen: Save successful, navigating back")
            onBack()
        }
    }

    LaunchedEffect(buyerId, state.buyers) {
        if (buyerId != null && !hasInitialized && state.buyers.isNotEmpty()) {
            val buyer = state.buyers.find { it.id == buyerId }
            buyer?.let {
                Timber.d("AddEditBuyerScreen: Initializing with existing buyer: ${it.name}")
                name = it.name
                mobile = it.mobileNumber
                address = it.address
                gst = it.gstNumber
                notes = it.notes
                hasInitialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (buyerId == null) "Add Buyer / జోడించండి" else "Edit Buyer / సవరించండి") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (buyerId != null) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Buyer/Trader Name / వ్యాపారి పేరు") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { if (it.length <= 10) mobile = it },
                label = { Text("Mobile Number / మొబైల్ సంఖ్య") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address / చిరునామా") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = gst,
                onValueChange = { gst = it },
                label = { Text("GST Number (Optional) / జిఎస్టి") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / గమనికలు") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    Timber.d("AddEditBuyerScreen: Save button clicked")
                    if (buyerId == null) {
                        viewModel.addBuyer(name, mobile, address, gst, notes)
                    } else {
                        val existingBuyer = state.buyers.find { it.id == buyerId }
                        existingBuyer?.let {
                            viewModel.updateBuyer(it.copy(
                                name = name,
                                mobileNumber = mobile,
                                address = address,
                                gstNumber = gst,
                                notes = notes
                            ))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && mobile.length == 10 && address.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save Buyer / సేవ్ చేయండి")
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Buyer?") },
                text = { Text("Are you sure you want to delete this buyer? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Timber.d("AddEditBuyerScreen: Deleting buyer: $buyerId")
                            buyerId?.let { viewModel.deleteBuyer(it) }
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
