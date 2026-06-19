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

import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.dasariravi145.agrolynch.util.BiometricAuth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scrollState = rememberScrollState()

    LaunchedEffect(state.user) {
        state.user?.let {
            if (name.isEmpty()) name = it.name
            if (location.isEmpty()) location = it.location
        }
    }

    LaunchedEffect(Unit) {
        timber.log.Timber.d("CREATE_PROFILE_OPENED")
        val uid = viewModel.getCurrentUserId()
        if (uid != null) {
            viewModel.fetchUserProfile(uid)
        }
    }

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) {
            onRegistered()
        }
    }

    if (state.isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF16A34A))
        }
        return
    }

    if (state.showBiometricPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBiometricPrompt() },
            title = { Text(stringResource(R.string.enable_biometric)) },
            text = { Text(stringResource(R.string.enable_biometric_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    if (activity != null) {
                        BiometricAuth.showBiometricPrompt(
                            activity = activity,
                            title = context.getString(R.string.biometric_login),
                            subtitle = context.getString(R.string.biometric_subtitle),
                            negativeButtonText = context.getString(R.string.skip),
                            errorAuthFailed = context.getString(R.string.biometric_failed),
                            onSuccess = {
                                viewModel.setBiometricEnabled(true)
                                viewModel.dismissBiometricPrompt()
                            },
                            onError = {
                                viewModel.onBiometricFailure(it)
                                viewModel.dismissBiometricPrompt()
                            }
                        )
                    } else {
                        viewModel.dismissBiometricPrompt()
                    }
                }) {
                    Text(stringResource(R.string.enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBiometricPrompt() }) {
                    Text(stringResource(R.string.skip))
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
        
        val title = if (state.isRegistered) stringResource(R.string.reset_pin) else stringResource(R.string.create_your_profile)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!state.isRegistered) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.full_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.location_market_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        Text(stringResource(R.string.set_4digit_pin), fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text(stringResource(R.string.pin_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = { Text(stringResource(R.string.confirm_pin)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !state.isLoading
            )
        }
        
        if (state.error != null) {
            Text(state.error!!, color = if (state.error!!.contains("successfully")) Color(0xFF16A34A) else MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        
        if (state.isLoading && state.loadingMessage != null) {
            Text(state.loadingMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (pin == confirmPin && pin.length == 4) {
                    viewModel.onEvent(AuthEvent.RegisterUser(name, location, pin))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = (state.isRegistered || (name.isNotBlank() && location.isNotBlank())) && pin.length == 4 && pin == confirmPin && !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.save_continue), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
