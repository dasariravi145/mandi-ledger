package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.LedgerSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onSummaryClick: (String, String) -> Unit, // partyId, partyType
    onBackClick: () -> Unit
) {
    val tabIndex by viewModel.tabIndex.collectAsState()
    val farmerSummaries by viewModel.farmerSummaries.collectAsState()
    val buyerSummaries by viewModel.buyerSummaries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ledger / ఖాతా పుస్తకం") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Farmers / రైతులు") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("Buyers / వ్యాపారులు") }
                )
            }

            val summaries = if (tabIndex == 0) farmerSummaries else buyerSummaries
            val type = if (tabIndex == 0) "FARMER" else "BUYER"

            if (summaries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No accounts found / ఖాతాలు లేవు")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(summaries) { summary ->
                        LedgerSummaryItem(
                            summary = summary,
                            onClick = { onSummaryClick(summary.partyId, type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerSummaryItem(summary: LedgerSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.partyName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Column {
                        Text(text = "Arrivals", fontSize = 10.sp, color = Color.Gray)
                        Text(text = "₹${String.format("%.0f", summary.totalDebit)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(text = "Paid", fontSize = 10.sp, color = Color.Gray)
                        Text(text = "₹${String.format("%.0f", summary.totalCredit)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val isAdvance = summary.advanceAmount > 0
                val amount = if (isAdvance) summary.advanceAmount else summary.balance
                val color = if (isAdvance) Color(0xFF1565C0) else if (amount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                val label = if (isAdvance) "Advance / అడ్వాన్స్" else if (amount > 0) "Pending / బాకీ" else "Settled / చెల్లించబడింది"

                Text(
                    text = "₹${String.format("%.2f", amount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
