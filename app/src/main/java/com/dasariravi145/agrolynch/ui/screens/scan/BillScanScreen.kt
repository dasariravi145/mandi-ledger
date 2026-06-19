package com.dasariravi145.agrolynch.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ads.BannerAdView
import com.dasariravi145.agrolynch.util.ExtractedData
import com.dasariravi145.agrolynch.util.Formatter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScanScreen(
    viewModel: BillScanViewModel,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToEntry: (ScanTarget, String, Double, Long, String, String, String, String, String, String, String, String, Double, Double, String, String, Int, Double, Double, Double, String) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTypeDialog by remember { mutableStateOf(false) }
    
    var hasCameraPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showCamera by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            bitmap?.let { b -> viewModel.processImage(b) }
        }
    }

    // Type selection dialog removed - directly default to STOCK_ENTRY
    /*
    if (showTypeDialog) {
        ScanTypeSelectionDialog(
            onTypeSelected = { 
                viewModel.setScanTarget(it)
                showTypeDialog = false 
            },
            onDismiss = onBackClick
        )
    }
    */

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val d = state.extractedData
            val deductionsStr = d.deductions.joinToString(";") { "${it.deductionType}:${it.amount}" }
            onNavigateToEntry(
                state.target ?: ScanTarget.STOCK_ENTRY,
                d.billNumber,
                if (d.amount > 0) d.amount else d.netAmount,
                d.date,
                d.farmerName,
                d.farmerPhone,
                d.farmerVillage,
                d.buyerName,
                d.partyName,
                d.productName,
                d.category,
                d.grade,
                d.quantity,
                d.rate,
                d.paymentMode,
                d.unit,
                d.numberOfBoxes,
                d.totalWeightTon,
                d.emptyBoxWeightPerBox,
                d.spoilagePercentage,
                deductionsStr
            )
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.read_bill)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (showCamera) showCamera = false 
                        else if (state.ocrFinished) viewModel.resetState()
                        else onBackClick() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.ocrFinished) {
                        IconButton(onClick = { viewModel.resetState() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retake")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showCamera && hasCameraPermission) {
            CameraView(
                onImageCaptured = { bitmap, rotation ->
                    viewModel.processImage(bitmap, rotation)
                    showCamera = false
                },
                onError = { Log.e("BillScan", "Error: ${it.message}") }
            )
        } else if (!showTypeDialog) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isPremium) { BannerAdView() }

                state.error?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }

                if (!state.ocrFinished && !state.isLoading) {
                    GuidanceCard()
                    
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    
                    Text(state.target?.let { getTargetLabel(it) } ?: stringResource(R.string.read_bill), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Capture bill to extract details.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { 
                            if (hasCameraPermission) showCamera = true 
                            else permissionLauncher.launch(Manifest.permission.CAMERA) 
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Capture Bill Photo", fontSize = 18.sp)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Choose from Gallery", fontSize = 18.sp)
                    }
                } else if (state.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF16A34A))
                        Spacer(Modifier.height(16.dp))
                        Text("Extracting numbers from bill...", fontWeight = FontWeight.Medium)
                    }
                } else if (state.ocrFinished) {
                    OcrReviewContent(
                        bitmap = state.currentImageBitmap,
                        data = state.extractedData,
                        target = state.target ?: ScanTarget.STOCK_ENTRY,
                        onConfirm = { viewModel.confirmOcrData(it) },
                        onRescan = { viewModel.resetState() }
                    )
                }
            }
        }
    }
}

@Composable
fun getTargetLabel(target: ScanTarget): String {
    return when(target) {
        ScanTarget.STOCK_ENTRY -> stringResource(R.string.farmer_arrival)
        ScanTarget.SALE_ENTRY -> stringResource(R.string.buyer_sale)
        ScanTarget.PAYMENT -> stringResource(R.string.receive_payment)
        ScanTarget.CHEQUE -> "Cheque"
        ScanTarget.EXPENSE -> stringResource(R.string.expenses)
    }
}

@Composable
fun ScanTypeSelectionDialog(onTypeSelected: (ScanTarget) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What are you reading?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanTarget.entries.forEach { target ->
                    ScanTypeOption(getTargetLabel(target), getIconForTarget(target), target, onTypeSelected)
                }
            }
        },
        confirmButton = {}
    )
}

fun getIconForTarget(target: ScanTarget) = when(target) {
    ScanTarget.STOCK_ENTRY -> Icons.Default.AddBusiness
    ScanTarget.SALE_ENTRY -> Icons.Default.ShoppingCart
    ScanTarget.PAYMENT -> Icons.Default.Payments
    ScanTarget.CHEQUE -> Icons.Default.CreditCard
    ScanTarget.EXPENSE -> Icons.Default.ReceiptLong
}

