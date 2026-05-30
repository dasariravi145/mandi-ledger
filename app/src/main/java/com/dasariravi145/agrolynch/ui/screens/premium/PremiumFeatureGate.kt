package com.dasariravi145.agrolynch.ui.screens.premium

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PremiumFeatureLockedDialog(
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Premium Feature Locked",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("This feature requires a Premium Subscription. Upgrade now to unlock OCR scanning, Cloud backup, and more!")
        },
        confirmButton = {
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
