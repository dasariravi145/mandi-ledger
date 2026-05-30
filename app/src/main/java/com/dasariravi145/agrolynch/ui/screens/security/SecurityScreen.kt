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

@Composable
fun SecurityScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onForgotPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isPinCorrect) {
        if (state.isPinCorrect) {
            onAuthenticated()
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
            Spacer(modifier = Modifier.height(64.dp))
            Text("Enter Your PIN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("మీ పిన్ నమోదు చేయండి", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    PinDot(isFilled = index < pin.length)
                }
            }
            
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
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
                Text("Forgot PIN? / పిన్ మర్చిపోయారా?", color = Color(0xFF16A34A))
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
            .background(if (isFilled) Color(0xFF16A34A) else Color.LightGray)
            .border(1.dp, Color.Gray, CircleShape)
    )
}

@Composable
fun KeyButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = Color(0xFFF3F4F6)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text == "DEL") {
                Icon(Icons.Default.Backspace, contentDescription = null, tint = Color.Black)
            } else {
                Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
