package com.dasariravi145.agrolynch.ui.screens.report

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.findActivity
import com.dasariravi145.agrolynch.data.local.dao.*
import java.text.SimpleDateFormat
import java.util.*

import android.content.Intent
import androidx.compose.ui.graphics.vector.ImageVector
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog

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
                            IconButton(onClick = { 
                                if (isPremium) viewModel.onExportClick(data) else showPremiumDialog = true 
                            }) {
                                Icon(
                                    Icons.Default.FileDownload, 
                                    contentDescription = "Export",
                                    tint = if (isPremium) MaterialTheme.colorScheme.primary else Color(0xFFFFD700)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                    
                    PeriodPresetRow(
                        selectedType = state.periodType,
                        onTypeSelected = { viewModel.onPeriodTypeSelected(it) }
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
                    ExportOptionItem("Share PDF Report", Icons.Default.Share) {
                        viewModel.shareReport(context, title, showExportOptions!!)
                    }
                    ExportOptionItem("Print PDF Report", Icons.Default.Print) {
                        viewModel.printReport(context, title, showExportOptions!!)
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
            onUpgradeClick = { showPremiumDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodPresetRow(selectedType: ReportPeriodType, onTypeSelected: (ReportPeriodType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriodType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BusinessReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val showExportOptions by viewModel.showExportOptions.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var isReportGenerated by remember { mutableStateOf(false) }

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
                title = { Text("Business Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isReportGenerated) {
                            viewModel.updateSearchQuery("")
                            isReportGenerated = false
                        } else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentData = getReportDataForCategory(viewModel, state.selectedCategory)
                        if (isPremium) viewModel.onExportClick(currentData) else showPremiumDialog = true
                    }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = if (isPremium) Color(0xFF0D47A1) else Color(0xFFFFD700)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (!isReportGenerated) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section 1: Select Report Type
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select Report Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    ReportTypeGridRedesign(
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = { viewModel.onCategorySelected(it) }
                    )
                }

                // Section 2: Select Period
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    PeriodChipsRedesign(
                        selectedPeriod = state.periodType,
                        onPeriodSelected = { viewModel.onPeriodTypeSelected(it) }
                    )
                }

                // Section 3: Dynamic Filters
                DynamicFiltersRedesign(state = state, viewModel = viewModel)

                // Section 4: Generate Button
                Button(
                    onClick = { isReportGenerated = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Report: ${state.rangeLabel}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                        TextButton(onClick = { 
                            viewModel.updateSearchQuery("")
                            isReportGenerated = false 
                        }) {
                            Text("Edit Filters", fontSize = 12.sp)
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
                    ReportContentByCategory(viewModel = viewModel, category = state.selectedCategory)
                }
            }
        }
    }

    if (showExportOptions != null) {
        val title = state.selectedCategory.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } + " Report"
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportOptions() },
            title = { Text("Export Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportOptionItem("Share PDF Report", Icons.Default.Share) {
                        viewModel.shareReport(context, title, showExportOptions!!)
                    }
                    ExportOptionItem("Print PDF Report", Icons.Default.Print) {
                        viewModel.printReport(context, title, showExportOptions!!)
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
            onUpgradeClick = { showPremiumDialog = false }
        )
    }
}

@Composable
fun ReportTypeGridRedesign(
    selectedCategory: ReportCategory,
    onCategorySelected: (ReportCategory) -> Unit
) {
    val categories = listOf(
        ReportCategory.OVERALL_BUSINESS to Pair("Overall", Icons.Default.Summarize),
        ReportCategory.FARMER to Pair("Farmer", Icons.Default.Person),
        ReportCategory.BUYER to Pair("Buyer", Icons.Default.ShoppingCart),
        ReportCategory.PRODUCT to Pair("Product", Icons.Default.Inventory),
        ReportCategory.SALES to Pair("Sales", Icons.AutoMirrored.Filled.TrendingUp),
        ReportCategory.ARRIVAL to Pair("Arrival", Icons.Default.LocalShipping),
        ReportCategory.PAYMENT to Pair("Payment", Icons.Default.Payments),
        ReportCategory.EXPENSE to Pair("Expense", Icons.Default.AccountBalanceWallet),
        ReportCategory.PENDING to Pair("Pending", Icons.Default.HourglassEmpty),
        ReportCategory.COMMISSION to Pair("Commission", Icons.Default.Percent)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = categories.chunked(3)
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (cat, info) ->
                    ReportTypeCardRedesign(
                        title = info.first,
                        icon = info.second,
                        isSelected = selectedCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill if row has fewer than 3 items
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ReportTypeCardRedesign(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0D47A1) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF16A34A),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PeriodChipsRedesign(
    selectedPeriod: ReportPeriodType,
    onPeriodSelected: (ReportPeriodType) -> Unit
) {
    val periods = listOf(
        ReportPeriodType.TODAY to "Today",
        ReportPeriodType.YESTERDAY to "Yesterday",
        ReportPeriodType.THIS_WEEK to "This Week",
        ReportPeriodType.THIS_MONTH to "This Month",
        ReportPeriodType.MONTHLY to "Monthly",
        ReportPeriodType.QUARTERLY to "Quarterly",
        ReportPeriodType.HALF_YEARLY to "Half-Yearly",
        ReportPeriodType.YEARLY to "Yearly",
        ReportPeriodType.CUSTOM_DATE to "Custom"
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        periods.forEach { (type, label) ->
            FilterChip(
                selected = selectedPeriod == type,
                onClick = { onPeriodSelected(type) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0D47A1),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF3F4F6),
                    labelColor = Color(0xFF1F2937)
                ),
                border = null,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun DynamicFiltersRedesign(state: ReportState, viewModel: ReportViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.periodType) {
            ReportPeriodType.MONTHLY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { ReportMonthPicker(selectedMonth = state.selectedMonth, onMonthSelected = { viewModel.updateSelection(month = it) }) }
                    Box(Modifier.weight(1f)) { ReportYearPicker(selectedYear = state.selectedYear, onYearSelected = { viewModel.updateSelection(year = it) }) }
                }
            }
            ReportPeriodType.QUARTERLY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { ReportQuarterPicker(selectedQuarter = state.selectedQuarter, onQuarterSelected = { viewModel.updateSelection(quarter = it) }) }
                    Box(Modifier.weight(1f)) { ReportYearPicker(selectedYear = state.selectedYear, onYearSelected = { viewModel.updateSelection(year = it) }) }
                }
            }
            ReportPeriodType.HALF_YEARLY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { ReportHalfYearPicker(selectedHalfYear = state.selectedHalfYear, onHalfYearSelected = { viewModel.updateSelection(halfYear = it) }) }
                    Box(Modifier.weight(1f)) { ReportYearPicker(selectedYear = state.selectedYear, onYearSelected = { viewModel.updateSelection(year = it) }) }
                }
            }
            ReportPeriodType.YEARLY -> {
                ReportYearPicker(selectedYear = state.selectedYear, onYearSelected = { viewModel.updateSelection(year = it) })
            }
            ReportPeriodType.CUSTOM_DATE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { DatePickerButton(label = "From", date = state.fromDate, onDateSelected = { viewModel.updateSelection(fromDate = it) }) }
                    Box(Modifier.weight(1f)) { DatePickerButton(label = "To", date = state.toDate, onDateSelected = { viewModel.updateSelection(toDate = it) }) }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ReportContentByCategory(viewModel: ReportViewModel, category: ReportCategory) {
    when (category) {
        ReportCategory.OVERALL_BUSINESS -> OverallBusinessReport(viewModel)
        ReportCategory.FARMER -> FarmerReportScreen(viewModel, {})
        ReportCategory.BUYER -> BuyerReportScreen(viewModel, {})
        ReportCategory.PRODUCT -> ProductReportScreen(viewModel, {})
        ReportCategory.SALES -> SalesReportScreen(viewModel, {})
        ReportCategory.ARRIVAL -> ArrivalReportScreen(viewModel, {})
        ReportCategory.PAYMENT -> PaymentReportScreen(viewModel, {})
        ReportCategory.EXPENSE -> ExpenseReportScreen(viewModel, {})
        ReportCategory.PENDING -> OutstandingAgingScreen(viewModel, {})
        ReportCategory.COMMISSION -> CommissionReportScreen(viewModel, {})
    }
}

fun getReportDataForCategory(viewModel: ReportViewModel, category: ReportCategory): List<Any> {
    return when (category) {
        ReportCategory.OVERALL_BUSINESS -> viewModel.summaryTotals.value.toList()
        ReportCategory.FARMER -> viewModel.farmerDetailedReport.value
        ReportCategory.BUYER -> viewModel.buyerDetailedReport.value
        ReportCategory.PRODUCT -> viewModel.productPerformanceReport.value
        ReportCategory.SALES -> viewModel.buyerDetailedReport.value
        ReportCategory.ARRIVAL -> viewModel.farmerDetailedReport.value
        ReportCategory.PAYMENT -> viewModel.paymentReport.value
        ReportCategory.EXPENSE -> viewModel.expenseReport.value
        ReportCategory.PENDING -> viewModel.outstandingAgingReport.value
        ReportCategory.COMMISSION -> viewModel.commissionReport.value
    }
}

@Composable
fun OverallBusinessReport(viewModel: ReportViewModel) {
    val totals by viewModel.summaryTotals.collectAsStateWithLifecycle()
    if (totals.isEmpty()) { EmptyReportState(); return }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Business Performance Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    listOf("Total Sales", "Total Arrivals", "Total Commission", "Total Expenses", "Farmer Payments", "Buyer Collections", "Farmer Pending", "Buyer Pending").forEach { label ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
                            Text(label, color = Color.Gray); Text("₹${Formatter.formatCurrency(totals[label] ?: 0.0)}", fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    val netProfit = totals["Net Profit/Loss"] ?: 0.0
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Net Profit/Loss", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("₹${Formatter.formatCurrency(netProfit)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (netProfit >= 0) Color(0xFF2E7D32) else Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun FarmerReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.farmerDetailedReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    if (data.isEmpty()) {
        EmptyReportState()
        return
    }

    val filtered = remember(data, state.searchQuery) { 
        data.filter { it.farmerName.contains(state.searchQuery, true) || it.productName.contains(state.searchQuery, true) } 
    }
    
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search farmer name") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        SummaryReportHeader(
            stringResource(R.string.net_payable), 
            filtered.sumOf { it.netAmount }, 
            stringResource(R.string.pending), 
            filtered.sumOf { it.pendingAmount }
        )
        
        if (filtered.isEmpty()) {
            EmptyReportState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), 
                contentPadding = PaddingValues(16.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column { Text(item.farmerName, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (item.billNumber.isNotBlank()) Text("Bill: ${item.billNumber}", fontSize = 10.sp, color = Color.Gray) }
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(8.dp)); Text("${item.productName} (${item.grade})", fontSize = 13.sp, color = Color.DarkGray)
                            Text("${Formatter.formatWeight(item.quantity)} ${item.unit} @ ₹${Formatter.formatCurrency(item.rate)}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column { Text("Gross: ₹${Formatter.formatCurrency(item.grossAmount)}", fontSize = 11.sp); Text("Comm: ₹${Formatter.formatCurrency(item.commissionAmount)}", fontSize = 11.sp, color = Color.Red) }
                                Column(horizontalAlignment = Alignment.End) { Text("Net Payable", fontSize = 10.sp, color = Color.Gray); Text("₹${Formatter.formatCurrency(item.netAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32)) }
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
    
    if (data.isEmpty()) {
        EmptyReportState()
        return
    }

    val filtered = remember(data, state.searchQuery) { 
        data.filter { it.buyerName.contains(state.searchQuery, true) || it.productName.contains(state.searchQuery, true) } 
    }
    
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search buyer name") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        SummaryReportHeader(
            stringResource(R.string.total_sales), 
            filtered.sumOf { it.saleAmount }, 
            stringResource(R.string.pending), 
            filtered.sumOf { it.pendingAmount }
        )
        
        if (filtered.isEmpty()) {
            EmptyReportState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), 
                contentPadding = PaddingValues(16.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column { Text(item.buyerName, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (item.billNumber.isNotBlank()) Text("Invoice: ${item.billNumber}", fontSize = 10.sp, color = Color.Gray) }
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(8.dp)); Text("${item.productName} (${item.grade})", fontSize = 13.sp, color = Color.DarkGray)
                            val displayQty = if (item.inputQuantity > 0) item.inputQuantity else item.quantity
                            Text("${Formatter.formatWeight(displayQty)} ${item.unit} @ ₹${Formatter.formatCurrency(item.rate)}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column { Text("Sale: ₹${Formatter.formatCurrency(item.saleAmount)}", fontSize = 11.sp); Text("Labor: ₹${Formatter.formatCurrency(item.laborCharges)}", fontSize = 11.sp) }
                                Column(horizontalAlignment = Alignment.End) { Text("Total", fontSize = 10.sp, color = Color.Gray); Text("₹${Formatter.formatCurrency(item.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0)) }
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
    val filtered = remember(data, state.searchQuery) { data.filter { it.productName.contains(state.searchQuery, true) } }
    if (filtered.isEmpty()) { EmptyReportState(); return }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filtered) { item ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(item.grade, fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { SummaryStatSmall("Arrivals", Formatter.formatWeight(item.totalArrivals)); SummaryStatSmall("Sold", Formatter.formatWeight(item.totalSold)); SummaryStatSmall("Stock", Formatter.formatWeight(item.currentStock), Color.Red) }
                    Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { SummaryStatSmall("Avg Buy", "₹${Formatter.formatCurrency(item.avgPurchaseRate)}"); SummaryStatSmall("Avg Sale", "₹${Formatter.formatCurrency(item.avgSaleRate)}"); SummaryStatSmall("Margin", "₹${Formatter.formatCurrency(item.avgSaleRate - item.avgPurchaseRate)}", Color(0xFF1565C0)) }
                }
            }
        }
    }
}

@Composable fun SalesReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) = BuyerReportScreen(viewModel, onBack)
@Composable fun ArrivalReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) = FarmerReportScreen(viewModel, onBack)
@Composable fun MonthlySalesReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) = BuyerReportScreen(viewModel, onBack)

@Composable
fun PaymentReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.paymentReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = remember(data, state.searchQuery) { data.filter { it.partyName.contains(state.searchQuery, true) } }
    if (filtered.isEmpty()) { EmptyReportState(); return }
    Column {
        SummaryReportHeader("Total Payments", filtered.sumOf { it.amount }, "Records", filtered.size.toDouble())
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(item.partyName, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("${item.partyType} | ${item.paymentMode}", fontSize = 11.sp, color = Color.Gray); Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray) }
                        Column(horizontalAlignment = Alignment.End) { Text("₹${Formatter.formatCurrency(item.amount)}", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), fontSize = 18.sp); Text("Bal: ₹${Formatter.formatCurrency(item.remainingBalance)}", fontSize = 11.sp, color = Color.Gray) }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val data by viewModel.expenseReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = remember(data, state.searchQuery) { data.filter { it.type.contains(state.searchQuery, true) || it.description.contains(state.searchQuery, true) } }
    if (filtered.isEmpty()) { EmptyReportState(); return }
    Column {
        SummaryReportHeader("Total Expenses", filtered.sumOf { it.amount }, "Records", filtered.size.toDouble())
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(item.type, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (item.description.isNotBlank()) Text(item.description, fontSize = 11.sp, color = Color.Gray); Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray) }
                        Text("₹${Formatter.formatCurrency(item.amount)}", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 18.sp)
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
    val filtered = remember(data, state.searchQuery) { data.filter { it.name.contains(state.searchQuery, true) } }
    if (filtered.isEmpty()) { EmptyReportState(); return }
    Column {
        SummaryReportHeader("Total Pending", filtered.sumOf { it.pendingAmount }, "Avg Age", filtered.map { it.daysPending }.average().takeIf { !it.isNaN() } ?: 0.0)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(item.type, fontSize = 10.sp, color = if (item.type == "BUYER") Color(0xFF1565C0) else Color(0xFFD97706)); if (item.lastPaymentDate != null) Text("Last Pmt: ${formatDate(item.lastPaymentDate)}", fontSize = 11.sp, color = Color.Gray) }
                        Column(horizontalAlignment = Alignment.End) { Text("₹${Formatter.formatCurrency(item.pendingAmount)}", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 18.sp); val ageColor = when { item.daysPending > 30 -> Color.Red; item.daysPending > 7 -> Color(0xFFD97706); else -> Color(0xFF2E7D32) }; Text("${item.daysPending} Days", fontSize = 12.sp, color = ageColor, fontWeight = FontWeight.Bold) }
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
    val filtered = remember(data, state.searchQuery) { data.filter { it.farmerName.contains(state.searchQuery, true) || it.productName.contains(state.searchQuery, true) } }
    if (filtered.isEmpty()) { EmptyReportState(); return }
    Column {
        SummaryReportHeader("Earned Comm.", filtered.sumOf { it.commissionAmount }, "Records", filtered.size.toDouble())
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text(item.farmerName, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("${item.productName} (${item.grade})", fontSize = 11.sp, color = Color.Gray) }; Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray) }
                        Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text("Gross: ₹${Formatter.formatCurrency(item.grossAmount)}", fontSize = 11.sp); Text("Comm %: ${Formatter.formatWeight(item.commissionPercent)}%", fontSize = 11.sp) }; Column(horizontalAlignment = Alignment.End) { Text("Comm. Amount", fontSize = 10.sp, color = Color.Gray); Text("₹${Formatter.formatCurrency(item.commissionAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706)) } }
                    }
                }
            }
        }
    }
}

