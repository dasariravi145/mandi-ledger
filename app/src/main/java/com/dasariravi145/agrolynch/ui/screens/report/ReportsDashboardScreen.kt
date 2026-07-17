package com.dasariravi145.agrolynch.ui.screens.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.domain.model.ChartPoint
import com.dasariravi145.agrolynch.ui.components.BarChart
import com.dasariravi145.agrolynch.ui.components.LineChart
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDashboardScreen(
    viewModel: ReportViewModel,
    onBackClick: () -> Unit,
    onNavigateToStockReport: () -> Unit,
    onNavigateToDailySalesReport: () -> Unit,
    onNavigateToMonthlySalesReport: () -> Unit,
    onNavigateToCommissionReport: () -> Unit,
    onNavigateToFarmerReport: () -> Unit,
    onNavigateToBuyerReport: () -> Unit,
    onNavigateToExpenseReport: () -> Unit,
    onNavigateToOutstandingReport: () -> Unit,
    onNavigateToProductPerformance: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary by viewModel.summaryTotals.collectAsStateWithLifecycle()
    val salesTrend by viewModel.salesTrend.collectAsStateWithLifecycle()
    val topProducts by viewModel.productPerformanceReport.collectAsStateWithLifecycle()
    
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.reports), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(state.rangeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.business_overview), fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricCard(stringResource(R.string.total_sales), summary["Total Sales"] ?: 0.0, Color(0xFF1B5E20))
                SummaryMetricCard(stringResource(R.string.commission), summary["Total Commission"] ?: 0.0, Color(0xFF0D47A1))
                SummaryMetricCard(stringResource(R.string.buyer_balance), summary["Buyer Pending"] ?: 0.0, Color(0xFFE65100))
                SummaryMetricCard(stringResource(R.string.farmer_balance), summary["Farmer Pending"] ?: 0.0, Color(0xFFC62828))
            }

            Text(stringResource(R.string.performance_trends), fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.sales_trend_30d), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (salesTrend.isNotEmpty()) {
                            LineChart(
                                data = salesTrend.map { ChartPoint(it.label, it.value.toFloat()) },
                                modifier = Modifier.height(100.dp)
                            )
                        } else {
                            Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_data), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.top_products), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val chartData = topProducts.take(5).map { ChartPoint(it.productName, it.totalSold.toFloat()) }
                        if (chartData.isNotEmpty()) {
                            BarChart(data = chartData, modifier = Modifier.height(100.dp))
                        } else {
                            Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_data), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.operational_reports), fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), stringResource(R.string.farmer_reports), Icons.Default.Person, Color(0xFF8B5CF6), onNavigateToFarmerReport)
                    ReportCard(Modifier.weight(1f), stringResource(R.string.buyer_reports), Icons.Default.Store, Color(0xFF0EA5E9), onNavigateToBuyerReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), stringResource(R.string.product_stats), Icons.Default.Inventory, Color(0xFF16A34A), onNavigateToProductPerformance)
                    ReportCard(Modifier.weight(1f), stringResource(R.string.commission), Icons.Default.PriceCheck, Color(0xFFD97706), onNavigateToCommissionReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), stringResource(R.string.payments), Icons.Default.Payments, Color(0xFFF43F5E), onNavigateToExpenseReport)
                    ReportCard(Modifier.weight(1f), stringResource(R.string.pending_aging), Icons.Default.PendingActions, Color(0xFFDC2626), onNavigateToOutstandingReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), stringResource(R.string.daily_sales), Icons.Default.Assignment, Color(0xFF2563EB), onNavigateToDailySalesReport)
                    ReportCard(Modifier.weight(1f), stringResource(R.string.stock_check), Icons.Default.Warehouse, Color(0xFF475569), onNavigateToStockReport)
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showFilterDialog) {
        ReportFilterDialog(
            state = state,
            onPeriodTypeChange = viewModel::setPeriodType,
            onSelectionUpdate = { m, y, q, h, fm, fy, tm, ty ->
                viewModel.updateSelection(
                    month = m, year = y, quarter = q, halfYear = h,
                    fromMonth = fm, fromYear = fy, toMonth = tm, toYear = ty
                )
            },
            onCustomDateRangeUpdate = viewModel::updateCustomDateRange,
            onDismiss = { showFilterDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportFilterDialog(
    state: ReportState,
    onPeriodTypeChange: (ReportPeriodType) -> Unit,
    onSelectionUpdate: (month: Int?, year: Int?, quarter: Int?, halfYear: Int?, fromMonth: Int?, fromYear: Int?, toMonth: Int?, toYear: Int?) -> Unit,
    onCustomDateRangeUpdate: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Report Period") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select Period Type", style = MaterialTheme.typography.labelMedium)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriodType.values().forEach { type ->
                        FilterChip(
                            selected = state.periodType == type,
                            onClick = { onPeriodTypeChange(type) },
                            label = { Text(type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                HorizontalDivider()

                when (state.periodType) {
                    ReportPeriodType.MONTHLY -> {
                        MonthYearPicker(
                            month = state.selectedMonth,
                            year = state.selectedYear,
                            onUpdate = { m, y -> onSelectionUpdate(m, y, null, null, null, null, null, null) }
                        )
                    }
                    ReportPeriodType.QUARTERLY -> {
                        QuarterYearPicker(
                            quarter = state.selectedQuarter,
                            year = state.selectedYear,
                            onUpdate = { q, y -> onSelectionUpdate(null, y, q, null, null, null, null, null) }
                        )
                    }
                    ReportPeriodType.HALF_YEARLY -> {
                        HalfYearYearPicker(
                            halfYear = state.selectedHalfYear,
                            year = state.selectedYear,
                            onUpdate = { h, y -> onSelectionUpdate(null, y, null, h, null, null, null, null) }
                        )
                    }
                    ReportPeriodType.YEARLY -> {
                        YearPicker(
                            year = state.selectedYear,
                            onUpdate = { y -> onSelectionUpdate(null, y, null, null, null, null, null, null) }
                        )
                    }
                    ReportPeriodType.CUSTOM_DATE -> {
                        CustomDatePicker(
                            startDate = state.startDate,
                            endDate = state.endDate,
                            onUpdate = onCustomDateRangeUpdate
                        )
                    }
                    ReportPeriodType.CUSTOM_MONTH -> {
                        CustomMonthRangePicker(
                            fromMonth = state.fromMonth,
                            fromYear = state.fromYear,
                            toMonth = state.toMonth,
                            toYear = state.toYear,
                            onUpdate = { fm, fy, tm, ty -> onSelectionUpdate(null, null, null, null, fm, fy, tm, ty) }
                        )
                    }
                    else -> {
                        Text("Current Selection: ${state.rangeLabel}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Apply") }
        }
    )
}

@Composable
fun MonthYearPicker(month: Int, year: Int, onUpdate: (Int, Int) -> Unit) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var expandedMonth by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expandedMonth = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (month in 1..12) months[month-1] else months[0])
                }
                DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                    months.forEachIndexed { index, name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { onUpdate(index + 1, year); expandedMonth = false })
                    }
                }
            }
            YearPicker(year, onUpdate = { onUpdate(month, it) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun QuarterYearPicker(quarter: Int, year: Int, onUpdate: (Int, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..4).forEach { q ->
                    FilterChip(
                        selected = quarter == q,
                        onClick = { onUpdate(q, year) },
                        label = { Text("Q$q") }
                    )
                }
            }
            YearPicker(year, onUpdate = { onUpdate(quarter, it) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HalfYearYearPicker(halfYear: Int, year: Int, onUpdate: (Int, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..2).forEach { h ->
                    FilterChip(
                        selected = halfYear == h,
                        onClick = { onUpdate(h, year) },
                        label = { Text("H$h") }
                    )
                }
            }
            YearPicker(year, onUpdate = { onUpdate(halfYear, it) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun YearPicker(year: Int, onUpdate: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 5..currentYear + 2).toList()
    
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(year.toString())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { y ->
                DropdownMenuItem(text = { Text(y.toString()) }, onClick = { onUpdate(y); expanded = false })
            }
        }
    }
}

@Composable
fun CustomDatePicker(startDate: Long, endDate: Long, onUpdate: (Long, Long) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    showDatePicker(context, startDate) { onUpdate(it, endDate) }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(startDate)))
            }
            Text("to", modifier = Modifier.align(Alignment.CenterVertically))
            OutlinedButton(
                onClick = {
                    showDatePicker(context, endDate) { onUpdate(startDate, it) }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(endDate)))
            }
        }
    }
}

@Composable
fun CustomMonthRangePicker(fromMonth: Int, fromYear: Int, toMonth: Int, toYear: Int, onUpdate: (Int, Int, Int, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("From", style = MaterialTheme.typography.labelSmall)
        MonthYearPicker(fromMonth, fromYear) { m, y -> onUpdate(m, y, toMonth, toYear) }
        Text("To", style = MaterialTheme.typography.labelSmall)
        MonthYearPicker(toMonth, toYear) { m, y -> onUpdate(fromMonth, fromYear, m, y) }
    }
}

fun showDatePicker(context: android.content.Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val result = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
            }
            onDateSelected(result.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun SummaryMetricCard(label: String, value: Double, color: Color) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(
                "₹${Formatter.formatCurrency(value)}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ReportCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
