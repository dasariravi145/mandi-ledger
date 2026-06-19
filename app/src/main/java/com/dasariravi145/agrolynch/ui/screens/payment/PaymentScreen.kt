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
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    ocrPartyName: String = "",
    ocrMode: String = "",
    onBackClick: () -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val farmers by viewModel.farmers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingAmount by viewModel.pendingAmount.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val autoBillNumber by viewModel.billNumber.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddDialog by remember { mutableStateOf(ocrAmount > 0) }
    var selectedPaymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var pendingFileForAction by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(exportStatus) {
        if (exportStatus == "PREMIUM_REQUIRED") {
            showPremiumDialog = true
        } else if (exportStatus.startsWith("SUCCESS:")) {
            val filePath = exportStatus.removePrefix("SUCCESS:")
            pendingFileForAction = File(filePath)
        }
    }

    if (pendingFileForAction != null) {
        AlertDialog(
            onDismissRequest = { pendingFileForAction = null },
            title = { Text("Bill Generated") },
            text = { Text("Would you like to Print or Share this bill?") },
            confirmButton = {
                Button(onClick = { 
                    com.dasariravi145.agrolynch.util.PdfGenerator.printPdf(context, pendingFileForAction!!)
                    pendingFileForAction = null
                }) {
                    Icon(Icons.Default.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    com.dasariravi145.agrolynch.util.PdfGenerator.sharePdf(context, pendingFileForAction!!)
                    pendingFileForAction = null
                }) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        )
    }

    LaunchedEffect(ocrPartyName) {
        if (ocrPartyName.isNotEmpty()) {
            // Find if party is buyer or farmer to set tab
            val isBuyer = buyers.any { it.name.contains(ocrPartyName, true) }
            val isFarmer = farmers.any { it.name.contains(ocrPartyName, true) }
            if (isFarmer && !isBuyer) viewModel.onTabSelected(1)
            else if (isBuyer) viewModel.onTabSelected(0)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (selectedTab == 0) stringResource(R.string.receive_payment) else stringResource(R.string.pay_farmer)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = { Text(stringResource(R.string.traders)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = { Text(stringResource(R.string.farmers)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_payment))
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
                        Text(stringResource(R.string.auto_fill_payment_bill, ocrAmount), fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
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
                            onClick = { selectedPaymentToEdit = payment },
                            onPrint = { viewModel.exportPayment(context, payment) }
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
                initialPartyName = ocrPartyName,
                autoBillNumber = autoBillNumber,
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
    onClick: () -> Unit,
    onPrint: () -> Unit
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
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${Formatter.formatCurrency(payment.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = color
                )
                IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, "Print", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
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
    initialPartyName: String = "",
    autoBillNumber: String = "",
    onPartySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String, String, String) -> Unit
) {
    val partyType = if (selectedTab == 0) "BUYER" else "FARMER"
    
    var selectedPartyId by remember { mutableStateOf("") }
    var selectedPartyName by remember { mutableStateOf("") }

    // Handle initial party selection (OCR) and auto-select next available farmer
    LaunchedEffect(farmers, buyers, initialPartyName, selectedTab) {
        if (initialPartyName.isNotEmpty()) {
            if (selectedTab == 0) {
                buyers.find { it.name.contains(initialPartyName, true) }?.let {
                    selectedPartyId = it.id
                    selectedPartyName = it.name
                    onPartySelected(it.id)
                }
            } else {
                farmers.find { it.name.contains(initialPartyName, true) }?.let {
                    selectedPartyId = it.id
                    selectedPartyName = it.name
                    onPartySelected(it.id)
                }
            }
        } else if (selectedTab == 1) {
            if (farmers.isNotEmpty()) {
                if (selectedPartyId.isEmpty() || farmers.none { it.id == selectedPartyId }) {
                    val nextFarmer = farmers.first()
                    selectedPartyId = nextFarmer.id
                    selectedPartyName = nextFarmer.name
                    onPartySelected(nextFarmer.id)
                    Timber.tag("PAY_FARMER").d("PAY_FARMER_DROPDOWN_REFRESHED: Auto-selected next available farmer ${nextFarmer.name}")
                }
            } else {
                selectedPartyId = ""
                selectedPartyName = ""
            }
        }
    }

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
            amount = Formatter.formatWeight(Math.abs(pendingAmount))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (selectedTab == 0) stringResource(R.string.receive_payment) else stringResource(R.string.pay_farmer))
                Text("Bill: $autoBillNumber", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge for pending count
                if (selectedTab == 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Pending Farmers (${farmers.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Party Dropdown
                ExposedDropdownMenuBox(expanded = partyExpanded, onExpandedChange = { partyExpanded = it }) {
                    OutlinedTextField(
                        value = selectedPartyName.ifEmpty { 
                            if (selectedTab == 0) stringResource(R.string.select_buyer) 
                            else if (farmers.isEmpty()) "No farmers with pending payments"
                            else stringResource(R.string.select_farmer) 
                        },
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedTab == 0 || farmers.isNotEmpty(),
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
                                    text = { 
                                        Text("${farmer.name} • Pending ₹${Formatter.formatCurrency(farmer.pendingAmount)}") 
                                    }, 
                                    onClick = { 
                                        selectedPartyId = farmer.id
                                        selectedPartyName = farmer.name
                                        onPartySelected(farmer.id)
                                        partyExpanded = false
                                        Timber.tag("PAY_FARMER").d("PAY_FARMER_DROPDOWN_REFRESHED: Selected ${farmer.name}")
                                    }
                                )
                            }
                        }
                    }
                }

                // Pending Amount Display
                if (selectedPartyId.isNotEmpty()) {
                    val isFarmerSettlePrevented = selectedTab == 1 && pendingAmount <= 0

                    Surface(
                        color = if (isFarmerSettlePrevented) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (selectedTab == 0) stringResource(R.string.amount_to_receive) else stringResource(R.string.amount_to_pay),
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isFarmerSettlePrevented) "Fully Settled" else "₹${Formatter.formatCurrency(Math.abs(pendingAmount))}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFarmerSettlePrevented) MaterialTheme.colorScheme.error 
                                        else if (selectedTab == 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            if (isFarmerSettlePrevented) {
                                Text(
                                    text = "This farmer is already fully settled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
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
                            label = { Text(stringResource(R.string.partial_payment)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isFarmerSettlePrevented
                        )
                        FilterChip(
                            selected = settlementType == "FULL",
                            onClick = { 
                                settlementType = "FULL"
                                amount = Formatter.formatWeight(Math.abs(pendingAmount))
                            },
                            label = { Text(stringResource(R.string.full_settlement)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isFarmerSettlePrevented
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = it
                        if (settlementType == "FULL") settlementType = "PARTIAL"
                    },
                    label = { Text(stringResource(R.string.payment_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₹") },
                    readOnly = settlementType == "FULL",
                    enabled = selectedPartyId.isNotEmpty() && !(selectedTab == 1 && pendingAmount <= 0)
                )

                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
                    OutlinedTextField(
                        value = paymentMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.payment_mode)) },
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
                        label = { Text(stringResource(R.string.ref_no)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
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
                enabled = selectedPartyId.isNotEmpty() && amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0 && !(selectedTab == 1 && pendingAmount <= 0)
            ) {
                Text(stringResource(R.string.confirm_payment))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
            title = { Text(stringResource(R.string.delete_payment_q)) },
            text = { Text(stringResource(R.string.delete_payment_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(payment.id) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.party) + ": ${payment.partyName} (${payment.partyType})")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_record), color = MaterialTheme.colorScheme.error)
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
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
