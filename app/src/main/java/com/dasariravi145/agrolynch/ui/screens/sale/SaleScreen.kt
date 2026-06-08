package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.compose.animation.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    ocrBuyer: String = "",
    ocrProduct: String = "",
    ocrQty: Double = 0.0,
    ocrRate: Double = 0.0,
    onBackClick: () -> Unit
) {
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val saleItems by viewModel.saleItems.collectAsStateWithLifecycle()
    val transactionTotal by viewModel.transactionTotal.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedBuyer by remember { 
        mutableStateOf(buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) }) 
    }
    var buyerSearchText by remember { mutableStateOf(ocrBuyer) }

    LaunchedEffect(buyers) {
        if (selectedBuyer == null && ocrBuyer.isNotEmpty()) {
            selectedBuyer = buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) }
        }
    }
    
    var buyerMobile by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }
    var buyerGst by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddItemSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            selectedBuyer = null
            buyerSearchText = ""
            buyerMobile = ""
            buyerAddress = ""
            buyerGst = ""
            
            if (viewModel.adMobManager.shouldShowAds()) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    viewModel.adMobManager.showInterstitialAd(activity) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Sale Entry Saved Successfully!")
                        }
                    }
                } else {
                    snackbarHostState.showSnackbar("Sale Entry Saved Successfully!")
                }
            } else {
                snackbarHostState.showSnackbar("Sale Entry Saved Successfully!")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sale_entry), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (saleItems.isNotEmpty() && (selectedBuyer != null || buyerSearchText.isNotBlank())) {
                val mobileError = stringResource(R.string.mobile_number_error)
                SaleTransactionSummaryCard(
                    total = transactionTotal,
                    isLoading = isLoading,
                    onSave = {
                        if (selectedBuyer != null) {
                            viewModel.createSale(buyer = selectedBuyer!!)
                        } else {
                            if (buyerMobile.length != 10) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(mobileError)
                                }
                            } else {
                                viewModel.registerBuyerAndCreateSale(
                                    name = buyerSearchText,
                                    mobile = buyerMobile,
                                    address = buyerAddress,
                                    gst = buyerGst
                                )
                            }
                        }
                    }
                )
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
            // STEP 1: BUYER SECTION (EXISTING)
            BuyerSearchableDropdown(
                value = buyerSearchText,
                onValueChange = { 
                    buyerSearchText = it
                    if (selectedBuyer?.name != it) selectedBuyer = null
                },
                selectedBuyer = selectedBuyer,
                buyers = buyers,
                onBuyerSelected = { 
                    selectedBuyer = it
                    buyerSearchText = it.name
                }
            )

            if (selectedBuyer == null && buyerSearchText.isNotBlank()) {
                NewBuyerDetailsFields(
                    mobile = buyerMobile,
                    onMobileChange = { buyerMobile = it },
                    address = buyerAddress,
                    onAddressChange = { buyerAddress = it },
                    gst = buyerGst,
                    onGstChange = { buyerGst = it }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // STEP 2: SALE ITEMS LIST
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(stringResource(R.string.sale_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (selectedBuyer != null || buyerSearchText.isNotBlank()) {
                    Button(
                        onClick = { showAddItemSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_item))
                    }
                }
            }

            if (saleItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_items_added), color = Color.Gray)
                }
            } else {
                saleItems.forEach { item ->
                    SaleItemRowCard(item = item, onRemove = { viewModel.removeSaleItem(item.id) })
                }
            }
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (showAddItemSheet) {
        AddItemModalSheet(
            viewModel = viewModel,
            onDismiss = { showAddItemSheet = false },
            onItemAdded = { 
                viewModel.addSaleItem(it)
                showAddItemSheet = false 
            }
        )
    }
}

