package com.dasariravi145.agrolynch.ui.screens.template

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.domain.model.ElementLayout
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDesignerScreen(
    viewModel: InvoiceProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewHtml by viewModel.previewHtml.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (state.isDirty) showUnsavedDialog = true
        else onBack()
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Design Changes") },
            text = { Text("Do you want to save your invoice customization before exiting?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveAll()
                    showUnsavedDialog = false
                    onBack()
                }) { Text("Save & Exit") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) { Text("Discard") }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Original Template?") },
            text = { Text("This will reset all customization for ${state.selectedTemplateId} to default layout.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreOriginalTemplate()
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Restore Original") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customize Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(state.selectedTemplateId, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isDirty) showUnsavedDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = state.undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = state.redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Button(
                        onClick = { showAiSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Design", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.saveAll() },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // AI Preview Banner if AI suggested changes
            if (state.aiPreviewConfig != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    border = BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Design Preview", fontWeight = FontWeight.Bold, color = Color(0xFF312E81))
                        }
                        state.aiReplyMessage?.let {
                            Text(it, fontSize = 12.sp, color = Color.DarkGray)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.discardAiDesign() }) {
                                Text("Discard")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.applyAiDesign() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Apply Design")
                            }
                        }
                    }
                }
            }

            // Canvas Zoom Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setZoomScale(state.zoomScale - 0.15f) }) {
                        Icon(Icons.Default.ZoomOut, null)
                    }
                    Text("${(state.zoomScale * 100).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.setZoomScale(state.zoomScale + 0.15f) }) {
                        Icon(Icons.Default.ZoomIn, null)
                    }
                    TextButton(onClick = { viewModel.setZoomScale(1.0f) }) {
                        Text("Reset Zoom", fontSize = 11.sp)
                    }
                }

                TextButton(onClick = { showRestoreDialog = true }) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(Modifier.width(4.dp))
                    Text("Restore Original", fontSize = 11.sp, color = Color.Red)
                }
            }

            // Main A4 Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                val activeConfig = state.aiPreviewConfig ?: state.wizardConfig
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f * state.zoomScale)
                        .aspectRatio(1f / 1.414f),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val scope = this
                        val pageWidth = scope.maxWidth.value * LocalContext.current.resources.displayMetrics.density
                        val pageHeight = scope.maxHeight.value * LocalContext.current.resources.displayMetrics.density

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        textZoom = 100
                                    }
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                    webViewClient = WebViewClient()
                                }
                            },
                            update = { webView ->
                                val currentHtml = previewHtml ?: ""
                                if (webView.tag != currentHtml) {
                                    webView.tag = currentHtml
                                    webView.loadDataWithBaseURL("file:///android_asset/", currentHtml, "text/html", "utf-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Overlays
                        val elements = listOf(
                            InvoiceWizardConfig.KEY_SHOP_NAME,
                            InvoiceWizardConfig.KEY_TAGLINE,
                            InvoiceWizardConfig.KEY_ADDRESS,
                            InvoiceWizardConfig.KEY_PHONE,
                            InvoiceWizardConfig.KEY_GSTIN,
                            InvoiceWizardConfig.KEY_LOGO,
                            InvoiceWizardConfig.KEY_GOD_IMAGE,
                            InvoiceWizardConfig.KEY_QR_CODE,
                            InvoiceWizardConfig.KEY_SIGNATURE_BLOCK,
                            InvoiceWizardConfig.KEY_STAMP,
                            InvoiceWizardConfig.KEY_PRODUCT_TABLE,
                            InvoiceWizardConfig.KEY_TOTALS_BOX,
                            InvoiceWizardConfig.KEY_THANK_YOU,
                            InvoiceWizardConfig.KEY_FOOTER,
                            InvoiceWizardConfig.KEY_WATERMARK
                        )

                        elements.forEach { key ->
                            val layout = activeConfig.getLayout(key)
                            if (layout.visible) {
                                val isLocked = state.lockedKeys.contains(key)

                                InteractiveCanvasOverlay(
                                    key = key,
                                    layout = layout,
                                    pageWidth = pageWidth,
                                    pageHeight = pageHeight,
                                    isSelected = state.selectedElementKey == key,
                                    isLocked = isLocked,
                                    onSelect = { viewModel.selectElement(key) },
                                    onMove = { dx, dy ->
                                        if (!isLocked) {
                                            val newX = (layout.xPercent + (dx / pageWidth) * 100).coerceIn(0f, 95f)
                                            val newY = (layout.yPercent + (dy / pageHeight) * 100).coerceIn(0f, 95f)
                                            viewModel.updateElementLayout(key) { it.copy(xPercent = newX, yPercent = newY) }
                                        }
                                    },
                                    onResize = { dw, dh ->
                                        if (!isLocked) {
                                            val newW = (layout.widthPercent + (dw / pageWidth) * 100).coerceIn(5f, 100f)
                                            val newH = (layout.heightPercent + (dh / pageHeight) * 100).coerceIn(2f, 100f)
                                            viewModel.updateElementLayout(key) { it.copy(widthPercent = newW, heightPercent = newH) }
                                        }
                                    },
                                    onDragEnd = { viewModel.recordUndo() }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Contextual Control Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Element Selector Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        val elementKeys = listOf(
                            InvoiceWizardConfig.KEY_SHOP_NAME,
                            InvoiceWizardConfig.KEY_ADDRESS,
                            InvoiceWizardConfig.KEY_PHONE,
                            InvoiceWizardConfig.KEY_LOGO,
                            InvoiceWizardConfig.KEY_GOD_IMAGE,
                            InvoiceWizardConfig.KEY_QR_CODE,
                            InvoiceWizardConfig.KEY_SIGNATURE_BLOCK,
                            InvoiceWizardConfig.KEY_PRODUCT_TABLE,
                            InvoiceWizardConfig.KEY_TOTALS_BOX,
                            InvoiceWizardConfig.KEY_THANK_YOU,
                            InvoiceWizardConfig.KEY_FOOTER
                        )

                        items(elementKeys) { key ->
                            val isSelected = state.selectedElementKey == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectElement(key) },
                                label = { Text(getFriendlyName(key), fontSize = 11.sp) }
                            )
                        }
                    }

                    // Selected Element Contextual Tools
                    state.selectedElementKey?.let { selectedKey ->
                        val layout = state.wizardConfig.getLayout(selectedKey)
                        val isLocked = state.lockedKeys.contains(selectedKey)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(getFriendlyName(selectedKey), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                    Row {
                                        IconButton(onClick = { viewModel.toggleLockElement(selectedKey) }) {
                                            Icon(
                                                if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock",
                                                tint = if (isLocked) Color.Red else Color.Gray
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleVisibility(selectedKey) }) {
                                            Icon(
                                                if (layout.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Visibility"
                                            )
                                        }
                                        IconButton(onClick = { viewModel.selectElement(null) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }
                                }

                                if (!isLocked && layout.visible) {
                                    // Align Row
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = { viewModel.alignElement(selectedKey, "LEFT") }, modifier = Modifier.weight(1f)) {
                                            Text("Left", fontSize = 10.sp)
                                        }
                                        OutlinedButton(onClick = { viewModel.alignElement(selectedKey, "CENTER_HORIZON") }, modifier = Modifier.weight(1f)) {
                                            Text("Center", fontSize = 10.sp)
                                        }
                                        OutlinedButton(onClick = { viewModel.alignElement(selectedKey, "RIGHT") }, modifier = Modifier.weight(1f)) {
                                            Text("Right", fontSize = 10.sp)
                                        }
                                    }

                                    // Styling Controls if text
                                    if (selectedKey in listOf(
                                            InvoiceWizardConfig.KEY_SHOP_NAME,
                                            InvoiceWizardConfig.KEY_TAGLINE,
                                            InvoiceWizardConfig.KEY_ADDRESS,
                                            InvoiceWizardConfig.KEY_PHONE,
                                            InvoiceWizardConfig.KEY_THANK_YOU
                                        )) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            FilterChip(
                                                selected = layout.fontWeight == "BOLD",
                                                onClick = {
                                                    viewModel.applyLayoutChange(selectedKey) {
                                                        it.copy(fontWeight = if (it.fontWeight == "BOLD") "NORMAL" else "BOLD")
                                                    }
                                                },
                                                label = { Text("Bold", fontSize = 11.sp) }
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = {
                                                    viewModel.applyLayoutChange(selectedKey) { it.copy(fontSize = (it.fontSize - 2f).coerceAtLeast(8f)) }
                                                }) { Icon(Icons.Default.Remove, null) }
                                                Text("Font ${layout.fontSize.toInt()}pt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = {
                                                    viewModel.applyLayoutChange(selectedKey) { it.copy(fontSize = (it.fontSize + 2f).coerceAtMost(60f)) }
                                                }) { Icon(Icons.Default.Add, null) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Designer Bottom Sheet
    if (showAiSheet) {
        AiDesignerSheet(
            viewModel = viewModel,
            isProcessing = state.isAiProcessing,
            onDismiss = { showAiSheet = false }
        )
    }
}

@Composable
fun InteractiveCanvasOverlay(
    key: String,
    layout: ElementLayout,
    pageWidth: Float,
    pageHeight: Float,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val x = (layout.xPercent / 100f) * pageWidth
    val y = (layout.yPercent / 100f) * pageHeight
    val width = (layout.widthPercent / 100f) * pageWidth
    val height = (layout.heightPercent / 100f) * pageHeight

    val density = LocalContext.current.resources.displayMetrics.density

    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(
                width = if (width > 0) (width / density).dp else 40.dp,
                height = if (height > 0) (height / density).dp else 20.dp
            )
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() }
                )
            }
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF6366F1), RoundedCornerShape(2.dp))
                else Modifier.border(1.dp, Color.Blue.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            )
            .zIndex(if (isSelected) 100f else layout.zIndex.toFloat())
            .clickable { onSelect() }
    ) {
        if (isSelected) {
            // Selection Badge
            Surface(
                color = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = RoundedCornerShape(bottomEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = getFriendlyName(key),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom-Right Corner Resize Handle
            if (!isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(16.dp)
                        .background(Color(0xFF6366F1), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onResize(dragAmount.x, dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() }
                            )
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDesignerSheet(
    viewModel: InvoiceProfileViewModel,
    isProcessing: Boolean,
    onDismiss: () -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("✨ AI Invoice Designer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Text(
                "Describe how you want your invoice designed in natural language (English/Telugu/Hindi).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Preset Chips
            Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Professional", "Minimal", "Premium", "Traditional Mandi", "Compact")) { preset ->
                    OutlinedButton(
                        onClick = {
                            viewModel.processAiDesignRequest(preset)
                            onDismiss()
                        }
                    ) {
                        Text(preset, fontSize = 12.sp)
                    }
                }
            }

            // Prompt Input
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("E.g., Make logo bigger and move QR to bottom right") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            viewModel.processAiDesignRequest(promptInput)
                            onDismiss()
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Design with AI")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun getFriendlyName(key: String): String {
    return when (key) {
        InvoiceWizardConfig.KEY_SHOP_NAME -> "Shop Name"
        InvoiceWizardConfig.KEY_TAGLINE -> "Tagline"
        InvoiceWizardConfig.KEY_ADDRESS -> "Address"
        InvoiceWizardConfig.KEY_PHONE -> "Phone"
        InvoiceWizardConfig.KEY_GSTIN -> "GSTIN"
        InvoiceWizardConfig.KEY_LOGO -> "Logo"
        InvoiceWizardConfig.KEY_GOD_IMAGE -> "God Image"
        InvoiceWizardConfig.KEY_QR_CODE -> "QR Code"
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK -> "Signature"
        InvoiceWizardConfig.KEY_STAMP -> "Stamp"
        InvoiceWizardConfig.KEY_PRODUCT_TABLE -> "Product Table"
        InvoiceWizardConfig.KEY_TOTALS_BOX -> "Totals Box"
        InvoiceWizardConfig.KEY_THANK_YOU -> "Thank You"
        InvoiceWizardConfig.KEY_FOOTER -> "Footer"
        InvoiceWizardConfig.KEY_WATERMARK -> "Watermark"
        else -> key
    }
}
