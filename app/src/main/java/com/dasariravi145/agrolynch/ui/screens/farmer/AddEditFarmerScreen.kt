package com.dasariravi145.agrolynch.ui.screens.farmer

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
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFarmerScreen(
    viewModel: FarmerViewModel,
    farmerId: String? = null,
    onBack: () -> Unit
) {
    Timber.d("AddEditFarmerScreen: Initializing for id: $farmerId")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val farmers by viewModel.filteredFarmers.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            Timber.d("AddEditFarmerScreen: Save successful, navigating back")
            onBack()
        }
    }

    LaunchedEffect(farmerId, state.farmers) {
        if (farmerId != null && !hasInitialized && state.farmers.isNotEmpty()) {
            val farmer = state.farmers.find { it.id == farmerId }
            farmer?.let {
                Timber.d("AddEditFarmerScreen: Initializing with existing farmer: ${it.name}")
                name = it.name
                mobile = it.mobileNumber
                village = it.village
                notes = it.notes
                hasInitialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (farmerId == null) "Add Farmer / జోడించండి" else "Edit Farmer / సవరించండి") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (farmerId != null) {
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
                label = { Text("Farmer Name / రైతు పేరు") },
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
                value = village,
                onValueChange = { village = it },
                label = { Text("Village / గ్రామం") },
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
                    Timber.d("AddEditFarmerScreen: Save button clicked")
                    if (farmerId == null) {
                        viewModel.addFarmer(name, mobile, village, notes)
                    } else {
                        val existingFarmer = state.farmers.find { it.id == farmerId }
                        existingFarmer?.let {
                            viewModel.updateFarmer(it.copy(
                                name = name,
                                mobileNumber = mobile,
                                village = village,
                                notes = notes
                            ))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && mobile.length == 10 && village.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save Farmer / సేవ్ చేయండి")
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Farmer?") },
                text = { Text("Are you sure you want to delete this farmer? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Timber.d("AddEditFarmerScreen: Deleting farmer: $farmerId")
                            farmerId?.let { viewModel.deleteFarmer(it) }
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
