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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    viewModel: VoiceViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
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
                title = { Text("AI Voice Entry") },
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
            when (state.step) {
                VoiceStep.LANGUAGE_SELECTION -> LanguageSelectionStep(viewModel)
                VoiceStep.SERVICE_SELECTION -> ServiceSelectionStep(viewModel)
                VoiceStep.INTERACTIVE_QUESTIONS -> InteractiveStep(viewModel, state)
                VoiceStep.REVIEW -> ReviewStep(viewModel, state)
                VoiceStep.SUCCESS -> SuccessStep {
                    viewModel.reset()
                    onBackClick()
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
            onUpgradeClick = { /* Navigate to Premium handled externally or just close */ }
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
fun ServiceSelectionStep(viewModel: VoiceViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("What do you want to create?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        val services = VoiceService.entries
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(services) { service ->
                Surface(
                    onClick = { viewModel.selectService(service) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(service.name.replace("_", " "), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Add, null)
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
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = if (state.isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
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
                modifier = Modifier.size(48.dp),
                tint = if (state.isListening) Color.Red else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        if (state.spokenText.isNotEmpty()) {
            Text("Captured: \"${state.spokenText}\"", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }

        if (state.existingRecordFound) {
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Existing Record Found!", fontWeight = FontWeight.Bold)
                    Text("Use these details?", fontSize = 12.sp)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.useExistingRecord(true) }) { Text("YES") }
                        OutlinedButton(onClick = { viewModel.useExistingRecord(false) }) { Text("NO") }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewStep(viewModel: VoiceViewModel, state: VoiceState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Review Entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.capturedData.forEach { (key, value) ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(key.replaceFirstChar { it.uppercase() } + ":", color = Color.Gray)
                        Text(value, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = { viewModel.saveEntry() }, modifier = Modifier.weight(1f)) {
                Text("Save Record")
            }
        }
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
        Text("Voice entry saved successfully.")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}
