package com.dasariravi145.agrolynch.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.util.Formatter

@Composable
fun StockReportScreen(
    viewModel: ReportViewModel,
    onBackClick: () -> Unit
) {
    val stockData by viewModel.stockReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filteredData = remember(stockData, state.searchQuery) {
        stockData.filter { it.productName.contains(state.searchQuery, ignoreCase = true) }
    }

    ReportLayout(
        title = "Inventory Stock Report",
        viewModel = viewModel,
        onBack = onBackClick,
        data = filteredData
    ) { padding ->
        if (filteredData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No stock found matching filters", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredData) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("ID: ${item.productId.takeLast(6).uppercase()}", fontSize = 11.sp, color = Color.Gray)
                                if (item.unit == "Boxes") {
                                    Text("${item.numberOfBoxes} Boxes", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val displayQty = if (item.unit == "Ton") "${Formatter.formatWeight(item.totalQuantity)} Ton" 
                                                 else if (item.unit == "Boxes") "${item.numberOfBoxes} Boxes"
                                                 else "${Formatter.formatWeight(item.totalQuantity)} KG"
                                
                                Text(
                                    displayQty,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.totalQuantity > 0) Color(0xFF16A34A) else Color.Red,
                                    fontSize = 18.sp
                                )
                                
                                if (item.unit != "KG") {
                                    Text("Net: ${Formatter.formatNetWeight(item.totalNetWeightKg)}", fontSize = 11.sp, color = Color.Gray)
                                    timber.log.Timber.d("REPORT_QTY_DISPLAY: $displayQty | Net: ${item.totalNetWeightKg} KG")
                                } else {
                                    timber.log.Timber.d("REPORT_QTY_DISPLAY: $displayQty")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
