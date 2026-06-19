package com.dasariravi145.agrolynch.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.screens.arrival.ArrivalViewModel
import com.dasariravi145.agrolynch.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerReviewScreen(
    result: ScannedBillResult,
    arrivalViewModel: ArrivalViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var scannedData by remember { mutableStateOf(result) }
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Scanned Bill", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val gradeEntries = scannedData.products.map { product ->
                            ArrivalViewModel.GradeEntry(
                                grade = product.gradeName,
                                quantity = product.grossQty / 1000.0, // Assuming Ton mode in backend for batch? Or KG?
                                rate = product.rate,
                                spoilage = product.damageQty,
                                unit = "Ton" // We'll use Ton as the default for conversion if requested
                            )
                        }
                        arrivalViewModel.saveArrivalBatch(
                            context = context,
                            farmerName = scannedData.farmerName,
                            farmerPhone = scannedData.phoneNumber,
                            farmerVillage = scannedData.village,
                            productName = "Mango", // Default for Mandi
                            productCategory = "Fruit",
                            unit = "Ton",
                            commissionPercent = (scannedData.commission / (scannedData.grandTotal + scannedData.commission) * 100), // Simple estimate
                            laborCharges = scannedData.cooli,
                            transportCharges = scannedData.transport,
                            packingCharges = scannedData.paper,
                            otherDeductionsUnused = scannedData.gate,
                            gradeEntries = gradeEntries
                        )
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirm & Create Arrivals", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Rescan")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Header Details", fontWeight = FontWeight.Bold, color = Color.Gray)
            
            ConfidenceTextField(
                label = "Farmer Name",
                value = scannedData.farmerName,
                confidence = scannedData.confidenceScores["farmerName"] ?: 100,
                onValueChange = { scannedData = scannedData.copy(farmerName = it) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfidenceTextField(
                    label = "Bill Number",
                    value = scannedData.billNumber,
                    confidence = scannedData.confidenceScores["billNumber"] ?: 100,
                    modifier = Modifier.weight(1f),
                    onValueChange = { scannedData = scannedData.copy(billNumber = it) }
                )
                ConfidenceTextField(
                    label = "Date",
                    value = scannedData.date,
                    confidence = scannedData.confidenceScores["date"] ?: 100,
                    modifier = Modifier.weight(1f),
                    onValueChange = { scannedData = scannedData.copy(date = it) }
                )
            }

            Text("Product Details", fontWeight = FontWeight.Bold, color = Color.Gray)
            scannedData.products.forEachIndexed { index, product ->
                ProductReviewCard(
                    product = product,
                    onUpdate = { updated ->
                        val newProducts = scannedData.products.toMutableList()
                        newProducts[index] = updated
                        scannedData = scannedData.copy(products = newProducts)
                    }
                )
            }

            Text("Charges & Deductions", fontWeight = FontWeight.Bold, color = Color.Gray)
            ChargesSection(
                scannedData = scannedData,
                onUpdate = { scannedData = it }
            )
            
            if (scannedData.confidenceScores.values.any { it < 70 }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(Modifier.width(8.dp))
                        Text("Unable to verify handwriting. Please review manually.", color = Color(0xFFE65100), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceTextField(
    label: String,
    value: String,
    confidence: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val isLowConfidence = confidence < 70
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label ($confidence%)") },
        modifier = modifier.fillMaxWidth(),
        isError = isLowConfidence,
        colors = if (isLowConfidence) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Red,
                errorBorderColor = Color.Red
            )
        } else OutlinedTextFieldDefaults.colors()
    )
}

@Composable
fun ProductReviewCard(
    product: DetectedProduct,
    onUpdate: (DetectedProduct) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(product.gradeName, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("Confidence: ${product.confidence}%", fontSize = 11.sp, color = if (product.confidence < 70) Color.Red else Color.Gray)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = product.grossQty.toString(),
                    onValueChange = { onUpdate(product.copy(grossQty = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Gross") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = product.rate.toString(),
                    onValueChange = { onUpdate(product.copy(rate = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Rate") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = product.amount.toString(),
                    onValueChange = { onUpdate(product.copy(amount = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ChargesSection(
    scannedData: ScannedBillResult,
    onUpdate: (ScannedBillResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChargeRow("Commission", scannedData.commission) { onUpdate(scannedData.copy(commission = it)) }
            ChargeRow("Transport", scannedData.transport) { onUpdate(scannedData.copy(transport = it)) }
            ChargeRow("Cooli/Labour", scannedData.cooli) { onUpdate(scannedData.copy(cooli = it)) }
            ChargeRow("Advance", scannedData.advance) { onUpdate(scannedData.copy(advance = it)) }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Grand Total", fontWeight = FontWeight.Black)
                Text("₹${Formatter.formatCurrency(scannedData.grandTotal)}", fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
            }
        }
    }
}

@Composable
fun ChargeRow(label: String, value: Double, onValueChange: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onValueChange(it.toDoubleOrNull() ?: 0.0) },
            modifier = Modifier.width(100.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}
