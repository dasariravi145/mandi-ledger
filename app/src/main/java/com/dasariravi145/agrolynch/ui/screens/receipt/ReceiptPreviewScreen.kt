package com.dasariravi145.agrolynch.ui.screens.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    data: ReceiptData,
    viewModel: ReceiptViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Preview / రసీదు ప్రివ్యూ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateAndShare(context, data) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { viewModel.generateAndShare(context, data) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share PDF / వాట్సాప్ ద్వారా పంపండి")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Gray.copy(alpha = 0.1f))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header
                    Text(text = data.agentName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Contact: ${data.agentContact}", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "RECEIPT FOR", fontSize = 12.sp, color = Color.Gray)
                            Text(text = data.partyName, fontWeight = FontWeight.Bold)
                            Text(text = data.partyType, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "DATE", fontSize = 12.sp, color = Color.Gray)
                            Text(text = dateFormat.format(Date(data.date)), fontWeight = FontWeight.Bold)
                            Text(text = "#${data.receiptId}", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Table
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Description", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                        Text(text = "Qty", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(text = "Rate", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(text = "Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    data.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = item.description, modifier = Modifier.weight(2f))
                            Text(text = item.quantity, modifier = Modifier.weight(1f))
                            Text(text = item.rate, modifier = Modifier.weight(1f))
                            Text(text = "₹${item.amount}", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Grand Total / మొత్తం", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "₹${data.totalAmount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (data.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Notes:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = data.notes, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                    Text(text = "Authorized Signature", modifier = Modifier.align(Alignment.End), color = Color.Gray)
                }
            }
        }
    }
}
