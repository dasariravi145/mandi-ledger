package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.compose.animation.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDetailScreen(
    viewModel: LedgerViewModel,
    partyId: String,
    partyType: String,
    onBack: () -> Unit
) {
    val summary by (if (partyType == "FARMER") viewModel.getFarmerLedger(partyId) else viewModel.getBuyerLedger(partyId))
        .collectAsStateWithLifecycle(initialValue = null)
    
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exportStatus) {
        if (exportStatus == "PREMIUM_REQUIRED") {
            showPremiumDialog = true
        } else if (exportStatus.startsWith("SUCCESS:")) {
            val filePath = exportStatus.removePrefix("SUCCESS:")
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Ledger PDF"))
        } else if (exportStatus.startsWith("FAILED:")) {
            snackbarHostState.showSnackbar(exportStatus.removePrefix("FAILED:"))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(summary?.partyName ?: "Ledger Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (partyType == "FARMER") "Farmer / రైతు" else "Buyer / వ్యాపారి", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { 
                            if (isPremium) {
                                summary?.let { viewModel.exportLedger(context, it, partyType) }
                            } else {
                                showPremiumDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Default.PictureAsPdf, 
                                contentDescription = "Export PDF",
                                tint = if (isPremium) MaterialTheme.colorScheme.primary else Color(0xFFFFD700)
                            )
                            if (!isPremium) {
                                Text("👑", modifier = Modifier.align(Alignment.TopEnd), fontSize = 10.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        summary?.let { s ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LedgerStatsHeader(s)
                
                // Search and Filter Row
                SearchBar(
                    query = filter.query,
                    onQueryChange = { viewModel.updateSearchQuery(it) }
                )

                if (s.entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions found matching filters.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.entries, key = { it.id }) { entry ->
                            EnhancedLedgerEntryItem(entry, partyType)
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF16A34A))
        }
    }

    if (showPremiumDialog) {
        PremiumFeatureLockedDialog(
            onDismiss = { showPremiumDialog = false },
            onUpgradeClick = { 
                showPremiumDialog = false
                // Nav handled by NavGraph typically or another event
            }
        )
    }
}

@Composable
fun LedgerStatsHeader(summary: LedgerSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)), // Dark Green
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Transactions", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(summary.totalTransactions.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Balance / బాకీ", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("₹${String.format(Locale.US, "%.2f", summary.balance)}", color = Color(0xFFFFEB3B), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStatItem("Total Amount", summary.totalDebit)
                SummaryStatItem("Payments", summary.totalCredit)
                if (summary.advanceAmount > 0) {
                    SummaryStatItem("Advance", summary.advanceAmount, Color(0xFF4FC3F7))
                }
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: Double, color: Color = Color.White) {
    Column {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 10.sp)
        Text("₹${String.format(Locale.US, "%.0f", value)}", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search product, bill # or type...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        trailingIcon = { if(query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, null) } },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF16A34A)
        ),
        singleLine = true
    )
}

@Composable
fun EnhancedLedgerEntryItem(entry: LedgerEntry, partyType: String) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransactionTypeBadge(entry.transactionType)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(entry.date)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                if (entry.details?.billNumber?.isNotEmpty() == true) {
                    Text(
                        text = "#${entry.details.billNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    entry.details?.let { details ->
                        if (details.productName.isNotEmpty()) {
                            Text(
                                text = "${details.category} | ${details.grade}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${details.quantity} ${details.unit} @ ₹${details.rate}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val color = when(entry.type) {
                        LedgerType.DEBIT -> Color(0xFFC62828) // Red
                        LedgerType.CREDIT -> Color(0xFF2E7D32) // Green
                    }
                    Text(
                        text = "${if (entry.type == LedgerType.DEBIT) "+" else "-"} ₹${String.format(Locale.US, "%.2f", entry.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                    Text(
                        text = "Bal: ₹${String.format(Locale.US, "%.0f", entry.balance)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Calculation Details / వివరాలు", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    entry.details?.let { details ->
                        if (details.productName.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text("Category / Grade", fontSize = 13.sp, color = Color.Gray)
                                Text("${details.category} / ${details.grade}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text("Quantity / Unit", fontSize = 13.sp, color = Color.Gray)
                                Text("${details.quantity} ${details.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            DetailRow("Rate", details.rate)
                            Spacer(Modifier.height(4.dp))
                        }

                        if (entry.transactionType == TransactionType.ARRIVAL) {
                            DetailRow("Gross Amount / మొత్తం", details.grossAmount)
                            DetailRow("Commission (${details.commissionPercent}%)", -details.commissionAmount, Color.Red)
                            if (details.laborCharges > 0) DetailRow("Labor / హమాలీ", -details.laborCharges, Color.Red)
                            if (details.transportCharges > 0) DetailRow("Transport / రవాణా", -details.transportCharges, Color.Red)
                            DetailRow("Net Payable / నికర మొత్తం", details.netAmount, fontWeight = FontWeight.ExtraBold)
                        } else if (entry.transactionType == TransactionType.SALE) {
                            DetailRow("Gross Amount / మొత్తం", details.grossAmount)
                            if (details.commissionAmount > 0) DetailRow("Commission / Margin", details.commissionAmount, Color(0xFF2E7D32))
                            if (details.laborCharges > 0) DetailRow("Labor / హమాలీ", details.laborCharges)
                            if (details.transportCharges > 0) DetailRow("Transport / రవాణా", details.transportCharges)
                            DetailRow("Total Collection / మొత్తం", details.netAmount, fontWeight = FontWeight.ExtraBold)
                        } else {
                            if (details.paymentMade > 0) DetailRow("Payment Amount", details.paymentMade, Color(0xFF2E7D32))
                            if (entry.reference.isNotBlank()) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Reference", fontSize = 13.sp, color = Color.Gray)
                                    Text(entry.reference, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: Double, color: Color = Color.Black, fontWeight: FontWeight = FontWeight.Normal) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(
            text = "₹${String.format(Locale.US, "%.2f", value)}",
            fontSize = 13.sp,
            color = color,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun TransactionTypeBadge(type: TransactionType) {
    val (text, color) = when(type) {
        TransactionType.ARRIVAL -> "ARRIVAL" to Color(0xFF1565C0) // Blue
        TransactionType.SALE -> "SALE" to Color(0xFFEF6C00) // Orange
        TransactionType.PAYMENT -> "PAYMENT" to Color(0xFF2E7D32) // Green
        TransactionType.ADJUSTMENT -> "ADJUST" to Color(0xFF7E57C2) // Purple
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
