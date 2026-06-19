package com.dasariravi145.agrolynch.ui.screens.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(
    viewModel: DeveloperViewModel,
    onBack: () -> Unit
) {
    if (!BuildConfig.DEBUG) {
        onBack()
        return
    }

    val premiumOverride by viewModel.premiumOverride.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Options") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Premium Testing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Override State:")
                        Text(
                            text = when(premiumOverride) {
                                true -> "Premium Active (Testing)"
                                false -> "INACTIVE"
                                null -> "NONE (Using Real Billing)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = when(premiumOverride) {
                                true -> Color(0xFF16A34A)
                                false -> Color.Red
                                null -> Color.Gray
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.setPremiumOverride(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("Enable Premium")
            }

            Button(
                onClick = { viewModel.setPremiumOverride(false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Disable Premium")
            }

            OutlinedButton(
                onClick = { viewModel.setPremiumOverride(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Premium State")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Note: These settings only apply to DEBUG builds and will not affect production users.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
