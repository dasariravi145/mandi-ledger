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
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
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
                title = { Text(stringResource(R.string.account_book)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    text = { Text(stringResource(R.string.farmers)) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text(stringResource(R.string.traders)) }
                )
            }

            val summaries = if (tabIndex == 0) farmerSummaries else buyerSummaries
            val type = if (tabIndex == 0) "FARMER" else "BUYER"

            if (summaries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_accounts_found))
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
                        val stockLabel = if (summary.advanceAmount > 0) stringResource(R.string.advance) else stringResource(R.string.total_amount)
                        Text(text = stockLabel, fontSize = 10.sp, color = Color.Gray)
                        Text(text = "₹${Formatter.formatCurrency(summary.totalDebit)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(text = stringResource(R.string.paid), fontSize = 10.sp, color = Color.Gray)
                        Text(text = "₹${Formatter.formatCurrency(summary.totalCredit)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val isAdvance = summary.advanceAmount > 0
                val amount = if (isAdvance) summary.advanceAmount else summary.balance
                val color = if (isAdvance) Color(0xFF1565C0) else if (amount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                val label = if (isAdvance) stringResource(R.string.advance) else if (amount > 0) stringResource(R.string.pending) else stringResource(R.string.settled)

                Text(
                    text = "₹${Formatter.formatCurrency(amount)}",
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
