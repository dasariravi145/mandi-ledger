package com.dasariravi145.agrolynch.ui.screens.auth

import android.app.Activity
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPinScreen(
    viewModel: AuthViewModel,
    onOtpSent: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current as Activity
    val phoneNumber = viewModel.getCurrentUserPhoneNumber() ?: ""

    LaunchedEffect(state.isOtpSent) {
        if (state.isOtpSent) {
            onOtpSent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reset_pin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.forgot_pin_q),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.otp_send_desc, phoneNumber),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (phoneNumber.isNotEmpty()) {
                        viewModel.onEvent(AuthEvent.SendOtp(phoneNumber, activity))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && phoneNumber.isNotEmpty()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.send_otp))
                }
            }
        }
    }
}
