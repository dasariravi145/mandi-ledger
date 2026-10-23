package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.ui.screens.arrival.FarmerArrivalForm
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.Formatter
import kotlinx.coroutines.launch

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
    ocrDeductions: String = "",
    onBackClick: () -> Unit,
    onScanBillClick: () -> Unit = {}
) {
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val produceGroups by viewModel.produceGroups.collectAsStateWithLifecycle()
    val buyerSaleTotal by viewModel.buyerSaleTotal.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val autoBillNumber by viewModel.billNumber.collectAsStateWithLifecycle()

    val saleQuantityInput by viewModel.saleQuantityInput.collectAsStateWithLifecycle()
    val saleQuantityUnit by viewModel.saleQuantityUnit.collectAsStateWithLifecycle()
    val showGrossTareMode by viewModel.showGrossTareMode.collectAsStateWithLifecycle()
    val buyerGrossWeightInput by viewModel.buyerGrossWeightInput.collectAsStateWithLifecycle()
    val buyerTareWeightInput by viewModel.buyerTareWeightInput.collectAsStateWithLifecycle()

    val deductions by viewModel.deductions.collectAsStateWithLifecycle()
    val overallLaborPercent by viewModel.overallLaborPercent.collectAsStateWithLifecycle()
    val overallTransport by viewModel.overallTransport.collectAsStateWithLifecycle()

    var selectedBuyer by remember {
        mutableStateOf(buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) })
    }
    var buyerSearchText by remember { mutableStateOf(ocrBuyer) }

    LaunchedEffect(buyers) {
        if (selectedBuyer == null && ocrBuyer.isNotEmpty()) {
            selectedBuyer = buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) }
        }
    }

    LaunchedEffect(ocrQty) {
        if (ocrQty > 0 && saleQuantityInput.isBlank()) {
            viewModel.onSaleQuantityChange(Formatter.formatWeight(ocrQty))
        }
    }

    LaunchedEffect(ocrDeductions) {
        if (ocrDeductions.isNotEmpty()) {
            ocrDeductions.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val type = parts[0]
                    val amount = parts[1].toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.addDeduction(type, amount)
                    }
                }
            }
        }
    }

    // Auto-create initial Produce Group if OCR product is provided
    LaunchedEffect(ocrProduct) {
        if (ocrProduct.isNotEmpty() && produceGroups.isEmpty()) {
            viewModel.addProduceGroup(
                productName = ocrProduct,
                productType = "",
                category = "Fruit"
            )
        }
    }

    var buyerMobile by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }
    var buyerGst by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Sheet states
    var showAddProduceSheet by remember { mutableStateOf(false) }
    var activeProduceGroupIdForAddFarmer by remember { mutableStateOf<String?>(null) }
    var editingFarmerEntry by remember { mutableStateOf<Pair<String, FarmerArrivalData>?>(null) }

    val error by viewModel.error.collectAsState(initial = "")
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            android.widget.Toast.makeText(context, "Sale Entry saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
            selectedBuyer = null
            buyerSearchText = ""
            buyerMobile = ""
            buyerAddress = ""
            buyerGst = ""

            if (viewModel.adMobManager.shouldShowAds()) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    viewModel.adMobManager.showInterstitialAd(activity) {
                        onBackClick()
                    }
                } else {
                    onBackClick()
                }
            } else {
                onBackClick()
            }
        }
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
                },
                actions = {
                    IconButton(onClick = onScanBillClick) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Bill", tint = Color(0xFF2E7D32))
                    }
                }
            )
        },
        bottomBar = {
            if (selectedBuyer != null || buyerSearchText.isNotBlank()) {
                BuyerSaleBottomBar(
                    total = buyerSaleTotal,
                    isLoading = isLoading,
                    onSave = {
                        if (!isLoading) {
                            if (selectedBuyer != null) {
                                viewModel.createBuyerSaleWithFarmerArrivals(
                                    context = context,
                                    buyer = selectedBuyer!!
                                )
                            } else {
                                if (buyerMobile.length != 10) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a valid 10-digit mobile number for the buyer")
                                    }
                                } else {
                                    viewModel.registerBuyerAndCreateSaleWithFarmerArrivals(
                                        context = context,
                                        name = buyerSearchText,
                                        mobile = buyerMobile,
                                        address = buyerAddress,
                                        gst = buyerGst
                                    )
                                }
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
            // STEP 1: BUYER SELECTION & DETAILS
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

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // STEP 2: BILL & SALE QUANTITY (KATA BILL) INPUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kata Bill & Quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = onScanBillClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan Bill", fontSize = 12.sp)
                }
            }

            // Sale Quantity Field & Unit Selector
            if (!showGrossTareMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saleQuantityInput,
                        onValueChange = { viewModel.onSaleQuantityChange(it) },
                        label = { Text("Sale Quantity *") },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )

                    var expandedUnit by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = saleQuantityUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedUnit = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                            listOf("Quintal", "Ton", "KG", "Bags").forEach { unitOption ->
                                DropdownMenuItem(
                                    text = { Text(unitOption) },
                                    onClick = {
                                        viewModel.onSaleQuantityUnitChange(unitOption)
                                        expandedUnit = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (buyerSaleTotal.buyerNetWeightKg > 0 && saleQuantityUnit != "KG") {
                    Text(
                        text = "Normalized Weight: ${Formatter.formatWeight(buyerSaleTotal.buyerNetWeightKg)} KG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Optional Gross & Tare Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Has Gross & Tare / Box Weights?", fontSize = 12.sp, color = Color.Gray)
                Switch(
                    checked = showGrossTareMode,
                    onCheckedChange = { viewModel.toggleGrossTareMode(it) }
                )
            }

            if (showGrossTareMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyerGrossWeightInput,
                        onValueChange = { viewModel.onBuyerGrossWeightChange(it) },
                        label = { Text("Gross Qty ($saleQuantityUnit)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = buyerTareWeightInput,
                        onValueChange = { viewModel.onBuyerTareWeightChange(it) },
                        label = { Text("Tare / Deduction ($saleQuantityUnit)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Weight Reconciliation Card
            WeightReconciliationCard(totals = buyerSaleTotal)

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // STEP 3: PRODUCE / FRUIT CATEGORIES & FARMER ALLOCATIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Farmer Produce Allocations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { showAddProduceSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("+ Add Produce")
                }
            }

            if (produceGroups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Text(
                            text = "No produce categories added yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Click '+ Add Produce' above to add fruits/varieties and allocate farmers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                produceGroups.forEach { group ->
                    ProduceGroupCard(
                        group = group,
                        onAddFarmer = {
                            activeProduceGroupIdForAddFarmer = group.id
                        },
                        onEditFarmer = { farmerData ->
                            editingFarmerEntry = Pair(group.id, farmerData)
                        },
                        onDeleteFarmer = { farmerDataId ->
                            viewModel.removeFarmerFromGroup(group.id, farmerDataId)
                        },
                        onDeleteGroup = {
                            viewModel.removeProduceGroup(group.id)
                        }
                    )
                }
            }

            // STEP 4: OVERALL CHARGES & SUMMARY
            if (produceGroups.any { it.farmers.isNotEmpty() }) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Buyer Charges & Labour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = overallLaborPercent,
                                onValueChange = { viewModel.onOverallLaborPercentChange(it) },
                                label = { Text("Labour (%)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = overallTransport,
                                onValueChange = { viewModel.onOverallTransportChange(it) },
                                label = { Text("Transport (₹)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (buyerSaleTotal.laborPercentage > 0) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Calculated Labour Equivalent:", fontSize = 12.sp, color = Color.DarkGray)
                                    Text(
                                        "${Formatter.formatWeight(buyerSaleTotal.laborEquivalentKg)} KG  (₹${Formatter.formatCurrency(buyerSaleTotal.totalLaborCharges)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }

                // Other Charges / Deductions
                Text(
                    text = "Other Buyer Charges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                deductions.forEachIndexed { index, deduction ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (deduction.deductionType == "Other") deduction.customName else deduction.deductionType,
                            modifier = Modifier.weight(1f)
                        )
                        Text("₹${Formatter.formatCurrency(deduction.amount)}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.removeDeduction(index) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                }

                var showAddDeduction by remember { mutableStateOf(false) }
                if (!showAddDeduction) {
                    OutlinedButton(onClick = { showAddDeduction = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Other Buyer Charge")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var dtype by remember { mutableStateOf("Loading") }
                            var cname by remember { mutableStateOf("") }
                            var amt by remember { mutableStateOf("") }
                            var exp by remember { mutableStateOf(false) }

                            Box {
                                OutlinedTextField(
                                    value = dtype,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { IconButton(onClick = { exp = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                                )
                                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    Constants.DEFAULT_DEDUCTION_TYPES.forEach { type ->
                                        DropdownMenuItem(text = { Text(type) }, onClick = { dtype = type; exp = false })
                                    }
                                }
                            }
                            if (dtype == "Other") {
                                OutlinedTextField(value = cname, onValueChange = { cname = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                            }
                            OutlinedTextField(
                                value = amt,
                                onValueChange = { amt = it },
                                label = { Text("Amount ₹") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                TextButton(onClick = { showAddDeduction = false }) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        val v = amt.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            viewModel.addDeduction(dtype, v, cname)
                                            showAddDeduction = false
                                        }
                                    },
                                    enabled = amt.isNotBlank()
                                ) { Text("Add") }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                BuyerSaleConfirmationSummaryCard(
                    buyerName = if (selectedBuyer != null) selectedBuyer!!.name else buyerSearchText,
                    buyerMobile = if (selectedBuyer != null) selectedBuyer!!.mobileNumber else buyerMobile,
                    buyerAddress = if (selectedBuyer != null) selectedBuyer!!.address else buyerAddress,
                    buyerGst = if (selectedBuyer != null) selectedBuyer!!.gstNumber else buyerGst,
                    totals = buyerSaleTotal,
                    billNumber = autoBillNumber
                )
            }

            Spacer(modifier = Modifier.height(180.dp))
        }
    }

    // Modal Sheet 1: Add Produce Group
    if (showAddProduceSheet) {
        AddProduceModalSheet(
            products = allProducts,
            onDismiss = { showAddProduceSheet = false },
            onConfirm = { productName, productType, category ->
                viewModel.addProduceGroup(productName, productType, category)
                showAddProduceSheet = false
            }
        )
    }

    // Modal Sheet 2: Add Farmer Arrival (Reusing FarmerArrivalForm)
    if (activeProduceGroupIdForAddFarmer != null) {
        val targetGroup = produceGroups.find { it.id == activeProduceGroupIdForAddFarmer }
        if (targetGroup != null) {
            ModalBottomSheet(
                onDismissRequest = { activeProduceGroupIdForAddFarmer = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                FarmerArrivalForm(
                    productName = targetGroup.productName,
                    productType = targetGroup.productType,
                    category = targetGroup.category,
                    farmers = farmers,
                    autoBillNo = autoBillNumber,
                    initialData = null,
                    onConfirm = { farmerData ->
                        viewModel.addFarmerToGroup(targetGroup.id, farmerData)
                        activeProduceGroupIdForAddFarmer = null
                    },
                    onCancel = {
                        activeProduceGroupIdForAddFarmer = null
                    }
                )
            }
        }
    }

    // Modal Sheet 3: Edit Farmer Arrival
    if (editingFarmerEntry != null) {
        val (groupId, farmerData) = editingFarmerEntry!!
        val targetGroup = produceGroups.find { it.id == groupId }
        if (targetGroup != null) {
            ModalBottomSheet(
                onDismissRequest = { editingFarmerEntry = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                FarmerArrivalForm(
                    productName = targetGroup.productName,
                    productType = targetGroup.productType,
                    category = targetGroup.category,
                    farmers = farmers,
                    autoBillNo = autoBillNumber,
                    initialData = farmerData,
                    onConfirm = { updatedFarmerData ->
                        viewModel.updateFarmerInGroup(groupId, updatedFarmerData)
                        editingFarmerEntry = null
                    },
                    onCancel = {
                        editingFarmerEntry = null
                    }
                )
            }
        }
    }
}

@Composable
fun WeightReconciliationCard(totals: BuyerSaleTransactionTotal) {
    val status = totals.weightMatchStatus
    val cardBg = when (status) {
        WeightMatchStatus.MATCHED -> Color(0xFFF1F8E9)
        WeightMatchStatus.UNDER_ALLOCATED, WeightMatchStatus.OVER_ALLOCATED -> Color(0xFFFFF3E0)
        WeightMatchStatus.NO_WEIGHT_ENTERED -> Color(0xFFF5F5F5)
    }
    val borderColor = when (status) {
        WeightMatchStatus.MATCHED -> Color(0xFF2E7D32)
        WeightMatchStatus.UNDER_ALLOCATED, WeightMatchStatus.OVER_ALLOCATED -> Color(0xFFE65100)
        WeightMatchStatus.NO_WEIGHT_ENTERED -> Color.LightGray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weight Reconciliation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )

                when (status) {
                    WeightMatchStatus.MATCHED -> {
                        Surface(
                            color = Color(0xFF2E7D32),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("✓ Weight Matched", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    WeightMatchStatus.UNDER_ALLOCATED -> {
                        Surface(
                            color = Color(0xFFE65100),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("⚠ Weight Not Matched", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    WeightMatchStatus.OVER_ALLOCATED -> {
                        Surface(
                            color = Color(0xFFC62828),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("⚠ Excess Allocation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    WeightMatchStatus.NO_WEIGHT_ENTERED -> {
                        Text("Enter Sale Quantity above", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider(color = borderColor.copy(alpha = 0.3f))

            val factor = SaleViewModel.getUnitConversionFactor(totals.saleQuantityUnit)
            val unallocatedInUnit = if (factor > 0) totals.weightDifferenceKg / factor else totals.weightDifferenceKg

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sale Quantity:", fontSize = 13.sp, color = Color.DarkGray)
                Text(
                    text = "${Formatter.formatWeight(totals.saleQuantityInput)} ${totals.saleQuantityUnit}" +
                            if (totals.saleQuantityUnit != "KG") " (${Formatter.formatWeight(totals.buyerNetWeightKg)} KG)" else "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Farmer Allocation:", fontSize = 13.sp, color = Color.DarkGray)
                Text("${Formatter.formatWeight(totals.totalFarmerNetWeightKg)} KG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            when (status) {
                WeightMatchStatus.UNDER_ALLOCATED -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining to Allocate:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text(
                            text = "${Formatter.formatWeight(unallocatedInUnit)} ${totals.saleQuantityUnit} (${Formatter.formatWeight(totals.weightDifferenceKg)} KG)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text(
                        "⚠ Please add or edit farmer allocations until remaining is 0.",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
                WeightMatchStatus.OVER_ALLOCATED -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Excess Allocation:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        Text(
                            text = "${Formatter.formatWeight(Math.abs(unallocatedInUnit))} ${totals.saleQuantityUnit} (${Formatter.formatWeight(Math.abs(totals.weightDifferenceKg))} KG)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text(
                        "⚠ Farmer allocation exceeds sale quantity. Please adjust farmer quantities.",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
                WeightMatchStatus.MATCHED -> {
                    Text(
                        "✓ Farmer allocation matches Sale Quantity exactly (${Formatter.formatWeight(totals.buyerNetWeightKg)} KG). Save is enabled.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProduceGroupCard(
    group: ProduceGroupDraft,
    onAddFarmer: () -> Unit,
    onEditFarmer: (FarmerArrivalData) -> Unit,
    onDeleteFarmer: (String) -> Unit,
    onDeleteGroup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFC5E1A5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (group.productType.isNotBlank()) "${group.productName} — ${group.productType}" else group.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text("Category: ${group.category}", fontSize = 12.sp, color = Color.Gray)
                }

                IconButton(onClick = onDeleteGroup) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = Color.Red)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

            // Farmers List
            if (group.farmers.isEmpty()) {
                Text(
                    text = "No farmers added for ${group.productName} ${group.productType}. Click '+ Add Farmer' below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                group.farmers.forEach { farmer ->
                    FarmerEntryRowCard(
                        farmer = farmer,
                        onEdit = { onEditFarmer(farmer) },
                        onDelete = { onDeleteFarmer(farmer.id) }
                    )
                }
            }

            // Add Farmer Button
            OutlinedButton(
                onClick = onAddFarmer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("+ Add Farmer", fontWeight = FontWeight.Bold)
            }

            if (group.farmers.isNotEmpty()) {
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Group Net KG: ${Formatter.formatWeight(group.totalNetWeightKg)} KG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Group Sale Total: ₹${Formatter.formatCurrency(group.totalBuyerSaleAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

@Composable
fun FarmerEntryRowCard(
    farmer: FarmerArrivalData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Farmer: ${farmer.farmerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF2E7D32)
                    )
                    if (farmer.farmerPhone.isNotBlank() || farmer.farmerVillage.isNotBlank()) {
                        Text(
                            text = listOf(farmer.farmerPhone, farmer.farmerVillage).filter { it.isNotBlank() }.joinToString(" • "),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFC5E1A5), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Net Weight: ${Formatter.formatWeight(farmer.totalNetWeightKg)} KG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Farmer Rate: ₹${Formatter.formatWeight(farmer.gradeEntries.firstOrNull()?.rate ?: 0.0)}/KG",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Farmer Payable: ₹${Formatter.formatCurrency(farmer.farmerNetPayable)}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Buyer Rate: ₹${Formatter.formatWeight(farmer.buyerSaleRate)}/KG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Buyer Sale Amount: ₹${Formatter.formatCurrency(farmer.buyerSaleAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF15803D)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProduceModalSheet(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (productName: String, productType: String, category: String) -> Unit
) {
    var productNameInput by remember { mutableStateOf("") }
    var productTypeInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Fruit") }
    var expandedProductDropdown by remember { mutableStateOf(false) }

    val filteredProducts = products.filter { it.name.contains(productNameInput, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Produce Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Product Selection / Input
            Box {
                OutlinedTextField(
                    value = productNameInput,
                    onValueChange = {
                        productNameInput = it
                        expandedProductDropdown = true
                    },
                    label = { Text("Product Name (e.g. Mango, Apple) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { expandedProductDropdown = !expandedProductDropdown }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                )

                if (expandedProductDropdown && filteredProducts.isNotEmpty() && productNameInput.isNotBlank()) {
                    DropdownMenu(
                        expanded = expandedProductDropdown,
                        onDismissRequest = { expandedProductDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        filteredProducts.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    productNameInput = product.name
                                    categoryInput = product.category.ifBlank { "Fruit" }
                                    expandedProductDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Product Type / Variety Input
            OutlinedTextField(
                value = productTypeInput,
                onValueChange = { productTypeInput = it },
                label = { Text("Variety / Product Type (e.g. Neelam, Benisha)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Fruit", "Vegetable").forEach { cat ->
                    FilterChip(
                        selected = categoryInput == cat,
                        onClick = { categoryInput = cat },
                        label = { Text(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (productNameInput.isNotBlank()) {
                            onConfirm(productNameInput.trim(), productTypeInput.trim(), categoryInput)
                        }
                    },
                    enabled = productNameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("Add Group")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BuyerSaleConfirmationSummaryCard(
    buyerName: String,
    buyerMobile: String,
    buyerAddress: String,
    buyerGst: String,
    totals: BuyerSaleTransactionTotal,
    billNumber: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text("Bill: $billNumber", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Text(stringResource(R.string.new_buyer_details), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SummaryRow(stringResource(R.string.farmer_name_label), buyerName)
                if (buyerMobile.isNotBlank()) SummaryRow(stringResource(R.string.mobile_number), buyerMobile)
                if (buyerAddress.isNotBlank()) SummaryRow(stringResource(R.string.address), buyerAddress)
                if (buyerGst.isNotBlank()) SummaryRow(stringResource(R.string.gst_number), buyerGst)
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Text("Transaction Totals", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryRow("Sale Quantity", "${Formatter.formatWeight(totals.saleQuantityInput)} ${totals.saleQuantityUnit}")
                SummaryRow("Normalized Net Weight", "${Formatter.formatWeight(totals.buyerNetWeightKg)} KG")
                SummaryRow("Total Farmer Allocation", "${Formatter.formatWeight(totals.totalFarmerNetWeightKg)} KG")
                SummaryRow("Farmer Purchase Total", "₹${Formatter.formatCurrency(totals.totalFarmerPurchaseAmount)}")
                SummaryRow("Buyer Sale Subtotal", "₹${Formatter.formatCurrency(totals.totalBuyerSaleSubtotal)}")

                if (totals.laborPercentage > 0) {
                    SummaryRow("Labour (${Formatter.formatWeight(totals.laborPercentage)}%)", "₹${Formatter.formatCurrency(totals.totalLaborCharges)}", color = Color.Red)
                }
                if (totals.totalTransportCharges > 0) {
                    SummaryRow(stringResource(R.string.transport_rs), "₹${Formatter.formatCurrency(totals.totalTransportCharges)}", color = Color.Red)
                }
                if (totals.totalOtherDeductions > 0) {
                    SummaryRow(stringResource(R.string.other_rs), "₹${Formatter.formatCurrency(totals.totalOtherDeductions)}", color = Color.Red)
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.total_collection), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "₹${Formatter.formatCurrency(totals.totalFinalCollection)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

@Composable
fun BuyerSaleBottomBar(
    total: BuyerSaleTransactionTotal,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = Color(0xFF111827),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Sale Quantity: ${Formatter.formatWeight(total.saleQuantityInput)} ${total.saleQuantityUnit}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        "Farmer Total: ${Formatter.formatWeight(total.totalFarmerNetWeightKg)} KG",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_collection), color = Color.Gray, fontSize = 12.sp)
                    Text(
                        Formatter.formatAmount(total.totalFinalCollection),
                        color = Color(0xFF4ADE80),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = total.isSaveEnabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (total.isSaveEnabled) Color(0xFF22C55E) else Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (total.isSaveEnabled) stringResource(R.string.confirm_save_sale) else "Match Weight to Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
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
        Text(
            text = stringResource(R.string.select_enter_buyer),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
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
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White)
                ) {
                    filtered.take(10).forEach { buyer ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(buyer.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Row {
                                        Text(
                                            "${stringResource(R.string.pending)}: ₹${Formatter.formatCurrency(buyer.pendingAmount)}",
                                            color = Color(0xFFC62828),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
fun SummaryRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
