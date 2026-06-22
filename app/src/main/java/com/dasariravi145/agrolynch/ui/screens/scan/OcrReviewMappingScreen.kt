package com.dasariravi145.agrolynch.ui.screens.scan

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillData
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillItem
import com.dasariravi145.agrolynch.util.ocr.BillValidationEngine
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.Constants
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewMappingScreen(
    viewModel: OcrReviewViewModel,
    onBack: () -> Unit,
    onConfirm: (ExtractedBillData) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRawText by remember { mutableStateOf(false) }
    var showBusinessDetails by remember { mutableStateOf(false) }

    LaunchedEffect(state.isConfirmed) {
        if (state.isConfirmed) {
            onConfirm(state.mappedData)
        }
    }

    val validation = BillValidationEngine.checkStatus(state.mappedData)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fill Farmer Arrival from Bill", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.confirm() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = validation.isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & Fill Form", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
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
            // A) Detected Header Info (Expandable Section)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(0.5.dp, Color.LightGray)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showBusinessDetails = !showBusinessDetails },
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Detected Header Info (Reference)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(if (showBusinessDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                    AnimatedVisibility(visible = showBusinessDetails) {
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReadOnlyField("Business Name", state.mappedData.businessName)
                            ReadOnlyField("Proprietor", state.mappedData.proprietorName)
                            ReadOnlyField("Mobile", state.mappedData.mobileNumbers)
                            ReadOnlyField("Original Bill No", state.mappedData.originalBillRefNo ?: "N/A")
                        }
                    }
                }
            }

            // Warnings Section
            if (validation.errors.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFFFA39E))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Missing Required Info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                        }
                        validation.errors.forEach { msg ->
                            Text("• $msg", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                        }
                    }
                }
            }

            // 1. Farmer Details
            ReviewSection("Farmer Details", Icons.Default.Person) {
                EditableField("Farmer Name *", state.mappedData.farmerName, isRequired = true) { 
                    viewModel.updateData(state.mappedData.copy(farmerName = it))
                }
                EditableField("Place / Village", state.mappedData.farmerPlace) { 
                    viewModel.updateData(state.mappedData.copy(farmerPlace = it))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Category:", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray)
                    CategorySelector(state.mappedData.category) { 
                        viewModel.updateData(state.mappedData.copy(category = it))
                    }
                }
                ReadOnlyField("Date", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(state.mappedData.date)))
            }

            // 2. Items / Products (Grade-wise entries)
            ReviewSection("Grade-wise Entries", Icons.Default.ShoppingBasket) {
                state.mappedData.items.forEachIndexed { index, item ->
                    ItemReviewCard(item) { updated ->
                        val newList = state.mappedData.items.toMutableList()
                        newList[index] = updated
                        viewModel.updateData(state.mappedData.copy(items = newList))
                    }
                }
                
                if (state.mappedData.items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("No items detected from bill table area.", color = Color.Red, fontSize = 12.sp)
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        val newList = state.mappedData.items.toMutableList()
                        newList.add(ExtractedBillItem(productName = "New Item"))
                        viewModel.updateData(state.mappedData.copy(items = newList))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Item Row")
                }
            }

            // 3. Deductions
            ReviewSection("Deductions", Icons.Default.RemoveCircleOutline) {
                DeductionRow("Commission (%)", state.mappedData.commission) { 
                    viewModel.updateData(state.mappedData.copy(commission = it))
                }
                DeductionRow("Labour / Coolie", state.mappedData.labour) { 
                    viewModel.updateData(state.mappedData.copy(labour = it))
                }
                DeductionRow("Transport", state.mappedData.transport) { 
                    viewModel.updateData(state.mappedData.copy(transport = it))
                }
                DeductionRow("Gate / Paper / Others", state.mappedData.gateOrPaper) { 
                    viewModel.updateData(state.mappedData.copy(gateOrPaper = it))
                }
                DeductionRow("Advance", state.mappedData.advance) { 
                    viewModel.updateData(state.mappedData.copy(advance = it))
                }
            }

            // 4. Calculation Summary
            CalculationSummaryCard(state.mappedData)

            // 5. Raw Text (Collapsible)
            TextButton(
                onClick = { showRawText = !showRawText },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (showRawText) "Hide Raw Text" else "Show Raw Text", fontSize = 12.sp)
            }
            
            AnimatedVisibility(visible = showRawText) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = state.mappedData.rawText,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ReviewSection(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun EditableField(label: String, value: String, isRequired: Boolean = false, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        val isError = isRequired && value.isBlank()
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isError) Color.Red else Color.LightGray
            )
        )
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value.ifEmpty { "N/A" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}

@Composable
private fun CategorySelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Fruit", "Veg", "Grain", "Other").forEach { cat ->
                DropdownMenuItem(text = { Text(cat) }, onClick = { onSelect(cat); expanded = false })
            }
        }
    }
}

@Composable
private fun ItemReviewCard(item: ExtractedBillItem, onUpdate: (ExtractedBillItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = BorderStroke(1.dp, if (item.warning != null) Color.Red.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                BasicTextField(
                    value = item.productName,
                    onValueChange = { onUpdate(item.copy(productName = it)) },
                    textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (item.productName.isEmpty()) Text("Product Name *", color = Color.LightGray, style = TextStyle(fontSize = 16.sp))
                        innerTextField()
                    }
                )
                
                var unitExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { unitExpanded = true }) {
                        Text(item.unit, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        listOf("KG", "Ton", "Boxes").forEach { u ->
                            DropdownMenuItem(text = { Text(u) }, onClick = { onUpdate(item.copy(unit = u)); unitExpanded = false })
                        }
                    }
                }
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Qty *", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = if (item.quantityKg == 0.0) "" else Formatter.formatWeight(item.quantityKg),
                        onValueChange = { onUpdate(item.copy(quantityKg = it.toDoubleOrNull() ?: 0.0, warning = null)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(4.dp),
                        isError = item.quantityKg <= 0
                    )
                }
                Column(Modifier.weight(0.8f)) {
                    val label = if (item.unit == "Boxes") "Waste%" else "Waste(KG)"
                    Text(label, fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = if (item.spoilage == 0.0) "" else Formatter.formatWeight(item.spoilage),
                        onValueChange = { onUpdate(item.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Rate/KG *", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = if (item.rate == 0.0) "" else Formatter.formatWeight(item.rate),
                        onValueChange = { onUpdate(item.copy(rate = it.toDoubleOrNull() ?: 0.0, warning = null)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(4.dp),
                        isError = item.rate <= 0
                    )
                }
            }
            
            if (item.warning != null) {
                Text(item.warning, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun DeductionRow(label: String, value: Double, onUpdate: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = if (value == 0.0) "" else Formatter.formatWeight(value),
            onValueChange = { onUpdate(it.toDoubleOrNull() ?: 0.0) },
            modifier = Modifier.width(120.dp),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun CalculationSummaryCard(data: ExtractedBillData) {
    val validated = BillValidationEngine.validate(data)
    val gross = validated.grossAmount
    val totalDed = validated.totalDeductions
    val net = validated.netAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1B5E20).copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryLine("Gross Amount", gross)
            SummaryLine("Total Deductions", -totalDed, Color.Red)
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFF1B5E20).copy(alpha = 0.1f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Farmer Net Payable", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("₹${Formatter.formatCurrency(net)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B5E20))
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: Double, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text("₹${Formatter.formatCurrency(amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