@Composable fun EmptyReportState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp), tint = Color.Gray); Spacer(Modifier.height(8.dp)); Text("No records found for selected report period", color = Color.Gray, textAlign = TextAlign.Center) } }
@Composable fun SummaryStatSmall(label: String, value: String, color: Color = Color.Black) = Column { Text(label, fontSize = 10.sp, color = Color.Gray); Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color) }
@Composable fun SummaryReportHeader(label1: String, val1: Double, label2: String, val2: Double) = Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text(label1, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp); Text("₹${Formatter.formatCurrency(val1)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black) }; Column(horizontalAlignment = Alignment.End) { Text(label2, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp); val displayVal = if (label2.contains("Age") || label2.contains("Days")) "${Formatter.formatWeight(val2)} Days" else if (label2.contains("Records")) Formatter.formatWeight(val2) else "₹${Formatter.formatCurrency(val2)}"; Text(displayVal, color = Color(0xFFFFEB3B), fontSize = 18.sp, fontWeight = FontWeight.Black) } } }
@Composable fun ExportOptionItem(label: String, icon: ImageVector, onClick: () -> Unit) = Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Text(label, fontWeight = FontWeight.Medium) } }
@Composable fun ReportMonthPicker(selectedMonth: Int, onMonthSelected: (Int) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }) { Text(SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth - 1) }.time)) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { (1..12).forEach { m -> DropdownMenuItem(text = { Text(SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, m - 1) }.time)) }, onClick = { onMonthSelected(m); expanded = false }) } } } }
@Composable fun ReportYearPicker(selectedYear: Int, onYearSelected: (Int) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }) { Text(selectedYear.toString()) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { (2020..2030).forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = { onYearSelected(y); expanded = false }) } } } }
@Composable fun ReportQuarterPicker(selectedQuarter: Int, onQuarterSelected: (Int) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }) { Text("Q$selectedQuarter") }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { (1..4).forEach { q -> DropdownMenuItem(text = { Text("Q$q") }, onClick = { onQuarterSelected(q); expanded = false }) } } } }
@Composable fun ReportHalfYearPicker(selectedHalfYear: Int, onHalfYearSelected: (Int) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }) { Text("H$selectedHalfYear") }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { (1..2).forEach { h -> DropdownMenuItem(text = { Text("H$h") }, onClick = { onHalfYearSelected(h); expanded = false }) } } } }
@Composable fun DatePickerButton(label: String, date: Long, onDateSelected: (Long) -> Unit) { 
    val context = LocalContext.current
    OutlinedButton(onClick = { 
        val activity = context.findActivity()
        if (activity != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            android.app.DatePickerDialog(
                activity, 
                { _, y, m, d -> 
                    val res = Calendar.getInstance().apply { 
                        set(y, m, d, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0) 
                    }
                    onDateSelected(res.timeInMillis) 
                }, 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH), 
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }) { 
        Text("$label: ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(date))}") 
    } 
}
@Composable fun MonthYearPickerButton(label: String, month: Int, year: Int, onSelected: (Int, Int) -> Unit) { var showDialog by remember { mutableStateOf(false) }; OutlinedButton(onClick = { showDialog = true }) { val cal = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1); set(Calendar.YEAR, year) }; Text("$label: ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)}") }; if (showDialog) { var tempMonth by remember { mutableStateOf(month) }; var tempYear by remember { mutableStateOf(year) }; AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Select Month & Year") }, text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReportMonthPicker(selectedMonth = tempMonth, onMonthSelected = { tempMonth = it }); ReportYearPicker(selectedYear = tempYear, onYearSelected = { tempYear = it }) } }, confirmButton = { TextButton(onClick = { onSelected(tempMonth, tempYear); showDialog = false }) { Text("OK") } }) } }
fun formatDate(time: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))
