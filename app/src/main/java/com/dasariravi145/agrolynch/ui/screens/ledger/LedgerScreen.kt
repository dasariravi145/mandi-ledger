package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onSummaryClick: (String, String) -> Unit, // partyId, partyType
    onBackClick: () -> Unit
) {
    val tabIndex by viewModel.tabIndex.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val farmerSummaries by viewModel.farmerSummaries.collectAsState()
    val buyerSummaries by viewModel.buyerSummaries.collectAsState()
    val lastRestoreInfo by viewModel.lastRestoreInfo.collectAsState()
    
    val archiveState by viewModel.archiveState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.archiveMessage.collect { msg ->
            if (msg.startsWith("UNSETTLED:")) {
                // Show settled check error dialog via local state if needed or just snackbar
                snackbarHostState.showSnackbar(msg.removePrefix("UNSETTLED: "))
            } else if (msg.startsWith("SUCCESS:")) {
                snackbarHostState.showSnackbar(msg.removePrefix("SUCCESS: "))
            } else {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    if (archiveState == ArchiveState.READY_FOR_DELETE) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeletion() },
            title = { Text("Delete Live History?") },
            text = { Text("The settled history has been archived and backed up successfully. Delete only the archived live transaction history now? The Farmer or Buyer will remain available.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeletion() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete History")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeletion() }) {
                    Text("Keep Live History")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // Restore Success Banner
            if (lastRestoreInfo != null) {
                val parts = lastRestoreInfo!!.split("|")
                if (parts.size == 2) {
                    val type = parts[0]
                    val timestamp = parts[1].toLongOrNull() ?: 0L
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Data restored successfully from ${type.lowercase().replaceFirstChar { it.uppercase() }} Backup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = dateFormat.format(Date(timestamp)),
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                                )
                            }
                            IconButton(onClick = { viewModel.dismissRestoreBanner() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = filter.query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { 
                    Text(if (tabIndex == 0) "Search Farmer" else "Search Buyer") 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

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
                            onClick = { onSummaryClick(summary.partyId, type) },
                            onArchiveClick = { viewModel.prepareArchive(summary, type) }
                        )
                    }
                }
            }
        }
    }

    if (archiveState != ArchiveState.IDLE && archiveState != ArchiveState.READY_FOR_DELETE) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF16A34A))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = when(archiveState) {
                            ArchiveState.VALIDATING -> "Checking settlement..."
                            ArchiveState.SNAPSHOT_CREATING -> "Creating archive snapshot..."
                            ArchiveState.BACKING_UP -> "Performing mandatory backup..."
                            ArchiveState.DELETING -> "Deleting live history..."
                            else -> "Processing..."
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerSummaryItem(summary: LedgerSummary, onClick: () -> Unit, onArchiveClick: () -> Unit) {
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
                        Text(text = stringResource(R.string.total_amount), fontSize = 10.sp, color = Color.Gray)
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
                val isPartial = !isAdvance && amount > 0 && summary.totalCredit > 0
                val isPending = !isAdvance && amount > 0 && summary.totalCredit <= 0
                val isSettled = !isAdvance && amount <= 0

                val color = if (isAdvance) Color(0xFF1565C0) else if (amount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                
                val label = when {
                    isAdvance -> stringResource(R.string.advance)
                    isPartial -> "Partial Payment"
                    isPending -> stringResource(R.string.pending)
                    else -> stringResource(R.string.settled)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSettled && !isAdvance) {
                        IconButton(onClick = onArchiveClick) {
                            Icon(Icons.Default.Inventory, contentDescription = "Archive and Delete History", tint = Color.Gray)
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        if (amount > 0) {
                            Text(
                                text = "₹${Formatter.formatCurrency(amount)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = color
                            )
                        }
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
    }
}
