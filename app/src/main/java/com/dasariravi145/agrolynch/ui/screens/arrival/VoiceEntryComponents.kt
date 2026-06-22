package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLanguageSelector(
    onLanguageSelected: (VoiceEntryLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Select Voice Language",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            VoiceEntryLanguage.entries.forEach { lang ->
                Surface(
                    onClick = { onLanguageSelected(lang) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceEntryReviewDialog(
    data: ParsedArrivalVoiceData,
    onConfirm: (ParsedArrivalVoiceData) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Detected Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\"${data.heardText}\"",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ReviewItem("Farmer", data.farmerName)
                ReviewItem("Product", data.product)
                ReviewItem("Grade", data.grade)
                ReviewItem("Quantity", data.quantity?.toString(), data.unit)
                ReviewItem("Rate", data.rate?.toString())
                ReviewItem("Commission", data.commission?.toString(), "%")
                ReviewItem("Transport", data.transport?.toString())
                ReviewItem("Labour", data.labor?.toString())
                ReviewItem("CAT/Other", data.otherDeduction?.toString())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(data) }) {
                Text("Confirm & Fill Form")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun ReviewItem(label: String, value: String?, suffix: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        if (value != null && value != "null") {
            Text(
                text = if (suffix != null) "$value $suffix" else value,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                "Needs review",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
