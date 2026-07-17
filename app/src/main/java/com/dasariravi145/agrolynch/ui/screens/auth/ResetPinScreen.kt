package com.dasariravi145.agrolynch.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.components.AuthLogo
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPinScreen(
    viewModel: AuthViewModel,
    onPinReset: () -> Unit,
    onBack: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isPinCorrect) {
        if (state.isPinCorrect) {
            Timber.tag("ForgotPinFlow").d("PIN updated. Navigating to Login.")
            onPinReset()
            viewModel.onEvent(AuthEvent.ClearError) // Clear to avoid loops
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthLogo()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Set New PIN",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4) newPin = it },
                label = { Text("New 4-Digit PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = { Text("Confirm New PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
            
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            
            if (state.isLoading && state.loadingMessage != null) {
                Text(state.loadingMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (newPin == confirmPin && newPin.length == 4) {
                        Timber.tag("ForgotPinFlow").d("Reset PIN button clicked")
                        viewModel.resetPin(newPin)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = newPin.length == 4 && newPin == confirmPin && !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}
