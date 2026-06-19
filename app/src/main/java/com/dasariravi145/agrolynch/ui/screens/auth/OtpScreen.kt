package com.dasariravi145.agrolynch.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.R

import androidx.compose.ui.graphics.Color
import com.dasariravi145.agrolynch.ui.components.AuthLogo
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) {
            Timber.d("OTP_VERIFY_SUCCESS")
            onVerified()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthLogo()
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.verify_otp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = state.phoneNumber,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { 
                    if (it.length <= 6) {
                        otp = it
                        Timber.d("OTP_CODE_LENGTH: ${it.length}")
                    }
                },
                label = { Text(stringResource(R.string.enter_otp)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = state.error != null,
                shape = MaterialTheme.shapes.medium
            )

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.isLoading && state.loadingMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.loadingMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (otp.length == 6) {
                        Timber.d("AUTH_DEBUG: OTP VERIFY BUTTON CLICKED")
                        viewModel.onEvent(AuthEvent.VerifyOtp(otp))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isLoading && otp.length == 6,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                if (state.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Verifying...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(stringResource(R.string.verify), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
