package com.dasariravi145.agrolynch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.auth.AuthViewModel
import com.dasariravi145.agrolynch.util.LanguageManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigate: (Boolean, Boolean, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLanguageSelected by LanguageManager.isLanguageSelected(context).collectAsState(initial = false)

    LaunchedEffect(key1 = true) {
        delay(800)
        val isLoggedIn = viewModel.isUserLoggedIn()
        val hasPin = viewModel.hasSavedPin()
        onNavigate(isLoggedIn, isLanguageSelected, hasPin)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Mandi Ledger",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mandi Agent Solution",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
