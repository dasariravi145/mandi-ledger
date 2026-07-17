package com.dasariravi145.agrolynch.ui.screens.auth

import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ui.components.AuthLogo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import com.dasariravi145.agrolynch.util.findActivity

@Composable
fun RegistrationScreen(
    viewModel: AuthViewModel,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val appContext = context.applicationContext

    LaunchedEffect(Unit) {
        viewModel.checkBiometricAvailability(appContext)
    }

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) {
            onRegistered()
        }
    }

    if (state.showBiometricSetupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.enableBiometric(false) },
            title = { Text("Enable Fingerprint Login?") },
            text = { Text("Use your fingerprint or device biometric to securely open Mandi Ledger.") },
            confirmButton = {
                TextButton(onClick = { viewModel.enableBiometric(true, activity) }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.enableBiometric(false) }) {
                    Text("Not Now")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthLogo()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Create Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address / Market Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("PIN") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = { Text("Confirm") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
        }
        
        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        
        if (state.isLoading && state.loadingMessage != null) {
            Text(state.loadingMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (pin == confirmPin && pin.length == 4) {
                    viewModel.onEvent(AuthEvent.RegisterUser(name, address, pin))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = name.isNotBlank() && address.isNotBlank() && pin.length == 4 && pin == confirmPin && !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
