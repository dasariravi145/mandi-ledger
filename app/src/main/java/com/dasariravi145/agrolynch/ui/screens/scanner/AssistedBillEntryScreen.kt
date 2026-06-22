package com.dasariravi145.agrolynch.ui.screens.scanner

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.util.Formatter

enum class AssistedStep(val label: String) {
    FARMER_NAME("Farmer Name"),
    VILLAGE("Village / Place"),
    PRODUCT_NAME("Product Name"),
    GRADE("Grade"),
    UNIT("Unit (KG/Ton/Boxes)"),
    QUANTITY("Quantity"),
    SPOILAGE("Waste / Spoilage"),
    RATE("Rate"),
    ADD_ANOTHER("Add Another Grade?"),
    COMMISSION("Commission %"),
    LABOUR("Labour / Coolie"),
    TRANSPORT("Transport"),
    OTHERS("Gate / Paper / Others"),
    ADVANCE("Advance")
}

data class AssistedItem(
    val product: String = "",
    val grade: String = "Grade A",
    val unit: String = "KG",
    val quantity: Double = 0.0,
    val spoilage: Double = 0.0,
    val rate: Double = 0.0
)

data class AssistedData(
    val farmerName: String = "",
    val village: String = "",
    val items: List<AssistedItem> = emptyList(),
    val commission: Double = 5.0,
    val labour: Double = 0.0,
    val transport: Double = 0.0,
    val others: Double = 0.0,
    val advance: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistedBillEntryScreen(
    bitmap: Bitmap,
    onBack: () -> Unit,
    onComplete: (AssistedData) -> Unit
) {
    var currentStep by remember { mutableStateOf(AssistedStep.FARMER_NAME) }
    var assistedData by remember { mutableStateOf(AssistedData()) }
    
    // Temporary state for the item being entered
    var currentItem by remember { mutableStateOf(AssistedItem()) }
    
    // Form field states
    var textValue by remember { mutableStateOf("") }
    
    LaunchedEffect(currentStep) {
        textValue = when(currentStep) {
            AssistedStep.FARMER_NAME -> assistedData.farmerName
            AssistedStep.VILLAGE -> assistedData.village
            AssistedStep.PRODUCT_NAME -> currentItem.product
            AssistedStep.GRADE -> currentItem.grade
            AssistedStep.UNIT -> currentItem.unit
            AssistedStep.QUANTITY -> if (currentItem.quantity == 0.0) "" else currentItem.quantity.toString()
            AssistedStep.SPOILAGE -> if (currentItem.spoilage == 0.0) "" else currentItem.spoilage.toString()
            AssistedStep.RATE -> if (currentItem.rate == 0.0) "" else currentItem.rate.toString()
            AssistedStep.COMMISSION -> assistedData.commission.toString()
            AssistedStep.LABOUR -> if (assistedData.labour == 0.0) "" else assistedData.labour.toString()
            AssistedStep.TRANSPORT -> if (assistedData.transport == 0.0) "" else assistedData.transport.toString()
            AssistedStep.OTHERS -> if (assistedData.others == 0.0) "" else assistedData.others.toString()
            AssistedStep.ADVANCE -> if (assistedData.advance == 0.0) "" else assistedData.advance.toString()
            else -> ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assisted Bill Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep.ordinal > 0) {
                            // Back step logic could be complex with items, for now just simple
                            currentStep = AssistedStep.entries[currentStep.ordinal - 1]
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Bill Image Reference
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Bill Reference",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topEnd = 12.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "Reference Image", 
                        color = Color.White, 
                        fontSize = 10.sp, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STEP ${currentStep.ordinal + 1} OF ${AssistedStep.entries.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = currentStep.label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                if (currentStep == AssistedStep.UNIT) {
                    UnitSelector(selected = textValue) { textValue = it }
                } else if (currentStep == AssistedStep.ADD_ANOTHER) {
                    AddAnotherGradeSelection(
                        onYes = {
                            assistedData = assistedData.copy(items = assistedData.items + currentItem)
                            currentItem = AssistedItem(product = currentItem.product) // carry over product
                            currentStep = AssistedStep.GRADE
                        },
                        onNo = {
                            assistedData = assistedData.copy(items = assistedData.items + currentItem)
                            currentStep = AssistedStep.COMMISSION
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter ${currentStep.label}") },
                        placeholder = { Text("Type here...") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when(currentStep) {
                                AssistedStep.QUANTITY, AssistedStep.SPOILAGE, AssistedStep.RATE,
                                AssistedStep.COMMISSION, AssistedStep.LABOUR, AssistedStep.TRANSPORT,
                                AssistedStep.OTHERS, AssistedStep.ADVANCE -> KeyboardType.Decimal
                                else -> KeyboardType.Text
                            }
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                if (currentStep != AssistedStep.ADD_ANOTHER) {
                    Button(
                        onClick = {
                            // Save current field to assistedData or currentItem
                            saveField(currentStep, textValue, assistedData, currentItem).let { (newData, newItem) ->
                                assistedData = newData
                                currentItem = newItem
                            }

                            if (currentStep == AssistedStep.ADVANCE) {
                                onComplete(assistedData)
                            } else {
                                currentStep = AssistedStep.entries[currentStep.ordinal + 1]
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentStep == AssistedStep.ADVANCE) "Finish & View Bill" else "Next Step", fontWeight = FontWeight.Bold)
                    }
                    
                    if (isOptional(currentStep)) {
                        TextButton(onClick = {
                            if (currentStep == AssistedStep.ADVANCE) {
                                onComplete(assistedData)
                            } else {
                                currentStep = AssistedStep.entries[currentStep.ordinal + 1]
                            }
                        }) {
                            Text("Skip this field", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitSelector(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("KG", "Ton", "Boxes").forEach { unit ->
            FilterChip(
                selected = selected == unit,
                onClick = { onSelect(unit) },
                label = { Text(unit) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AddAnotherGradeSelection(onYes: () -> Unit, onNo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onYes,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Yes, Add Another Grade")
        }
        OutlinedButton(
            onClick = onNo,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("No, Continue to Charges")
        }
    }
}

private fun isOptional(step: AssistedStep): Boolean {
    return when(step) {
        AssistedStep.VILLAGE, AssistedStep.SPOILAGE, AssistedStep.LABOUR, 
        AssistedStep.TRANSPORT, AssistedStep.OTHERS, AssistedStep.ADVANCE -> true
        else -> false
    }
}

private fun saveField(step: AssistedStep, value: String, data: AssistedData, item: AssistedItem): Pair<AssistedData, AssistedItem> {
    return when(step) {
        AssistedStep.FARMER_NAME -> data.copy(farmerName = value) to item
        AssistedStep.VILLAGE -> data.copy(village = value) to item
        AssistedStep.PRODUCT_NAME -> data to item.copy(product = value)
        AssistedStep.GRADE -> data to item.copy(grade = value.ifEmpty { "Grade A" })
        AssistedStep.UNIT -> data to item.copy(unit = value.ifEmpty { "KG" })
        AssistedStep.QUANTITY -> data to item.copy(quantity = value.toDoubleOrNull() ?: 0.0)
        AssistedStep.SPOILAGE -> data to item.copy(spoilage = value.toDoubleOrNull() ?: 0.0)
        AssistedStep.RATE -> data to item.copy(rate = value.toDoubleOrNull() ?: 0.0)
        AssistedStep.COMMISSION -> data.copy(commission = value.toDoubleOrNull() ?: 5.0) to item
        AssistedStep.LABOUR -> data.copy(labour = value.toDoubleOrNull() ?: 0.0) to item
        AssistedStep.TRANSPORT -> data.copy(transport = value.toDoubleOrNull() ?: 0.0) to item
        AssistedStep.OTHERS -> data.copy(others = value.toDoubleOrNull() ?: 0.0) to item
        AssistedStep.ADVANCE -> data.copy(advance = value.toDoubleOrNull() ?: 0.0) to item
        else -> data to item
    }
}
