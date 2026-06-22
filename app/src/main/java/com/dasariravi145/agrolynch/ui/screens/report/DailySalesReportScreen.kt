package com.dasariravi145.agrolynch.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.util.Formatter

@Composable
fun DailySalesReportScreen(
    viewModel: ReportViewModel,
    onBackClick: () -> Unit
) {
    val data by viewModel.buyerDetailedReport.collectAsStateWithLifecycle()
    
    ReportLayout(
        title = "Daily Sales Report",
        viewModel = viewModel,
        onBack = onBackClick,
        data = data
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val totalSales = data.sumOf { it.saleAmount }
            SummaryReportHeader("Total Sales", totalSales, "Transaction Count", data.size.toDouble())
            
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(item.buyerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatDate(item.date), fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("${item.productName} (${item.grade})", fontSize = 13.sp, color = Color.DarkGray)
                            val displayQty = if (item.inputQuantity > 0) item.inputQuantity else item.quantity
                            Text("${Formatter.formatWeight(displayQty)} ${item.unit} @ ₹${Formatter.formatCurrency(item.rate)}", fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Sale Amt: ₹${Formatter.formatCurrency(item.saleAmount)}", fontSize = 12.sp)
                                Text("₹${Formatter.formatCurrency(item.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}
