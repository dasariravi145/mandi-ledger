package com.dasariravi145.agrolynch.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    onBackClick: () -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val farmers by viewModel.farmers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingAmount by viewModel.pendingAmount.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var showAddDialog by remember { mutableStateOf(ocrAmount > 0) }
    var selectedPaymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (selectedTab == 0) "Buyer Payments / వ్యాపారి చెల్లింపులు" else "Farmer Payments / రైతు చెల్లింపులు") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = { Text("Buyers / వ్యాపారులు") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = { Text("Farmers / రైతులు") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // OCR Badge
            if (ocrAmount > 0) {
                Surface(color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Text("Auto-filling payment from bill (Amt: ₹$ocrAmount)", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isLoading && payments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(payments) { payment ->
                        PaymentItem(
                            payment = payment,
                            onClick = { selectedPaymentToEdit = payment }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPaymentDialog(
                buyers = buyers,
                farmers = farmers,
                pendingAmount = pendingAmount,
                selectedTab = selectedTab,
                initialAmount = if (ocrAmount > 0) ocrAmount.toString() else "",
                initialRef = ocrBillNo,
                onPartySelected = viewModel::onPartySelected,
                onDismiss = { showAddDialog = false },
                onSave = { partyId, partyName, partyType, amount, mode, ref, notes ->
                    viewModel.addPayment(partyId, partyName, partyType, amount, mode, ref, notes)
                    showAddDialog = false
                }
            )
        }

        if (selectedPaymentToEdit != null) {
            EditPaymentDialog(
                payment = selectedPaymentToEdit!!,
                onDismiss = { selectedPaymentToEdit = null },
                onUpdate = { updated ->
                    viewModel.updatePayment(updated)
                    selectedPaymentToEdit = null
                },
                onDelete = { id ->
                    viewModel.deletePayment(id)
                    selectedPaymentToEdit = null
                }
            )
        }
    }
}

@Composable
fun PaymentItem(
    payment: PaymentEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (payment.partyType == "BUYER") Icons.Default.TrendingDown else Icons.Default.TrendingUp
            val color = if (payment.partyType == "BUYER") Color(0xFF16A34A) else Color(0xFFDC2626) // Green for money coming in, Red for going out
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = payment.partyName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = payment.paymentMode, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                if (payment.referenceNumber.isNotBlank()) {
                    Text(text = "Ref: ${payment.referenceNumber}", fontSize = 12.sp)
                }
                Text(text = dateFormat.format(Date(payment.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
            Text(
                text = "₹${payment.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    buyers: List<BuyerEntity>,
    farmers: List<FarmerEntity>,
    pendingAmount: Double,
    selectedTab: Int,
    initialAmount: String = "",
    initialRef: String = "",
    onPartySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String, String, String) -> Unit
) {
    val partyType = if (selectedTab == 0) "BUYER" else "FARMER"
    var selectedPartyId by remember { mutableStateOf("") }
    var selectedPartyName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(initialAmount) }
    var paymentMode by remember { mutableStateOf(if (initialRef.isNotEmpty()) "UPI" else "CASH") }
    var reference by remember { mutableStateOf(initialRef) }
    var notes by remember { mutableStateOf("") }
    
    var settlementType by remember { mutableStateOf("PARTIAL") } // "FULL" or "PARTIAL"

    var partyExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    val paymentModes = listOf("CASH", "UPI", "BANK_TRANSFER", "CHEQUE")

    LaunchedEffect(settlementType, pendingAmount) {
        if (settlementType == "FULL") {
            amount = String.format(Locale.US, "%.2f", Math.abs(pendingAmount))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedTab == 0) "Buyer Payment / వ్యాపారి చెల్లింపు" else "Farmer Payment / రైతుకు చెల్లింపు") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Party Dropdown
                ExposedDropdownMenuBox(expanded = partyExpanded, onExpandedChange = { partyExpanded = it }) {
                    OutlinedTextField(
                        value = selectedPartyName.ifEmpty { if (selectedTab == 0) "Select Buyer" else "Select Farmer" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = partyExpanded, onDismissRequest = { partyExpanded = false }) {
                        if (selectedTab == 0) {
                            buyers.forEach { buyer ->
                                DropdownMenuItem(
                                    text = { Text(buyer.name) }, 
                                    onClick = { 
                                        selectedPartyId = buyer.id
                                        selectedPartyName = buyer.name
                                        onPartySelected(buyer.id)
                                        partyExpanded = false 
                                    }
                                )
                            }
                        } else {
                            farmers.forEach { farmer ->
                                DropdownMenuItem(
                                    text = { Text(farmer.name) }, 
                                    onClick = { 
                                        selectedPartyId = farmer.id
                                        selectedPartyName = farmer.name
                                        onPartySelected(farmer.id)
                                        partyExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                // Pending Amount Display
                if (selectedPartyId.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (selectedTab == 0) "Amount to Receive / వ్యాపారి బాకీ" else "Amount to Pay / రైతుకు ఇవ్వాల్సిన బాకీ",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", Math.abs(pendingAmount))}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }

                    // Settlement Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settlementType == "PARTIAL",
                            onClick = { settlementType = "PARTIAL" },
                            label = { Text("Partial / కొంత మొత్తం") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settlementType == "FULL",
                            onClick = { 
                                settlementType = "FULL"
                                amount = String.format(Locale.US, "%.2f", Math.abs(pendingAmount))
                            },
                            label = { Text("Full / పూర్తి సెటిల్మెంట్") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = it
                        if (settlementType == "FULL") settlementType = "PARTIAL"
                    },
                    label = { Text("Payment Amount / చెల్లింపు మొత్తం") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₹") },
                    readOnly = settlementType == "FULL"
                )

                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
                    OutlinedTextField(
                        value = paymentMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode / పద్ధతి") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        paymentModes.forEach { mode ->
                            DropdownMenuItem(text = { Text(mode) }, onClick = { paymentMode = mode; modeExpanded = false })
                        }
                    }
                }

                if (paymentMode != "CASH") {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Ref No / ట్రాన్సాక్షన్ నంబర్") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / గమనికలు") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (selectedPartyId.isNotEmpty() && amt > 0) {
                        onSave(selectedPartyId, selectedPartyName, partyType, amt, paymentMode, reference, notes)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = selectedPartyId.isNotEmpty() && amount.isNotEmpty()
            ) {
                Text("Confirm Payment / చెల్లింపును ధృవీకరించు")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel / రద్దు") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPaymentDialog(
    payment: PaymentEntity,
    onDismiss: () -> Unit,
    onUpdate: (PaymentEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var notes by remember { mutableStateOf(payment.notes) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Payment?") },
            text = { Text("Are you sure you want to delete this payment record?") },
            confirmButton = {
                TextButton(onClick = { onDelete(payment.id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment / సవరించండి") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Party: ${payment.partyName} (${payment.partyType})")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Record", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(payment.copy(
                        amount = amount.toDoubleOrNull() ?: payment.amount,
                        notes = notes
                    ))
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
