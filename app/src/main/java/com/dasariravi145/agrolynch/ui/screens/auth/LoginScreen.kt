package com.dasariravi145.agrolynch.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.R

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOtpSent: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isOtpSent) {
        if (state.isOtpSent) {
            onOtpSent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF16A34A)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.welcome_to_mandi_ledger),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 10) phoneNumber = it },
            label = { Text(stringResource(R.string.mobile_number)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Text("+91 ", modifier = Modifier.padding(start = 12.dp)) },
            shape = MaterialTheme.shapes.medium
        )
        
        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                if (phoneNumber.length == 10) {
                    viewModel.onEvent(AuthEvent.SendOtp("+91$phoneNumber", context as Activity))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isLoading && phoneNumber.length == 10,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = MaterialTheme.shapes.large
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.send_otp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
