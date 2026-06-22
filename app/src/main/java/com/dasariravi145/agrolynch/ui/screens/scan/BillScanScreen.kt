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
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillData
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
            // We pass a dummy string for deductions here as it's now handled by the review screen confirm
            onNavigateToEntry(
                state.target ?: ScanTarget.STOCK_ENTRY,
                d.originalBillRefNo ?: "",
                d.netAmount,
                d.date,
                d.farmerName,
                "", // phone
                d.farmerPlace,
                "", // buyer
                "", // party
                d.items.firstOrNull()?.productName ?: "",
                "General",
                d.items.firstOrNull()?.grade ?: "",
                d.items.firstOrNull()?.quantityKg ?: 0.0,
                d.items.firstOrNull()?.rate ?: 0.0,
                "", // mode
                "KG",
                0, // boxes
                0.0, // weightTon
                0.0, // emptyBox
                0.0, // spoilage
                "" // deductions
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
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text("Could not read clearly. Please enter manually.", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    onNavigateToEntry(ScanTarget.STOCK_ENTRY, "", 0.0, System.currentTimeMillis(), "", "", "", "", "", "", "", "", 0.0, 0.0, "", "", 0, 0.0, 0.0, 0.0, "")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Open Manual Entry")
                            }
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
                    // Automatically trigger navigation to review screen
                    LaunchedEffect(state.ocrFinished) {
                        onNavigateToEntry(
                            state.target ?: ScanTarget.STOCK_ENTRY,
                            "", 0.0, 0L, "", "", "", "", "", "", "", "", 0.0, 0.0, "", "", 0, 0.0, 0.0, 0.0, ""
                        )
                    }
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
