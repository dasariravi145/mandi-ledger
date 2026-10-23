package com.dasariravi145.agrolynch.ui.screens.marketoutlook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import com.dasariravi145.agrolynch.util.Formatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketOutlookScreen(
    viewModel: MarketOutlookViewModel,
    onBackClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProductName.collectAsStateWithLifecycle()
    val selectedVariety by viewModel.selectedVarietyName.collectAsStateWithLifecycle()
    val selectedGrade by viewModel.selectedGradeName.collectAsStateWithLifecycle()
    val selectedMarket by viewModel.selectedMarketName.collectAsStateWithLifecycle()
    val forecastResult by viewModel.forecastResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var showPremiumPaywall by remember { mutableStateOf(!isPremium) }

    if (showPremiumPaywall && !isPremium) {
        PremiumFeatureLockedDialog(
            title = "Premium AI Market Outlook",
            message = "Analyze commodity arrival trends, price signals, regional mandi price comparisons, and predictive market forecasts! Upgrade to Premium to unlock.",
            onDismiss = { showPremiumPaywall = false },
            onUpgradeClick = {
                showPremiumPaywall = false
                onUpgradeClick()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Market Outlook", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (isPremium) Color(0xFF6366F1) else Color(0xFFFF9800),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isPremium) "AI PREMIUM" else "LOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1: PRODUCT & MARKET SELECTION CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Commodity & Market",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Product & Variety Dropdowns
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expandedProd by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedProduct,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Product") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedProd = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(expanded = expandedProd, onDismissRequest = { expandedProd = false }) {
                                listOf("Mango", "Apple", "Banana", "Tomato", "Onion", "Potato").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            viewModel.onProductSelected(p)
                                            expandedProd = false
                                        }
                                    )
                                }
                            }
                        }

                        var expandedVar by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedVariety,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Variety") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedVar = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(expanded = expandedVar, onDismissRequest = { expandedVar = false }) {
                                listOf("Neelam", "Benisha", "Totapuri", "Alphonso", "Local").forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text(v) },
                                        onClick = {
                                            viewModel.onVarietySelected(v)
                                            expandedVar = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Grade & Market Dropdowns
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expandedGrade by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedGrade,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Grade") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedGrade = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(expanded = expandedGrade, onDismissRequest = { expandedGrade = false }) {
                                listOf("Grade A", "Grade B", "Grade C", "Premium").forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            viewModel.onGradeSelected(g)
                                            expandedGrade = false
                                        }
                                    )
                                }
                            }
                        }

                        var expandedMarket by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = selectedMarket,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mandi / Market") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedMarket = true }) {
                                        Icon(Icons.Default.Place, null)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(expanded = expandedMarket, onDismissRequest = { expandedMarket = false }) {
                                viewModel.availableMarkets.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            viewModel.onMarketSelected(m)
                                            expandedMarket = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Analyze Button
                    Button(
                        onClick = {
                            if (isPremium) {
                                viewModel.analyzeMarket()
                            } else {
                                showPremiumPaywall = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.QueryStats, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze Market Outlook", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // STEP 2: FORECAST RESULTS DISPLAY
            forecastResult?.let { result ->
                // Direction Badge
                val (dirColor, dirIcon, dirText) = when (result.direction) {
                    ForecastDirection.LIKELY_UP -> Triple(Color(0xFF2E7D32), Icons.Default.TrendingUp, "LIKELY UP 📈")
                    ForecastDirection.LIKELY_DOWN -> Triple(Color(0xFFC62828), Icons.Default.TrendingDown, "LIKELY DOWN 📉")
                    ForecastDirection.LIKELY_STABLE -> Triple(Color(0xFF1565C0), Icons.Default.TrendingFlat, "LIKELY STABLE ➔")
                    ForecastDirection.INSUFFICIENT_DATA -> Triple(Color.Gray, Icons.Default.Info, "INSUFFICIENT DATA ⚠")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = dirColor.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, dirColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${result.productName} — ${result.varietyName} (${result.gradeName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Market: ${result.marketName}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Surface(
                                color = dirColor,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(dirIcon, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(dirText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = dirColor.copy(alpha = 0.2f))

                        if (result.direction != ForecastDirection.INSUFFICIENT_DATA) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Reference Price (Today):", fontSize = 12.sp, color = Color.DarkGray)
                                    Text("₹${Formatter.formatCurrency(result.currentReferencePrice)}/KG", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expected Range (Tomorrow):", fontSize = 12.sp, color = Color.DarkGray)
                                    Text(
                                        "₹${Formatter.formatCurrency(result.expectedLowTomorrow)} – ₹${Formatter.formatCurrency(result.expectedHighTomorrow)}/KG",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = dirColor
                                    )
                                }
                            }

                            // Confidence Score Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Forecast Confidence Score:", fontSize = 11.sp, color = Color.Gray)
                                    Text("${result.confidencePercent}% (${result.dataQuality} Data Quality)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { result.confidencePercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = dirColor,
                                    trackColor = Color.LightGray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                // Best Selling Window Advice
                if (result.bestSellingWindowAdvice.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Selling Window Suggestion:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text(result.bestSellingWindowAdvice, fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                // Observed Market Signals ("Why?")
                if (result.observedSignals.isNotEmpty()) {
                    Text("Observed Market Signals:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            result.observedSignals.forEach { signal ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(signal, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // AI Explanation Card
                if (result.aiExplanation.isNotBlank()) {
                    Text("AI Market Summary:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(result.aiExplanation, fontSize = 12.sp, color = Color(0xFF312E81))
                        }
                    }
                }

                // Price History Trend Chart
                if (result.priceHistory.size >= 2) {
                    Text("Price History Trend:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            PriceTrendCanvasChart(points = result.priceHistory)
                        }
                    }
                }

                // Nearby Regional Markets Table
                if (result.nearbyMarkets.isNotEmpty()) {
                    Text("Nearby Regional Mandis:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(6.dp)) {
                                Text("Mandi Market", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Distance", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Modal Rate", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Trend", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            result.nearbyMarkets.forEach { nearby ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(nearby.marketName, Modifier.weight(1.5f), fontSize = 11.sp)
                                    Text("${nearby.distanceKm} km", Modifier.weight(1f), fontSize = 11.sp, color = Color.Gray)
                                    Text("₹${Formatter.formatCurrency(nearby.currentModalPrice)}/kg", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = nearby.trend,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (nearby.trend == "UP") Color(0xFF2E7D32) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Daily Forecast Horizon Table
                if (result.dailyForecasts.isNotEmpty()) {
                    Text("Short-term Forecast Horizon:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(6.dp)) {
                                Text("Day", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Expected Range", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Signal", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            result.dailyForecasts.forEach { point ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(point.dayLabel, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("₹${Formatter.formatCurrency(point.expectedLow)} – ₹${Formatter.formatCurrency(point.expectedHigh)}", Modifier.weight(1.5f), fontSize = 11.sp)
                                    Text(
                                        text = point.direction.name.replace("LIKELY_", ""),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (point.direction == ForecastDirection.LIKELY_UP) Color(0xFF2E7D32) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Data Source & Freshness Footer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Data Source: ${result.dataSourceName}", fontSize = 10.sp, color = Color.Gray)
                            if (result.isCachedData) {
                                Text("Cached Outlook", fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Last Updated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(result.lastUpdatedTimestamp))}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(result.limitationsDisclaimer, fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PriceTrendCanvasChart(points: List<MarketPricePoint>) {
    val minVal = (points.minOfOrNull { it.modalRate } ?: 0.0) * 0.9
    val maxVal = (points.maxOfOrNull { it.modalRate } ?: 100.0) * 1.1

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = points.size

            if (count < 2) return@Canvas

            val dx = width / (count - 1)
            val path = Path()

            points.forEachIndexed { i, point ->
                val x = i * dx
                val normalizedY = (point.modalRate - minVal) / (maxVal - minVal)
                val y = height - (normalizedY * height).toFloat()

                if (i == 0) path.moveTo(x, y)
                else path.lineTo(x, y)

                drawCircle(color = Color(0xFF6366F1), radius = 4.dp.toPx(), center = Offset(x, y))
            }

            drawPath(
                path = path,
                color = Color(0xFF6366F1),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            Text(sdf.format(Date(points.first().date)), fontSize = 10.sp, color = Color.Gray)
            Text(sdf.format(Date(points.last().date)), fontSize = 10.sp, color = Color.Gray)
        }
    }
}
