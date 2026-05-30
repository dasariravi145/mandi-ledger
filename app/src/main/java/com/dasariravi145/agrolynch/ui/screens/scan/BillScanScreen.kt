package com.dasariravi145.agrolynch.ui.screens.scan

import android.Manifest
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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dasariravi145.agrolynch.ads.BannerAdView
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import com.dasariravi145.agrolynch.util.ExtractedData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScanScreen(
    viewModel: BillScanViewModel,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToEntry: (ScanTarget, String, Double, Long) -> Unit, // target, billNo, amount, date
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showTypeDialog by remember { mutableStateOf(true) }
    
    var hasCameraPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) 
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

    if (showTypeDialog) {
        ScanTypeSelectionDialog(
            onTypeSelected = { 
                viewModel.setScanTarget(it)
                showTypeDialog = false 
            },
            onDismiss = onBackClick
        )
    }

    if (state.isSuccess) {
        LaunchedEffect(Unit) {
            onNavigateToEntry(
                state.target!!,
                state.extractedData.billNumber,
                state.extractedData.amount,
                state.extractedData.date
            )
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart OCR Scan") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (showCamera) showCamera = false 
                        else if (state.ocrFinished) viewModel.resetState()
                        else onBackClick() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (showCamera && hasCameraPermission) {
            CameraView(
                onImageCaptured = { bitmap ->
                    viewModel.processImage(bitmap)
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
                if (!isPremium) {
                    BannerAdView()
                }

                if (!state.ocrFinished && !state.isLoading) {
                    // SCAN STEP
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    
                    Text("Capture ${state.target?.name?.replace("_", " ")} Bill", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Scan to auto-fill Bill #, Date and Amount", textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { 
                            if (hasCameraPermission) showCamera = true 
                            else permissionLauncher.launch(Manifest.permission.CAMERA) 
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Open Camera / కెమెరా", fontSize = 18.sp)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Pick from Gallery / గ్యాలరీ", fontSize = 18.sp)
                    }
                } else if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF16A34A))
                    }
                } else if (state.ocrFinished) {
                    // REVIEW STEP
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
fun ScanTypeSelectionDialog(
    onTypeSelected: (ScanTarget) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What do you want to create?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanTypeOption("Add Stock Entry", Icons.Default.AddBusiness, ScanTarget.STOCK_ENTRY, onTypeSelected)
                ScanTypeOption("Add Sale Entry", Icons.Default.ShoppingCart, ScanTarget.SALE_ENTRY, onTypeSelected)
                ScanTypeOption("Add Payment", Icons.Default.Payments, ScanTarget.PAYMENT, onTypeSelected)
            }
        },
        confirmButton = {}
    )
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
fun OcrReviewContent(
    bitmap: Bitmap?,
    data: ExtractedData,
    target: ScanTarget,
    onConfirm: (ExtractedData) -> Unit,
    onRescan: () -> Unit
) {
    var editAmount by remember { mutableStateOf(data.amount.toString()) }
    var editBillNo by remember { mutableStateOf(data.billNumber) }
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var editDate by remember { mutableStateOf(sdf.format(Date(data.date))) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Text("Preview Extracted Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editBillNo,
                    onValueChange = { editBillNo = it },
                    label = { Text("Bill Number / బిల్ నంబర్") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = { Text("Amount / నగదు") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                )

                OutlinedTextField(
                    value = editDate,
                    onValueChange = { editDate = it },
                    label = { Text("Bill Date (dd/mm/yyyy)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("Rescan")
            }
            Button(
                onClick = { 
                    val finalDate = try { sdf.parse(editDate)?.time ?: data.date } catch(e: Exception) { data.date }
                    onConfirm(data.copy(
                        amount = editAmount.toDoubleOrNull() ?: 0.0,
                        billNumber = editBillNo,
                        date = finalDate
                    ))
                },
                modifier = Modifier.weight(2f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Confirm & Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraView(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
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
                        val bitmap = image.toBitmap()
                        onImageCaptured(bitmap)
                        image.close()
                    }
                    override fun onError(exception: ImageCaptureException) { onError(exception) }
                })
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).size(80.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Icon(Icons.Default.Camera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
        }
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
