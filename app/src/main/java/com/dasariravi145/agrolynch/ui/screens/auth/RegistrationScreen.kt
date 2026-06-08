package com.dasariravi145.agrolynch.ui.screens.auth

import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
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

@Composable
fun RegistrationScreen(
    viewModel: AuthViewModel,
    onRegistered: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) {
            onRegistered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(stringResource(R.string.create_your_profile), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text(stringResource(R.string.location_market_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(stringResource(R.string.set_4digit_pin), fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("PIN") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = { Text(stringResource(R.string.confirm_pin)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )
        }
        
        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                if (pin == confirmPin && pin.length == 4) {
                    viewModel.onEvent(AuthEvent.RegisterUser(name, location, pin))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = name.isNotBlank() && pin.length == 4 && pin == confirmPin && !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
        ) {
            Text(stringResource(R.string.save_continue), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
