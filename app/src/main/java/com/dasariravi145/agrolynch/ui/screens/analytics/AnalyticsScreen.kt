package com.dasariravi145.agrolynch.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.ui.components.BarChart
import com.dasariravi145.agrolynch.ui.components.LineChart
import com.dasariravi145.agrolynch.ui.components.PieChart
import com.dasariravi145.agrolynch.domain.model.TopEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.business_analytics)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Sales Trend
                AnalyticsCard(title = stringResource(R.string.sales_trend_7d)) {
                    LineChart(data = summary!!.salesTrend, modifier = Modifier.fillMaxWidth())
                }

                // 2. Product Distribution
                AnalyticsCard(title = stringResource(R.string.top_products)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PieChart(data = summary!!.productDistribution, modifier = Modifier.weight(1f))
                        Column(modifier = Modifier.weight(1f)) {
                            summary!!.productDistribution.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).padding(2.dp).graphicsLayer {  }.then(Modifier.background(Color(item.color))))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = item.label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Top Farmers
                AnalyticsCard(title = stringResource(R.string.top_farmers)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary!!.topFarmers.forEach { farmer ->
                            TopEntityRow(entity = farmer)
                        }
                    }
                }

                // 4. Top Buyers
                AnalyticsCard(title = stringResource(R.string.top_buyers)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary!!.topBuyers.forEach { buyer ->
                            TopEntityRow(entity = buyer)
                        }
                    }
                }

                // 5. Profit Trend
                AnalyticsCard(title = stringResource(R.string.estimated_profit_trend)) {
                    BarChart(data = summary!!.profitTrend, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun TopEntityRow(entity: TopEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = entity.name, fontWeight = FontWeight.Medium)
            Text(text = "${entity.transactionCount} entries", fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = "₹${Formatter.formatCurrency(entity.totalValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
