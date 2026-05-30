package com.dasariravi145.agrolynch.ui.screens.report

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.data.local.dao.*
import java.text.SimpleDateFormat
import java.util.*

import android.content.Intent
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import java.io.File

import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLayout(
    title: String,
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    data: List<Any>,
    content: @Composable (PaddingValues) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val showExportOptions by viewModel.showExportOptions.collectAsStateWithLifecycle()
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
                type = context.contentResolver.getType(uri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } else if (exportStatus.startsWith("FAILED:")) {
            snackbarHostState.showSnackbar(exportStatus.removePrefix("FAILED:"))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(shadowElevation = 4.dp, color = Color.White) {
                Column {
                    TopAppBar(
                        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { 
                                    if (isPremium) viewModel.onExportClick(data) else showPremiumDialog = true 
                                }) {
                                    Icon(
                                        Icons.Default.FileDownload, 
                                        contentDescription = "Export",
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
                    
                    DatePresetRow(
                        selectedPreset = state.datePreset,
                        onPresetSelected = { viewModel.setDatePreset(it) }
                    )
                    
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search name, product or bill #") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = { if(state.searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Close, null) } },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        content(padding)
    }

    if (showExportOptions != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportOptions() },
            title = { Text("Export Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportOptionItem("PDF Document", Icons.Default.PictureAsPdf) {
                        viewModel.exportReport(context, ExportFormat.PDF, title.replace(" ", "_"), showExportOptions!!)
                    }
                    ExportOptionItem("Excel Worksheet", Icons.Default.TableChart) {
                        viewModel.exportReport(context, ExportFormat.EXCEL, title.replace(" ", "_"), showExportOptions!!)
                    }
                    ExportOptionItem("CSV File", Icons.Default.TextSnippet) {
                        viewModel.exportReport(context, ExportFormat.CSV, title.replace(" ", "_"), showExportOptions!!)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissExportOptions() }) { Text("Cancel") }
            }
        )
    }

    if (showPremiumDialog) {
        PremiumFeatureLockedDialog(
            onDismiss = { showPremiumDialog = false },
            onUpgradeClick = { 
                showPremiumDialog = false
                // Logic to navigate to premium screen if needed, 
                // but usually handled by common component
            }
        )
    }
}

@Composable
fun ExportOptionItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DatePresetRow(selectedPreset: DatePreset, onPresetSelected: (DatePreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatePreset.entries.filter { it != DatePreset.CUSTOM }.forEach { preset ->
            FilterChip(
                selected = selectedPreset == preset,
                onClick = { onPresetSelected(preset) },
                label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
fun FarmerReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.farmerDetailedReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { it.farmerName.contains(state.searchQuery, true) || it.productName.contains(state.searchQuery, true) }
    }

    ReportLayout(
        title = "Farmer Transaction Report",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val totalVal = filtered.sumOf { it.netAmount }
            val totalPending = filtered.sumOf { it.pendingAmount }
            SummaryReportHeader("Net Amount", totalVal, "Total Pending", totalPending)
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(item.farmerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("${item.productName} (${item.grade})", fontSize = 13.sp, color = Color.DarkGray)
                            Text("${item.quantity} ${item.unit} @ ₹${item.rate}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column {
                                    Text("Gross: ₹${String.format("%.0f", item.grossAmount)}", fontSize = 11.sp)
                                    Text("Comm: ₹${String.format("%.0f", item.commissionAmount)} (${item.commissionPercent}%)", fontSize = 11.sp, color = Color.Red)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Payable", fontSize = 10.sp, color = Color.Gray)
                                    Text("₹${String.format("%.2f", item.netAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.buyerDetailedReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { it.buyerName.contains(state.searchQuery, true) || it.productName.contains(state.searchQuery, true) }
    }

    ReportLayout(
        title = "Buyer Sales Report",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val totalSales = filtered.sumOf { it.saleAmount }
            val totalPending = filtered.sumOf { it.pendingAmount }
            SummaryReportHeader("Total Sales", totalSales, "Collection Due", totalPending)
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(item.buyerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("${item.productName} (${item.grade})", fontSize = 13.sp, color = Color.DarkGray)
                            Text("${item.quantity} ${item.unit} @ ₹${item.rate}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column {
                                    Text("Labor: ₹${String.format("%.0f", item.laborCharges)}", fontSize = 11.sp)
                                    Text("Transport: ₹${String.format("%.0f", item.transportCharges)}", fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Amount", fontSize = 10.sp, color = Color.Gray)
                                    Text("₹${String.format("%.2f", item.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.productPerformanceReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { it.productName.contains(state.searchQuery, true) }
    }

    ReportLayout(
        title = "Product Performance",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(item.grade, fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            SummaryStatSmall("Arrivals", "${item.totalArrivals}")
                            SummaryStatSmall("Sold", "${item.totalSold}")
                            SummaryStatSmall("Stock", "${item.currentStock}", Color.Red)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            SummaryStatSmall("Avg Buy", "₹${String.format("%.1f", item.avgPurchaseRate)}")
                            SummaryStatSmall("Avg Sale", "₹${String.format("%.1f", item.avgSaleRate)}")
                            val profit = item.avgSaleRate - item.avgPurchaseRate
                            SummaryStatSmall("Margin", "₹${String.format("%.1f", profit)}", Color(0xFF1565C0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutstandingAgingScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.outstandingAgingReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { it.name.contains(state.searchQuery, true) }
    }

    ReportLayout(
        title = "Outstanding & Aging",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val totalPending = filtered.sumOf { it.pendingAmount }
            SummaryReportHeader("Total Pending", totalPending, "Average Age", filtered.map { it.daysPending }.average().takeIf { !it.isNaN() } ?: 0.0)
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(item.type, fontSize = 10.sp, color = if (item.type == "BUYER") Color(0xFF1565C0) else Color(0xFFD97706))
                                if (item.lastPaymentDate != null) {
                                    Text("Last Pmt: ${formatDate(item.lastPaymentDate)}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${String.format("%.0f", item.pendingAmount)}", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 18.sp)
                                val ageColor = when {
                                    item.daysPending > 30 -> Color.Red
                                    item.daysPending > 7 -> Color(0xFFD97706)
                                    else -> Color(0xFF2E7D32)
                                }
                                Text("${item.daysPending} days old", fontSize = 12.sp, color = ageColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommissionReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.commissionReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { 
            it.buyerName.contains(state.searchQuery, true) || 
            it.farmerName.contains(state.searchQuery, true) ||
            it.productName.contains(state.searchQuery, true)
        }
    }

    ReportLayout(
        title = "Commission Earnings",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val totalCommission = filtered.sumOf { it.commissionAmount + it.marginAmount }
            SummaryReportHeader("Total Earnings", totalCommission, "Records", filtered.size.toDouble())
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Commission Earned", fontSize = 12.sp, color = Color.Gray)
                                    Text("₹${String.format("%.2f", item.commissionAmount + item.marginAmount)}", fontWeight = FontWeight.Black, color = Color(0xFFD97706), fontSize = 20.sp)
                                }
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    LabelValueText("Buyer", item.buyerName)
                                    Spacer(Modifier.height(8.dp))
                                    LabelValueText("Farmer", item.farmerName)
                                }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    LabelValueText("Product", "${item.productName} (${item.grade})", Alignment.End)
                                    Spacer(Modifier.height(8.dp))
                                    LabelValueText("Qty", "${item.quantity} KG", Alignment.End)
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sale Amt: ₹${String.format("%.0f", item.saleAmount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Comm: ${item.commissionPercent}% (₹${String.format("%.0f", item.commissionAmount)})", fontSize = 11.sp, color = Color(0xFF16A34A))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelValueText(label: String, value: String, horizontalAlignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PaymentReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.paymentReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val filtered = remember(data, state.searchQuery) {
        data.filter { it.partyName.contains(state.searchQuery, true) }
    }

    ReportLayout(
        title = "Payment History Report",
        viewModel = viewModel,
        onBack = onBack,
        data = filtered
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val total = filtered.sumOf { it.amount }
            SummaryReportHeader("Total Payments", total, "Records", filtered.size.toDouble())
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.partyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${item.partyType} | ${item.paymentMode}", fontSize = 11.sp, color = Color.Gray)
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${String.format("%.0f", item.amount)}", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), fontSize = 18.sp)
                                Text("Bal: ₹${String.format("%.0f", item.remainingBalance)}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySalesReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    BuyerReportScreen(viewModel, onBack)
}

@Composable
fun ExpenseReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    PaymentReportScreen(viewModel, onBack)
}

@Composable
fun SummaryStatSmall(label: String, value: String, color: Color = Color.Black) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SummaryReportHeader(label1: String, val1: Double, label2: String, val2: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
            Column {
                Text(label1, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("₹${String.format("%.0f", val1)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(label2, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                val displayVal = if (label2.contains("Age")) String.format("%.0f Days", val2) 
                                 else if (label2.contains("Records") || label2.contains("Count")) String.format("%.0f", val2)
                                 else "₹${String.format("%.0f", val2)}"
                Text(displayVal, color = Color(0xFFFFEB3B), fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

fun formatDate(time: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))
