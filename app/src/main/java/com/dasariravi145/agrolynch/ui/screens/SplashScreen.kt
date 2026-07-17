package com.dasariravi145.agrolynch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.auth.AuthViewModel
import com.dasariravi145.agrolynch.util.LanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import com.google.firebase.FirebaseApp

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigate: (Boolean, Boolean, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(key1 = true) {
        Timber.tag("SplashFlow").d("Splash started")
        
        try {
            // Set a hard timeout of 5 seconds for the entire startup sequence
            withTimeout(5000) {
                Timber.tag("FirebaseInit").d("FirebaseApp.initializeApp starting")
                try {
                    FirebaseApp.initializeApp(context)
                    Timber.tag("FirebaseInit").d("FirebaseApp initialized")
                } catch (e: Exception) {
                    Timber.tag("FirebaseInit").e(e, "Firebase initialization error")
                }

                delay(1000) // Branding delay

                // Run IO-bound checks in a background thread to prevent Main thread hang
                val startupData = withContext(Dispatchers.IO) {
                    Timber.tag("Session").d("Loading startup data from IO")
                    
                    val isLoggedIn = viewModel.isUserLoggedIn()
                    Timber.tag("Session").d("isLoggedIn: $isLoggedIn")

                    var isPinCreated = viewModel.isPinCreated() || viewModel.hasSavedPin()
                    var hasProfile = viewModel.isProfileCreated() || (try { viewModel.getLocalUser() } catch(e: Exception) { null }) != null

                    // If Firebase user exists but local profile is missing, attempt restore (e.g., after reinstall)
                    if (isLoggedIn && !hasProfile) {
                        Timber.tag("SplashFlow").d("Firebase user exists but local profile missing. Attempting restore.")
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val restored = viewModel.checkAndRestoreProfile(uid)
                            if (restored) {
                                Timber.tag("SplashFlow").d("Found existing profile on Firestore. Restored successfully.")
                                isPinCreated = true
                                hasProfile = true
                            }
                        }
                    }

                    val isLangSelected = try {
                        LanguageManager.isLanguageSelected(context).firstOrNull() ?: false
                    } catch (e: Exception) {
                        Timber.tag("SplashFlow").e(e, "Language check failed")
                        false
                    }
                    Timber.tag("SplashFlow").d("isLangSelected: $isLangSelected")
                    
                    Timber.tag("Session").d("Data loaded: isLoggedIn=$isLoggedIn, isLangSelected=$isLangSelected, hasProfile=$hasProfile, isPinCreated=$isPinCreated")
                    
                    Triple(isLoggedIn, isLangSelected, hasProfile && isPinCreated)
                }

                // If logged in, fetch profile in background (don't block navigation)
                if (startupData.first) {
                    val phone = viewModel.getCurrentUserPhoneNumber()
                    if (phone != null) {
                        viewModel.fetchUserProfile(phone)
                    }
                }

                Timber.tag("Navigation").d("Navigating: isLoggedIn=${startupData.first}, isLangSelected=${startupData.second}, hasProfile=${startupData.third}")
                onNavigate(startupData.first, startupData.second, startupData.third)
            }
        } catch (e: Exception) {
            Timber.tag("SplashFlow").e(e, "Splash critical timeout or error")
            // EMERGENCY FALLBACK: Go to login if we hang or crash
            try {
                Timber.tag("Navigation").d("Emergency navigation to login")
                onNavigate(false, true, false)
            } catch (navEx: Exception) {
                Timber.tag("Navigation").e(navEx, "Emergency navigation failed")
            }
        }
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
