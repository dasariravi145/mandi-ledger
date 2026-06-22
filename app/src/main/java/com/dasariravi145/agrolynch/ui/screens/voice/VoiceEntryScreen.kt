package com.dasariravi145.agrolynch.ui.screens.voice

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
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
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    viewModel: VoiceViewModel,
    onNavigateToArrival: (FarmerArrivalDraft, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event is VoiceNavigationEvent.NavigateToArrival) {
                onNavigateToArrival(event.draft, event.autoSave)
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
                title = { Text("Guided Voice Entry", fontWeight = FontWeight.Bold) },
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

            if (state.step != VoiceStep.LANGUAGE_SELECTION && state.step != VoiceStep.COMPLETED) {
                ProgressChecklist(state.step)
            }

            if (state.step == VoiceStep.LANGUAGE_SELECTION) {
                LanguageSelectionPlaceholder()
            } else {
                WizardStepUI(viewModel, state)
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
fun ProgressChecklist(currentStep: VoiceStep) {
    val steps = VoiceStep.entries.filter { 
        it != VoiceStep.LANGUAGE_SELECTION && it != VoiceStep.COMPLETED
    }
    
    val scrollState = rememberScrollState()
    
    // Auto scroll to current step
    LaunchedEffect(currentStep) {
        val index = steps.indexOf(currentStep)
        if (index >= 0) {
            scrollState.animateScrollTo(index * 80) // rough estimate
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(steps.size) { index ->
            val step = steps[index]
            val isCompleted = step.ordinal < currentStep.ordinal
            val isCurrent = step == currentStep
            
            Box(
                modifier = Modifier
                    .background(
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> Color(0xFFE8F5E9)
                            else -> Color.White
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = step.label,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Color.White else if (isCompleted) Color(0xFF2E7D32) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WizardStepUI(viewModel: VoiceViewModel, state: VoiceState) {
    val context = LocalContext.current
    
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                viewModel.onSpokenResult(spokenText)
            }
        }
    }

    val currentPrompt = viewModel.getCurrentQuestion()

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.step.label, 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = currentPrompt,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(80.dp)
        )

        if (state.awaitingConfirmation) {
            DetectionResultUI(viewModel, state)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, state.selectedLanguage?.locale ?: "en-IN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, currentPrompt)
                            }
                            voiceLauncher.launch(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Tap to Speak", color = Color.Gray)
            }
        }
        
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))

        if (viewModel.isOptional(state.step) && !state.awaitingConfirmation) {
            TextButton(onClick = { viewModel.skipCurrentStep() }) {
                Text("Skip Optional Field")
            }
        }
        
        if (!state.awaitingConfirmation) {
            TextButton(onClick = { viewModel.forceComplete() }) {
                Text("Go to Form", color = Color.Gray)
            }
        }
    }
}

@Composable
fun DetectionResultUI(viewModel: VoiceViewModel, state: VoiceState) {
    var manualValue by remember { mutableStateOf(state.detectedValue) }
    var isEditing by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Heard: \"${state.spokenText}\"", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        if (isEditing) {
            OutlinedTextField(
                value = manualValue,
                onValueChange = { manualValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correct value") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.updateManually(manualValue); viewModel.confirmValue() }) {
                    Text("Confirm & Next")
                }
                TextButton(onClick = { isEditing = false }) { Text("Cancel") }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Detected:", fontSize = 12.sp, color = Color.Gray)
                    Text(state.detectedValue.ifEmpty { "(Empty)" }, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.confirmValue() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirm")
                }
                OutlinedButton(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Retry")
                }
            }
            TextButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Type Manually")
            }
        }
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
            Text("Language: ${selected?.name ?: "Select Language..."}")
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("Please select your language above to start", color = Color.Gray)
        }
    }
}
