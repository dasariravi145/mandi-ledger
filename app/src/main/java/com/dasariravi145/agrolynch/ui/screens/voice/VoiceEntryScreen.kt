package com.dasariravi145.agrolynch.ui.screens.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    viewModel: VoiceViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isPremium) {
        if (!state.isPremium) {
            showPremiumDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Stock Entry") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                },
                label = "VoiceStepAnimation"
            ) { step ->
                when (step) {
                    VoiceStep.LANGUAGE_SELECTION -> LanguageSelectionStep(viewModel)
                    VoiceStep.INTERACTIVE_QUESTIONS -> InteractiveStep(viewModel, state)
                    VoiceStep.EDITABLE_FORM -> EditableFormStep(viewModel, state)
                    VoiceStep.SUCCESS -> SuccessStep {
                        viewModel.reset()
                        onBackClick()
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showPremiumDialog) {
        PremiumFeatureLockedDialog(
            onDismiss = onBackClick,
            onUpgradeClick = { /* Navigate to Premium */ }
        )
    }
}

@Composable
fun LanguageSelectionStep(viewModel: VoiceViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Select Language / భాషను ఎంచుకోండి", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(viewModel.languages) { lang ->
                Surface(
                    onClick = { viewModel.selectLanguage(lang) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lang.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveStep(viewModel: VoiceViewModel, state: VoiceState) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startListening(context)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = viewModel.getCurrentQuestion(),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = if (state.isListening) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                )
                .clickable {
                    if (state.isListening) viewModel.stopListening()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Mic",
                modifier = Modifier.size(56.dp),
                tint = if (state.isListening) Color.Red else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        if (state.spokenText.isNotEmpty()) {
            Text("Captured: \"${state.spokenText}\"", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Badge(containerColor = if(state.confidence > 0.7) Color(0xFF2E7D32) else Color.Red) {
                Text("Confidence: ${(state.confidence * 100).toInt()}%", color = Color.White)
            }
        }
    }
}

@Composable
fun EditableFormStep(viewModel: VoiceViewModel, state: VoiceState) {
    var session by remember { mutableStateOf(state.session) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Verify & Complete Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FARMER DETAILS", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(value = session.farmerName, onValueChange = { session = session.copy(farmerName = it) }, label = { Text("Farmer Name *") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = session.farmerPhone, 
                        onValueChange = { 
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                session = session.copy(farmerPhone = it) 
                            }
                        }, 
                        label = { Text("Phone") }, 
                        modifier = Modifier.weight(1f), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(value = session.farmerAddress, onValueChange = { session = session.copy(farmerAddress = it) }, label = { Text("Address/Village") }, modifier = Modifier.weight(1f))
                }

                HorizontalDivider()
                Text("PRODUCT DETAILS", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(value = session.productName, onValueChange = { session = session.copy(productName = it) }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = session.productCategory, onValueChange = { session = session.copy(productCategory = it) }, label = { Text("Category *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = session.grade, onValueChange = { session = session.copy(grade = it) }, label = { Text("Grade *") }, modifier = Modifier.weight(1f))
                }

                HorizontalDivider()
                Text("QUANTITY & RATES", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = session.quantity.toString(), onValueChange = { session = session.copy(quantity = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Quantity (KG) *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = session.spoilage.toString(), onValueChange = { session = session.copy(spoilage = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Damage / Soot") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = session.rate.toString(), onValueChange = { session = session.copy(rate = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Rate *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = session.amount.toString(), onValueChange = { session = session.copy(amount = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Voice Amt (Ref)") }, modifier = Modifier.weight(1f), readOnly = true)
                }

                HorizontalDivider()
                Text("CHARGES", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = session.commission.toString(), onValueChange = { session = session.copy(commission = it.toDoubleOrNull() ?: 5.0) }, label = { Text("Comm %") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = session.labor.toString(), onValueChange = { session = session.copy(labor = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Labor") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            enabled = session.farmerName.isNotEmpty() && session.productName.isNotEmpty() && session.quantity > 0
        ) {
            Text("Review & Save", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        Spacer(Modifier.height(40.dp))
    }

    if (showConfirmDialog) {
        val netAmt = (session.quantity * session.rate) - (session.quantity * session.rate * session.commission / 100) - session.transport - session.labor - session.packing
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm and Save?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Farmer: ${session.farmerName}")
                    Text("Product: ${session.productName} (${session.grade})")
                    Text("Qty: ${session.quantity} ${session.unit}")
                    Text("Net Payable: ₹${String.format(Locale.US, "%.2f", netAmt)}")
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.updateSession(session)
                    viewModel.saveEntry()
                    showConfirmDialog = false 
                }) { Text("Confirm Save") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Edit") }
            }
        )
    }
}

@Composable
fun SuccessStep(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(16.dp))
        Text("Success!", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Stock entry saved successfully.")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}
