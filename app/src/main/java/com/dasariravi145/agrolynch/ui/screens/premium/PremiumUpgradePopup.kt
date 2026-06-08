package com.dasariravi145.agrolynch.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dasariravi145.agrolynch.R

@Composable
fun PremiumUpgradePopup(
    onUpgradeClick: () -> Unit,
    onSkipClick: (Boolean) -> Unit
) {
    var doNotShowAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onSkipClick(doNotShowAgain) },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Unlock powerful features for your mandi business.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Benefits List
                val benefits = listOf(
                    "Ad-free experience",
                    "Unlimited OCR Bill Scanning",
                    "AI Vision Bill Scan",
                    "Voice Entry",
                    "Cloud Backup & Restore",
                    "Export PDF & Excel Reports",
                    "WhatsApp PDF Share",
                    "Priority Support"
                )

                benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { doNotShowAgain = !doNotShowAgain },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = doNotShowAgain,
                        onCheckedChange = { doNotShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.dont_show_again),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text(
                        text = "Upgrade Now",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onSkipClick(doNotShowAgain) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for Now",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
