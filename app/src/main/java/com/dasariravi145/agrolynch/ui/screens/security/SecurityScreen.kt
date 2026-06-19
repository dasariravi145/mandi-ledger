package com.dasariravi145.agrolynch.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.auth.AuthEvent
import com.dasariravi145.agrolynch.ui.screens.auth.AuthViewModel
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ui.components.AuthLogo
import androidx.compose.material.icons.filled.Fingerprint
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import com.dasariravi145.agrolynch.util.BiometricAuth

@Composable
fun SecurityScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onForgotPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        viewModel.checkBiometricAvailability(context)
    }

    LaunchedEffect(state.isPinCorrect) {
        if (state.isPinCorrect) {
            onAuthenticated()
        }
    }

    // Auto-show biometric if enabled
    LaunchedEffect(state.isBiometricEnabled, state.isBiometricAvailable) {
        if (state.isBiometricEnabled && state.isBiometricAvailable && activity != null) {
            BiometricAuth.showBiometricPrompt(
                activity = activity,
                title = context.getString(R.string.biometric_login),
                subtitle = context.getString(R.string.biometric_subtitle),
                negativeButtonText = context.getString(R.string.use_pin),
                errorAuthFailed = context.getString(R.string.biometric_failed),
                onSuccess = { viewModel.onBiometricSuccess() },
                onError = { viewModel.onBiometricFailure(it) }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AuthLogo()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(stringResource(R.string.enter_pin), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    PinDot(isFilled = index < pin.length)
                }
            }
            
            if (state.error != null) {
                val errorMessage = if (state.error == "invalid_pin") stringResource(R.string.invalid_pin) else state.error!!
                Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            if (state.isBiometricEnabled && state.isBiometricAvailable) {
                Spacer(modifier = Modifier.height(24.dp))
                IconButton(
                    onClick = {
                        if (activity != null) {
                            BiometricAuth.showBiometricPrompt(
                                activity = activity,
                                title = context.getString(R.string.biometric_login),
                                subtitle = context.getString(R.string.biometric_subtitle),
                                negativeButtonText = context.getString(R.string.use_pin),
                                errorAuthFailed = context.getString(R.string.biometric_failed),
                                onSuccess = { viewModel.onBiometricSuccess() },
                                onError = { viewModel.onBiometricFailure(it) }
                            )
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = "Use Fingerprint",
                    color = Color(0xFF16A34A),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Numeric Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
            
            for (i in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (j in 0 until 3) {
                        val key = keys[i * 3 + j]
                        if (key.isNotEmpty()) {
                            KeyButton(
                                text = key,
                                onClick = {
                                    if (state.error != null) {
                                        pin = ""
                                        viewModel.onEvent(AuthEvent.ClearError)
                                    }
                                    if (key == "DEL") {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    } else {
                                        if (pin.length < 4) {
                                            pin += key
                                            if (pin.length == 4) {
                                                viewModel.onEvent(AuthEvent.VerifyPin(pin))
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }
            
            TextButton(
                onClick = onForgotPin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.forgot_pin_q), color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PinDot(isFilled: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (isFilled) Color(0xFF16A34A) else Color.Transparent)
            .border(2.dp, if (isFilled) Color(0xFF16A34A) else Color.Gray, CircleShape)
    )
}

@Composable
fun KeyButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = Color(0xFFF3F4F6)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
            } else if (text == "DEL") {
                Icon(Icons.Default.Backspace, contentDescription = null, tint = Color.Black)
            } else if (text != null) {
                Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
