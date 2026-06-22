package com.dasariravi145.agrolynch.ui.screens.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.ui.screens.arrival.ArrivalViewModel
import androidx.navigation.NavController
import com.dasariravi145.agrolynch.ui.navigation.Screen
import timber.log.Timber
import java.util.concurrent.Executors

@Composable
fun FarmerBillScannerScreen(
    viewModel: ScannerViewModel,
    arrivalViewModel: ArrivalViewModel,
    navController: NavController,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScannerUiState.Idle, is ScannerUiState.Processing -> {
                CameraPreview(
                    onImageCaptureCreated = { imageCapture = it },
                    modifier = Modifier.fillMaxSize()
                )

                if (state is ScannerUiState.Processing) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Processing image...", color = Color.White)
                        }
                    }
                } else {
                    // Capture UI
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val capture = imageCapture ?: return@FloatingActionButton
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = image.toBitmap()
                                            viewModel.onImageCaptured(bitmap)
                                            image.close()
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Timber.e(exception, "CAPTURE_FAILED")
                                        }
                                    }
                                )
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Capture bill photo for reference", color = Color.White)
                    }
                }
            }
            is ScannerUiState.Captured -> {
                AssistedBillEntryScreen(
                    bitmap = state.bitmap,
                    onBack = { viewModel.reset() },
                    onComplete = { data ->
                        // Convert products list to string format for NewArrivalScreen
                        val ocrItems = data.items.joinToString(";") { p ->
                            "${p.product}|${p.grade}|${p.quantity}|${p.rate}|${p.quantity * p.rate}|${p.unit}|${p.spoilage}"
                        }
                        
                        val route = Screen.NewArrival.passOcr(
                            billNo = "",
                            farmer = data.farmerName,
                            phone = "",
                            village = data.village,
                            comm = data.commission,
                            labor = data.labour,
                            transport = data.transport,
                            deductions = "Gate/Others:${data.others};Advance:${data.advance}",
                            ocrItems = ocrItems,
                            autoSave = false,
                            product = data.items.firstOrNull()?.product ?: "",
                            qty = data.items.firstOrNull()?.quantity ?: 0.0,
                            rate = data.items.firstOrNull()?.rate ?: 0.0,
                            unit = data.items.firstOrNull()?.unit ?: "KG"
                        )
                        navController.navigate(route) {
                            popUpTo(Screen.FarmerBillScanner.route) { inclusive = true }
                        }
                    }
                )
            }
            is ScannerUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = Color.Red)
                        Button(onClick = { viewModel.reset() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CameraPreview(
    onImageCaptureCreated: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setTargetRotation(previewView.display.rotation)
                    .build()
                
                onImageCaptureCreated(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Timber.e(e, "CAMERA_BIND_FAILED")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
