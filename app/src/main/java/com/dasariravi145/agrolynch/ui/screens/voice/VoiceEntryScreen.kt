package com.dasariravi145.agrolynch.ui.screens.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    viewModel: VoiceViewModel,
    onNavigateToArrival: (FarmerArrivalDraft) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event is VoiceNavigationEvent.NavigateToArrival) {
                onNavigateToArrival(event.draft)
            }
        }
    }

    LaunchedEffect(state.isPremium) {
        if (!state.isPremium) {
            showPremiumDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_entry), fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            LanguageSelector(viewModel, state.selectedLanguage)

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                },
                label = "VoiceStepAnimation",
                modifier = Modifier.weight(1f)
            ) { step ->
                if (step == VoiceStep.LANGUAGE_SELECTION) {
                    LanguageSelectionPlaceholder()
                } else {
                    InteractiveStep(viewModel, state)
                }
            }
            
            VoiceChecklist(state.draft)
            
            if (state.draft.farmerName.isNotEmpty() && state.draft.productName.isNotEmpty()) {
                Button(
                    onClick = { viewModel.forceComplete() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Review & Save")
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
fun LanguageSelector(viewModel: VoiceViewModel, selected: VoiceLanguage?) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Language, null)
            Spacer(Modifier.width(8.dp))
            Text("Language: ${selected?.name ?: "Select..."}")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
            viewModel.languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = { viewModel.selectLanguage(lang); expanded = false }
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Please select a language to start", color = Color.Gray)
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
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = viewModel.getCurrentQuestion(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(60.dp)
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
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
                modifier = Modifier.size(48.dp),
                tint = if (state.isListening) Color.Red else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        if (state.spokenText.isNotEmpty()) {
            Text(
                "Captured: \"${state.spokenText}\"", 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }
        
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { viewModel.skipCurrentStep() }, shape = RoundedCornerShape(8.dp)) {
                Text("Skip")
            }
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, shape = RoundedCornerShape(8.dp)) {
                Text(if (state.spokenText.isEmpty()) "Speak" else "Speak Again")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(onClick = { viewModel.forceComplete() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Manually", color = Color.Gray)
            }
        }
    }
}

@Composable
fun VoiceChecklist(draft: FarmerArrivalDraft) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier.padding(12.dp).verticalScroll(rememberScrollState()), 
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Progress Checklist", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.Gray)
            HorizontalDivider(Modifier.padding(bottom = 4.dp), thickness = 0.5.dp)
            
            ChecklistItem("Farmer Name", draft.farmerName)
            ChecklistItem("Phone", draft.phone)
            ChecklistItem("Village", draft.village)
            ChecklistItem("Product", draft.productName)
            ChecklistItem("Grade", draft.grade)
            ChecklistItem("Unit", draft.unitType)
            ChecklistItem("Quantity", if (draft.quantity > 0) draft.quantity.toString() else "")
            ChecklistItem("Rate", if (draft.rate > 0) "₹${draft.rate}" else "")
        }
    }
}

@Composable
fun ChecklistItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank() && value != "0.0") {
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
            } else {
                Text("Pending", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
        }
    }
}
