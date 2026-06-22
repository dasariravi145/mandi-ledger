package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.findActivity
import com.dasariravi145.agrolynch.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    val isPrinting by viewModel.isPrinting.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showPremiumDialog by remember { mutableStateOf(false) }
    var pendingFileForAction by remember { mutableStateOf<File?>(null) }
    
    LaunchedEffect(exportStatus) {
        if (exportStatus == "PREMIUM_REQUIRED") {
            showPremiumDialog = true
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
                        Text(summary?.partyName ?: stringResource(R.string.ledger_details), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (partyType == "FARMER") stringResource(R.string.farmer) else stringResource(R.string.buyer), fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearFilters() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Clear Filters")
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
                        Text(stringResource(R.string.no_transactions_found), color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.entries, key = { it.id }) { entry ->
                            val billNo = entry.details?.billNumber?.ifEmpty { entry.id } ?: entry.id
                            EnhancedLedgerEntryItem(
                                entry = entry, 
                                partyType = partyType,
                                isPrinting = isPrinting == billNo,
                                isSharing = isSharing == billNo,
                                onPrint = { 
                                    android.widget.Toast.makeText(context, "Preparing bill...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.printLedgerEntry(context, entry, partyType)
                                },
                                onShare = {
                                    android.widget.Toast.makeText(context, "Preparing bill...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.shareLedgerEntry(context, entry, partyType)
                                }
                            )
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
                    Text(stringResource(R.string.total_transactions), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(summary.totalTransactions.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.pending), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("₹${Formatter.formatCurrency(summary.balance)}", color = Color(0xFFFFEB3B), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStatItem(stringResource(R.string.total_amount), summary.totalDebit)
                SummaryStatItem(stringResource(R.string.payments), summary.totalCredit)
                if (summary.advanceAmount > 0) {
                    SummaryStatItem(stringResource(R.string.advance), summary.advanceAmount, Color(0xFF4FC3F7))
                }
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: Double, color: Color = Color.White) {
    Column {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 10.sp)
        Text("₹${Formatter.formatCurrency(value)}", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        placeholder = { Text(stringResource(R.string.search_ledger_hint)) },
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
fun EnhancedLedgerEntryItem(
    entry: LedgerEntry, 
    partyType: String,
    isPrinting: Boolean = false,
    isSharing: Boolean = false,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare, 
                        modifier = Modifier.size(28.dp),
                        enabled = !isSharing && !isPrinting
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = onPrint, 
                        modifier = Modifier.size(28.dp),
                        enabled = !isPrinting && !isSharing
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                if (entry.details?.billNumber?.isNotEmpty() == true) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "#${entry.details!!.billNumber}",
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
                            val hasMultipleGrades = (details.arrivalItems.size > 1)
                            
                            if (!hasMultipleGrades) {
                                val rateToDisplay = if (details.ratePerKg > 0) details.ratePerKg 
                                                   else if (details.unit == "Ton") details.rate / 1000.0 
                                                   else details.rate
                                val rateLabel = if (details.unit == "Ton" || details.unit == "Boxes" || details.ratePerKg > 0) "/ KG" 
                                               else "/ ${details.unit}"
                                
                                val qtyDisplay = Formatter.formatQuantityDisplay(details.quantity, details.unit)
                                val netWeightDisplay = if (details.unit != "KG") " (${Formatter.formatNetWeight(details.totalNetWeightKg)})" else ""

                                Text(
                                    text = "$qtyDisplay$netWeightDisplay @ ₹${Formatter.formatWeight(rateToDisplay)} $rateLabel",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                timber.log.Timber.d("ACCOUNT_BOOK_QTY_DISPLAY: $qtyDisplay$netWeightDisplay")
                            } else {
                                Text(
                                    text = "${Formatter.formatWeight(details.totalNetWeightKg)} KG | ${details.arrivalItems.size} Grades",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            timber.log.Timber.d("LEDGER_DISPLAY_UNIT: ledgerEntryId=${entry.id} displayedQuantity=${details.quantity} displayedUnit=${details.unit}")
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val color = when(entry.type) {
                        LedgerType.DEBIT -> Color(0xFFC62828) // Red
                        LedgerType.CREDIT -> Color(0xFF2E7D32) // Green
                    }
                    Text(
                        text = "${if (entry.type == LedgerType.DEBIT) "+" else "-"} ₹${Formatter.formatCurrency(entry.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                    Text(
                        text = "Bal: ₹${Formatter.formatCurrency(entry.balance)}",
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
                    
                    Text(stringResource(R.string.calculation_details), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    entry.details?.let { details ->
                        if (details.arrivalItems.size > 1 || details.saleItems.size > 1) {
                            Text("Grade Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth().background(Color(0xFFF1F8E9)).padding(8.dp)) {
                                Text("Grade", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Qty/Unit", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Rate", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Amount", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                            if (details.arrivalItems.isNotEmpty()) {
                                details.arrivalItems.forEach { item ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(item.grade, Modifier.weight(1f), fontSize = 11.sp)
                                        Text(Formatter.formatWeight(item.finalNetWeightKg), Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                        Text("₹${Formatter.formatWeight(item.ratePerKg)}", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                        Text("₹${Formatter.formatCurrency(item.grossAmount)}", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                    }
                                }
                            } else {
                                details.saleItems.forEach { item ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(item.grade, Modifier.weight(1f), fontSize = 11.sp)
                                        val displayQty = if (item.inputQuantity > 0) item.inputQuantity else item.quantitySold
                                        Text("${Formatter.formatWeight(displayQty)} ${item.unit}", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                        Text("₹${Formatter.formatWeight(item.saleRate)}", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                        Text("₹${Formatter.formatCurrency(item.saleAmount)}", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                    }
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                Text("Total", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (details.arrivalItems.isNotEmpty()) {
                                    Text(Formatter.formatWeight(details.totalNetWeightKg), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                } else {
                                    Text("${Formatter.formatWeight(details.quantity)} ${details.unit}", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                }
                                Text("-", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("₹${Formatter.formatCurrency(details.grossAmount)}", Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
                            }
                            Spacer(Modifier.height(12.dp))
                        } else if (details.productName.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.category_grade), fontSize = 13.sp, color = Color.Gray)
                                Text("${details.category} / ${details.grade}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.quantity_unit), fontSize = 13.sp, color = Color.Gray)
                                Text(Formatter.formatQuantityDisplay(details.quantity, details.unit), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (details.unit != "KG") {
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Net Weight", fontSize = 13.sp, color = Color.Gray)
                                    val netW = if (details.totalNetWeightKg > 0.0) details.totalNetWeightKg 
                                               else if (details.unit == "Ton") details.quantity * 1000.0 
                                               else details.quantity
                                    Text(Formatter.formatNetWeight(netW), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            val rateToDisplay = if (details.ratePerKg > 0) details.ratePerKg 
                                               else if (details.unit == "Ton") details.rate / 1000.0 
                                               else details.rate
                            val rateLabel = if (details.unit == "Ton" || details.unit == "Boxes" || details.ratePerKg > 0) "/ KG" 
                                           else "/ ${details.unit}"
                            
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.rate), fontSize = 13.sp, color = Color.Gray)
                                Text("${Formatter.formatAmount(rateToDisplay)} $rateLabel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            if (details.unit == "Boxes") {
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Total Gross KG", fontSize = 13.sp, color = Color.Gray)
                                    Text("${Formatter.formatWeight(details.totalGrossKg)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Empty Box Weight/Box", fontSize = 13.sp, color = Color.Gray)
                                    Text("${Formatter.formatWeight(details.emptyBoxWeightPerBox)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Total Empty Box Weight", fontSize = 13.sp, color = Color.Gray)
                                    Text("${Formatter.formatWeight(details.lessWeightKg)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                val weightAfterEmpty = (details.totalGrossKg - details.lessWeightKg).coerceAtLeast(0.0)
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Weight After Empty Boxes", fontSize = 13.sp, color = Color.Gray)
                                    Text("${Formatter.formatWeight(weightAfterEmpty)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                    Text("Spoilage (${Formatter.formatWeight(details.spoilagePercentage)}%)", fontSize = 13.sp, color = Color.Gray)
                                    Text("${Formatter.formatWeight(details.spoilageKg)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        if (entry.transactionType == TransactionType.ARRIVAL) {
                            DetailRow(stringResource(R.string.gross_amount), details.grossAmount)
                            DetailRow("${stringResource(R.string.commission)} (${details.commissionPercent}%)", -details.commissionAmount, Color.Red)
                            if (details.laborCharges > 0) DetailRow(stringResource(R.string.labor_charges), -details.laborCharges, Color.Red)
                            if (details.transportCharges > 0) DetailRow(stringResource(R.string.transport), -details.transportCharges, Color.Red)
                            if (details.packingCharges > 0) DetailRow(stringResource(R.string.packing), -details.packingCharges, Color.Red)
                            
                            details.deductions.forEach { d ->
                                val label = if (d.deductionType == "Other") d.customName else d.deductionType
                                DetailRow(label, -d.amount, Color.Red)
                            }
                            
                            DetailRow(stringResource(R.string.net_payable), details.netAmount, fontWeight = FontWeight.ExtraBold)
                        } else if (entry.transactionType == TransactionType.SALE) {
                            DetailRow(stringResource(R.string.gross_amount), details.grossAmount)
                            if (details.commissionAmount > 0) DetailRow(stringResource(R.string.commission_margin), details.commissionAmount, Color(0xFF2E7D32))
                            if (details.laborCharges > 0) DetailRow(stringResource(R.string.labor_charges), details.laborCharges)
                            if (details.transportCharges > 0) DetailRow(stringResource(R.string.transport), details.transportCharges)
                            
                            details.deductions.forEach { d ->
                                val label = if (d.deductionType == "Other") d.customName else d.deductionType
                                DetailRow(label, d.amount)
                            }

                            DetailRow(stringResource(R.string.total_collection), details.netAmount, fontWeight = FontWeight.ExtraBold)
                        } else {
                            if (details.paymentMade > 0) DetailRow(stringResource(R.string.payment_amount), details.paymentMade, Color(0xFF2E7D32))
                            if (entry.reference.isNotBlank()) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.reference), fontSize = 13.sp, color = Color.Gray)
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
            text = "₹${Formatter.formatCurrency(value)}",
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
