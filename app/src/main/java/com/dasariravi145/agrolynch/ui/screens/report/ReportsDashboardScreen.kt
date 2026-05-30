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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.domain.model.ChartPoint
import com.dasariravi145.agrolynch.ui.components.BarChart
import com.dasariravi145.agrolynch.ui.components.LineChart

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
    val summary by viewModel.summaryTotals.collectAsStateWithLifecycle()
    val salesTrend by viewModel.salesTrend.collectAsStateWithLifecycle()
    val topProducts by viewModel.productPerformanceReport.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Reports / నివేదికలు", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 1. DASHBOARD STYLE SUMMARY
            Text("Business Overview / సారాంశం", fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricCard("Total Sales", summary["Total Sales"] ?: 0.0, Color(0xFF1B5E20))
                SummaryMetricCard("Commission", summary["Total Commission"] ?: 0.0, Color(0xFF0D47A1))
                SummaryMetricCard("Buyer Pending", summary["Buyer Pending"] ?: 0.0, Color(0xFFE65100))
                SummaryMetricCard("Farmer Pending", summary["Farmer Pending"] ?: 0.0, Color(0xFFC62828))
            }

            // 2. VISUAL REPORTING (Charts)
            Text("Performance Trends / పోకడలు", fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Sales Trend (30d)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (salesTrend.isNotEmpty()) {
                            LineChart(
                                data = salesTrend.map { ChartPoint(it.label, it.value.toFloat()) },
                                modifier = Modifier.height(100.dp)
                            )
                        } else {
                            Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No data", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Top Products", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val chartData = topProducts.take(5).map { ChartPoint(it.productName, it.totalSold.toFloat()) }
                        if (chartData.isNotEmpty()) {
                            BarChart(data = chartData, modifier = Modifier.height(100.dp))
                        } else {
                            Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No data", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // 3. DETAILED REPORTS GRID
            Text("Operational Reports / నివేదికలు", fontWeight = FontWeight.Bold, color = Color.Gray)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), "Farmer Reports", "రైతు నివేదికలు", Icons.Default.Person, Color(0xFF8B5CF6), onNavigateToFarmerReport)
                    ReportCard(Modifier.weight(1f), "Buyer Reports", "వ్యాపారి నివేదికలు", Icons.Default.Store, Color(0xFF0EA5E9), onNavigateToBuyerReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), "Product Stats", "ఉత్పత్తి నివేదికలు", Icons.Default.Inventory, Color(0xFF16A34A), onNavigateToProductPerformance)
                    ReportCard(Modifier.weight(1f), "Commission", "కమీషన్", Icons.Default.PriceCheck, Color(0xFFD97706), onNavigateToCommissionReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), "Payments", "చెల్లింపులు", Icons.Default.Payments, Color(0xFFF43F5E), onNavigateToExpenseReport)
                    ReportCard(Modifier.weight(1f), "Pending/Aging", "బకాయిలు", Icons.Default.PendingActions, Color(0xFFDC2626), onNavigateToOutstandingReport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportCard(Modifier.weight(1f), "Daily Sales", "రోజువారీ అమ్మకాలు", Icons.Default.Assignment, Color(0xFF2563EB), onNavigateToDailySalesReport)
                    ReportCard(Modifier.weight(1f), "Stock Check", "స్టాక్", Icons.Default.Warehouse, Color(0xFF475569), onNavigateToStockReport)
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
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
                "₹${String.format("%.0f", value)}",
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
    teluguTitle: String,
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
            Text(teluguTitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