@Composable
fun ScanTypeOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, target: ScanTarget, onSelect: (ScanTarget) -> Unit) {
    Surface(
        onClick = { onSelect(target) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrReviewContent(
    bitmap: Bitmap?,
    data: ExtractedData,
    target: ScanTarget,
    onConfirm: (ExtractedData) -> Unit,
    onRescan: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    var billNo by remember { mutableStateOf(data.billNumber) }
    var dateStr by remember { mutableStateOf(sdf.format(Date(data.date))) }
    var farmerName by remember { mutableStateOf(data.farmerName) }
    var farmerPhone by remember { mutableStateOf(data.farmerPhone) }
    var farmerVillage by remember { mutableStateOf(data.farmerVillage) }
    var buyerName by remember { mutableStateOf(data.buyerName) }
    var productName by remember { mutableStateOf(data.productName) }
    var category by remember { mutableStateOf(data.category) }
    var grade by remember { mutableStateOf(data.grade) }
    var unit by remember { mutableStateOf(data.unit) }
    var qty by remember { mutableStateOf(if(data.quantity > 0) Formatter.formatWeight(data.quantity) else "") }
    var boxes by remember { mutableStateOf(if(data.numberOfBoxes > 0) data.numberOfBoxes.toString() else "") }
    var weightTon by remember { mutableStateOf(if(data.totalWeightTon > 0) Formatter.formatWeight(data.totalWeightTon) else "") }
    var emptyWtPerBox by remember { mutableStateOf(if(data.emptyBoxWeightPerBox > 0) Formatter.formatWeight(data.emptyBoxWeightPerBox) else "") }
    var spoilagePercent by remember { mutableStateOf(if(data.spoilagePercentage > 0) Formatter.formatWeight(data.spoilagePercentage) else "0") }
    var damage by remember { mutableStateOf(if(data.damageOrSoot > 0) Formatter.formatWeight(data.damageOrSoot) else "0") }
    var rate by remember { mutableStateOf(if(data.rate > 0) Formatter.formatWeight(data.rate) else "") }
    var amount by remember { mutableStateOf(if(data.amount > 0) Formatter.formatCurrency(data.amount) else "") }
    var comm by remember { mutableStateOf(if(data.commission > 0) Formatter.formatWeight(data.commission) else "5") }
    var labor by remember { mutableStateOf(if(data.labor > 0) Formatter.formatWeight(data.labor) else "0") }
    var trans by remember { mutableStateOf(if(data.transport > 0) Formatter.formatWeight(data.transport) else "0") }
    var netAmt by remember { mutableStateOf(if(data.netAmount > 0) Formatter.formatCurrency(data.netAmount) else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Text("Detected Numbers & Dates:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.detectedNumbers.forEach { valStr ->
                SuggestionChip(onClick = { /* Could auto-assign logic here if needed */ }, label = { Text(valStr) })
            }
        }

        HorizontalDivider()

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                OcrSelectionField("Bill Number", billNo, { billNo = it }, data.detectedNumbers)
                OcrSelectionField("Date (dd/mm/yyyy)", dateStr, { dateStr = it }, data.detectedNumbers)

                when(target) {
                    ScanTarget.STOCK_ENTRY -> {
                        OutlinedTextField(value = farmerName, onValueChange = { farmerName = it }, label = { Text("Farmer Name *") }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = farmerPhone, onValueChange = { if(it.length <= 10) farmerPhone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            OutlinedTextField(value = farmerVillage, onValueChange = { farmerVillage = it }, label = { Text("Village") }, modifier = Modifier.weight(1f))
                        }
                        
                        OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Grade") }, modifier = Modifier.weight(1f))
                        }
                        
                        // Unit Selection
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Unit:", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
                            listOf("KG", "Ton", "Boxes").forEach { u ->
                                FilterChip(
                                    selected = unit == u,
                                    onClick = { unit = u },
                                    label = { Text(u) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (unit == "Boxes") {
                            OcrSelectionField("Total Weight (Ton) *", weightTon, { weightTon = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Number of Boxes *", boxes, { boxes = it }, data.detectedNumbers, KeyboardType.Number)
                            OcrSelectionField("Empty Wt/Box (KG) *", emptyWtPerBox, { emptyWtPerBox = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Spoilage %", spoilagePercent, { spoilagePercent = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Rate per KG *", rate, { rate = it }, data.detectedNumbers, KeyboardType.Decimal)
                        } else {
                            OcrSelectionField("Quantity (${unit}) *", qty, { qty = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Damage / Soot", damage, { damage = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField(if (unit == "Ton") "Rate per KG *" else "Rate *", rate, { rate = it }, data.detectedNumbers, KeyboardType.Decimal)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OcrSelectionField("Commission %", comm, { comm = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Labor ₹", labor, { labor = it }, data.detectedNumbers, KeyboardType.Decimal)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OcrSelectionField("Transport ₹", trans, { trans = it }, data.detectedNumbers, KeyboardType.Decimal)
                            OcrSelectionField("Net Amount ₹", netAmt, { netAmt = it }, data.detectedNumbers, KeyboardType.Decimal)
                        }
                    }
                    else -> { /* Other types hidden */ }
                }
            }
        }

        val isReady = when(target) {
            ScanTarget.STOCK_ENTRY -> {
                if (unit == "Boxes") farmerName.isNotEmpty() && productName.isNotEmpty() && weightTon.isNotEmpty() && boxes.isNotEmpty() && emptyWtPerBox.isNotEmpty() && rate.isNotEmpty()
                else farmerName.isNotEmpty() && productName.isNotEmpty() && qty.isNotEmpty() && rate.isNotEmpty()
            }
            else -> farmerName.isNotEmpty() && amount.isNotEmpty()
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f).height(56.dp)) { Text("Rescan") }
            Button(
                onClick = { 
                    val finalDate = try { sdf.parse(dateStr)?.time ?: data.date } catch(e: Exception) { data.date }
                    onConfirm(data.copy(
                        billNumber = billNo, date = finalDate,
                        farmerName = farmerName, farmerPhone = farmerPhone, farmerVillage = farmerVillage,
                        buyerName = buyerName, partyName = farmerName,
                        productName = productName, category = category, grade = grade, unit = unit,
                        quantity = if (unit == "Boxes") weightTon.toDoubleOrNull() ?: 0.0 else qty.toDoubleOrNull() ?: 0.0,
                        numberOfBoxes = boxes.toIntOrNull() ?: 0,
                        totalWeightTon = if (unit == "Boxes") weightTon.toDoubleOrNull() ?: 0.0 else 0.0,
                        emptyBoxWeightPerBox = emptyWtPerBox.toDoubleOrNull() ?: 0.0,
                        totalEmptyBoxWeightKg = (boxes.toIntOrNull() ?: 0) * (emptyWtPerBox.toDoubleOrNull() ?: 0.0),
                        spoilagePercentage = spoilagePercent.toDoubleOrNull() ?: 0.0,
                        damageOrSoot = damage.toDoubleOrNull() ?: 0.0,
                        rate = rate.toDoubleOrNull() ?: 0.0, amount = amount.toDoubleOrNull() ?: 0.0,
                        commission = comm.toDoubleOrNull() ?: 0.0, 
                        labor = labor.toDoubleOrNull() ?: 0.0,
                        transport = trans.toDoubleOrNull() ?: 0.0,
                        netAmount = netAmt.toDoubleOrNull() ?: 0.0
                    ))
                },
                modifier = Modifier.weight(2f).height(56.dp),
                enabled = isReady,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) { Text("Review Farmer Arrival", fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrSelectionField(label: String, value: String, onValueChange: (String) -> Unit, options: List<String>, keyboardType: KeyboardType = KeyboardType.Text) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        FlowRow(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.take(8).forEach { opt ->
                SuggestionChip(
                    onClick = { onValueChange(opt) },
                    label = { Text(opt, fontSize = 10.sp) },
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = if(value == opt) Color(0xFF16A34A) else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun GuidanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("TIPS:", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            GuidanceRow("Handwriting is hard to read for names.")
            GuidanceRow("Assign detected numbers to fields manually.")
            GuidanceRow("Verify everything before saving.")
        }
    }
}

@Composable
fun GuidanceRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = Color(0xFF166534))
    }
}

@Composable
fun CameraView(onImageCaptured: (Bitmap, Int) -> Unit, onError: (ImageCaptureException) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { imageCaptureInstance() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    } catch (e: Exception) { Log.e("CameraView", "Binding failed", e) }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = image.toBitmap()
                        onImageCaptured(bitmap, rotation)
                        image.close()
                    }
                    override fun onError(exception: ImageCaptureException) { onError(exception) }
                })
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).size(70.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Icon(Icons.Default.Camera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
        }
    }
}

private fun imageCaptureInstance() = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    .build()

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