@Composable
fun BuyerSearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    selectedBuyer: BuyerEntity?,
    buyers: List<BuyerEntity>,
    onBuyerSelected: (BuyerEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = buyers.filter { it.name.contains(value, ignoreCase = true) }

    Column {
        Text(stringResource(R.string.select_enter_buyer), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it); expanded = true },
                placeholder = { Text(stringResource(R.string.type_buyer_name)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { 
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            if (expanded && filtered.isNotEmpty()) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                ) {
                    filtered.take(10).forEach { buyer ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(buyer.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Row {
                                        Text("Pending: ₹${String.format("%.0f", buyer.pendingAmount)}", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(12.dp))
                                        Text(buyer.mobileNumber, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            },
                            onClick = { onBuyerSelected(buyer); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewBuyerDetailsFields(
    mobile: String,
    onMobileChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    gst: String,
    onGstChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.new_buyer_details),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        OutlinedTextField(
            value = mobile,
            onValueChange = { 
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    onMobileChange(it)
                }
            },
            label = { Text(stringResource(R.string.mobile_number) + " *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(R.string.address) + " *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = gst,
            onValueChange = onGstChange,
            label = { Text(stringResource(R.string.gst_number) + " (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SaleItemRowCard(item: SaleItemDraft, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.arrival.farmerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text("${item.arrival.productName} (${item.arrival.grade})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Qty: ${item.quantity} ${item.arrival.unit}", fontSize = 13.sp)
                    Text("Rate: ₹${item.saleRate} (Buy: ₹${item.arrival.purchaseRate})", fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Amount", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${String.format("%.2f", item.netAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemModalSheet(
    viewModel: SaleViewModel,
    onDismiss: () -> Unit,
    onItemAdded: (SaleItemDraft) -> Unit
) {
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val farmersWithStock by viewModel.farmersWithStock.collectAsStateWithLifecycle()
    
    var selectedFarmerId by remember { mutableStateOf<String?>(null) }
    val stockByFarmer by if (selectedFarmerId != null) 
        viewModel.getAvailableStockByFarmer(selectedFarmerId!!).collectAsStateWithLifecycle(emptyList())
        else remember { mutableStateOf(emptyList<ArrivalEntity>()) }
    
    var selectedStockItem by remember { mutableStateOf<ArrivalEntity?>(null) }

    // Form inputs
    var saleQty by remember { mutableStateOf("") }
    var saleRate by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("0") }
    var transport by remember { mutableStateOf("0") }
    var commission by remember { mutableStateOf("5.0") }
    var other by remember { mutableStateOf("0") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = modalState) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.add_item), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Step 2: Select Farmer
            Text(stringResource(R.string.select_farmer), style = MaterialTheme.typography.labelMedium)
            SelectionDropdown(
                items = farmersWithStock.map { it.farmerName },
                selectedIndex = farmersWithStock.indexOfFirst { it.farmerId == selectedFarmerId },
                onSelect = { index ->
                    selectedFarmerId = farmersWithStock[index].farmerId
                    selectedStockItem = null
                }
            )

            // Step 3: Load Stock Entries
            if (selectedFarmerId != null) {
                Text(stringResource(R.string.select_stock_entry), style = MaterialTheme.typography.labelMedium)
                stockByFarmer.forEach { arrival ->
                    FarmerStockSelectionCard(
                        arrival = arrival,
                        isSelected = selectedStockItem?.id == arrival.id,
                        onClick = { 
                            selectedStockItem = arrival
                            saleRate = arrival.purchaseRate.toString()
                            if (arrival.commissionPercent > 0) {
                                commission = arrival.commissionPercent.toString()
                            }
                        }
                    )
                }
            }

            // Step 4 & 5: Details
            if (selectedStockItem != null) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.available_stock), fontSize = 11.sp, color = Color.Gray)
                            Text("${selectedStockItem!!.remainingQuantity} ${selectedStockItem!!.unit}", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.farmer_purchase_rate), fontSize = 11.sp, color = Color.Gray)
                            Text("₹${selectedStockItem!!.purchaseRate}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = saleQty,
                    onValueChange = { saleQty = it },
                    label = { Text(stringResource(R.string.sale_qty_with_unit, selectedStockItem!!.unit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = (saleQty.toDoubleOrNull() ?: 0.0) > selectedStockItem!!.remainingQuantity
                )

                OutlinedTextField(
                    value = saleRate,
                    onValueChange = { saleRate = it },
                    label = { Text(stringResource(R.string.sale_rate_per_unit, selectedStockItem!!.unit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commission, 
                        onValueChange = { commission = it }, 
                        label = { Text(stringResource(R.string.comm_percent)) }, 
                        modifier = Modifier.weight(1f), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            val amt = ((saleQty.toDoubleOrNull() ?: 0.0) * (saleRate.toDoubleOrNull() ?: 0.0) * (commission.toDoubleOrNull() ?: 0.0)) / 100
                            Text(stringResource(R.string.earned_amt, String.format("%.2f", amt)), color = Color(0xFF16A34A))
                        }
                    )
                    OutlinedTextField(value = labor, onValueChange = { labor = it }, label = { Text(stringResource(R.string.labor_rs)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }

                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = transport, onValueChange = { transport = it }, label = { Text(stringResource(R.string.transport_rs)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = other, onValueChange = { other = it }, label = { Text(stringResource(R.string.other_rs)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }

                Button(
                    onClick = {
                        onItemAdded(SaleItemDraft(
                            arrival = selectedStockItem!!,
                            quantity = saleQty.toDoubleOrNull() ?: 0.0,
                            saleRate = saleRate.toDoubleOrNull() ?: 0.0,
                            commissionPercent = commission.toDoubleOrNull() ?: 0.0,
                            laborCharges = labor.toDoubleOrNull() ?: 0.0,
                            transportCharges = transport.toDoubleOrNull() ?: 0.0,
                            otherCharges = other.toDoubleOrNull() ?: 0.0
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = saleQty.isNotEmpty() && saleRate.isNotEmpty() && (saleQty.toDoubleOrNull() ?: 0.0) <= selectedStockItem!!.remainingQuantity
                ) {
                    Text(stringResource(R.string.add_to_sale_list))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FarmerStockSelectionCard(arrival: ArrivalEntity, isSelected: Boolean, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(arrival.date))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${arrival.productName} (${arrival.grade})", fontWeight = FontWeight.Bold)
                Text(dateStr, fontSize = 11.sp, color = Color.Gray)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Stock: ${arrival.remainingQuantity} ${arrival.unit}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                Text("Rate: ₹${arrival.purchaseRate}", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SelectionDropdown(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
            color = Color.White
        ) {
            Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (selectedIndex >= 0) items[selectedIndex] else "Select...")
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}

@Composable
fun SaleTransactionSummaryCard(
    total: TransactionTotal,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111827),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.total_items), color = Color.Gray, fontSize = 12.sp)
                    Text("Qty: ${total.totalQuantity} Units", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_collection), color = Color.Gray, fontSize = 12.sp)
                    Text("₹${String.format("%.2f", total.totalNetAmount)}", color = Color(0xFF4ADE80), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(stringResource(R.string.confirm_save_sale), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
