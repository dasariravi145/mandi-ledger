package com.dasariravi145.agrolynch.ui.screens.template

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.BillTemplateType
import com.dasariravi145.agrolynch.domain.model.ElementLayout
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import com.dasariravi145.agrolynch.ui.screens.settings.AssetPicker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvoiceProfileScreen(
    viewModel: InvoiceProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewHtml by viewModel.previewHtml.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler {
        if (state.isDirty) showUnsavedDialog = true
        else onBack()
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved invoice changes. What would you like to do?") },
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

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isDirty) showUnsavedDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { viewModel.undo() }, enabled = state.undoStack.isNotEmpty()) {
                            Icon(Icons.Default.Undo, "Undo")
                        }
                        IconButton(onClick = { viewModel.redo() }, enabled = state.redoStack.isNotEmpty()) {
                            Icon(Icons.Default.Redo, "Redo")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = { viewModel.saveAll() },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(8.dp))
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Business Info") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Layout Editor") })
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 1) {
                    // Editor Mode Selector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.editorMode == EditorMode.GUIDED,
                            onClick = { viewModel.setEditorMode(EditorMode.GUIDED) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Guided") }
                        SegmentedButton(
                            selected = state.editorMode == EditorMode.FREE_EDIT,
                            onClick = { viewModel.setEditorMode(EditorMode.FREE_EDIT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Free Edit") }
                        SegmentedButton(
                            selected = state.editorMode == EditorMode.REVIEW,
                            onClick = { viewModel.setEditorMode(EditorMode.REVIEW) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Review") }
                    }

                    if (state.editorMode == EditorMode.GUIDED) {
                        GuidedEditorHeader(state, viewModel)
                    }
                }

                // Live Preview & Editor
                Box(Modifier.fillMaxWidth()) {
                    HtmlPreviewCard(
                        html = previewHtml ?: "", 
                        templateName = state.selectedTemplateId,
                        isEditMode = selectedTab == 1 && state.editorMode != EditorMode.REVIEW,
                        editorMode = state.editorMode,
                        config = state.wizardConfig,
                        selectedKey = state.selectedElementKey,
                        onElementSelect = { viewModel.selectElement(it) },
                        onElementMove = { key, x, y -> viewModel.updateElementLayout(key) { it.copy(xPercent = x, yPercent = y) } },
                        onDragEnd = { viewModel.recordUndo() }
                    )
                }

                if (selectedTab == 0) {
                    SectionTitle("1. Select Invoice Template")
                    TemplateSelectionSection(state.selectedTemplateId) { viewModel.onTemplateSelected(it) }

                    SectionTitle("2. Business Details")
                    BusinessDetailsSection(state.profile) { viewModel.onBusinessDetailChanged(it) }

                    SectionTitle("3. Branding Assets")
                    BrandingAssetsSection(state.profile, viewModel)
                } else {
                    if (state.editorMode != EditorMode.REVIEW) {
                        SectionTitle("Element Customization")
                        if (state.selectedElementKey != null) {
                            ElementEditorControls(
                                key = state.selectedElementKey!!,
                                config = state.wizardConfig,
                                viewModel = viewModel
                            )
                        } else if (state.editorMode != EditorMode.REVIEW) {
                            PageStyleControls(state.wizardConfig, viewModel)
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { viewModel.resetToDefault() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.RestartAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reset Template Layout")
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.autoArrangeLayout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Auto Arrange Layout")
                        }
                    } else {
                        ReviewModeActions(viewModel)
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp)) // Padding for sticky bottom bar if any
            }
        }
    }
}

@Composable
fun GuidedEditorHeader(state: ProfessionalInvoiceUiState, viewModel: InvoiceProfileViewModel) {
    val visibleElements = viewModel.guidedElementOrder.filter { state.wizardConfig.getLayout(it).visible }
    val currentStep = visibleElements.indexOf(state.selectedElementKey) + 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Step $currentStep of ${visibleElements.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                ElementNavigatorDropdown(state, viewModel)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Place: ${getFriendlyName(state.selectedElementKey ?: "")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Recommended: ${getRecommendation(state.selectedElementKey ?: "")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.weight(1f),
                    enabled = currentStep > 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                    Text("Back")
                }
                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(if (currentStep == visibleElements.size) "Finish" else "Next")
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                }
                TextButton(onClick = { viewModel.skipStep() }) {
                    Text("Skip")
                }
            }
        }
    }
}

private fun getRecommendation(key: String): String {
    return when(key) {
        InvoiceWizardConfig.KEY_SHOP_NAME -> "Top Center (stacked below God Image)"
        InvoiceWizardConfig.KEY_TAGLINE -> "Below Shop Name"
        InvoiceWizardConfig.KEY_ADDRESS -> "Top Left"
        InvoiceWizardConfig.KEY_PHONE -> "Top Left (below address)"
        InvoiceWizardConfig.KEY_GSTIN -> "Top Left (below phone)"
        InvoiceWizardConfig.KEY_LOGO -> "Top Right"
        InvoiceWizardConfig.KEY_GOD_IMAGE -> "Top Center"
        InvoiceWizardConfig.KEY_PRODUCT_TABLE -> "Below Metadata Panel"
        InvoiceWizardConfig.KEY_TOTALS_BOX -> "Bottom Right"
        InvoiceWizardConfig.KEY_QR_CODE -> "Bottom Left"
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK -> "Bottom Right (below totals)"
        InvoiceWizardConfig.KEY_STAMP -> "Beside Signature"
        InvoiceWizardConfig.KEY_THANK_YOU -> "Bottom Center"
        InvoiceWizardConfig.KEY_FOOTER -> "Bottom Center"
        InvoiceWizardConfig.KEY_WATERMARK -> "Page Center (low opacity)"
        else -> "Recommended position"
    }
}

@Composable
fun ElementNavigatorDropdown(state: ProfessionalInvoiceUiState, viewModel: InvoiceProfileViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("All Elements")
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            viewModel.guidedElementOrder.forEach { key ->
                DropdownMenuItem(
                    text = { Text(getFriendlyName(key)) },
                    onClick = {
                        viewModel.selectElement(key)
                        expanded = false
                    },
                    leadingIcon = {
                        if (state.selectedElementKey == key) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

@Composable
fun ReviewModeActions(viewModel: InvoiceProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Layout Placement Completed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Review your invoice preview above. If everything looks good, save your template.", textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.setEditorMode(EditorMode.GUIDED) }, modifier = Modifier.weight(1f)) {
                    Text("Edit Again")
                }
                Button(onClick = { viewModel.saveAll() }, modifier = Modifier.weight(1f)) {
                    Text("Save Template")
                }
            }
        }
    }
}

@Composable
fun HtmlPreviewCard(
    html: String, 
    templateName: String,
    isEditMode: Boolean = false,
    editorMode: EditorMode = EditorMode.GUIDED,
    config: InvoiceWizardConfig? = null,
    selectedKey: String? = null,
    onElementSelect: (String) -> Unit = {},
    onElementMove: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    var hasError by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.414f), 
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.LightGray) 
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val scope = this
            val pageWidth = scope.maxWidth.value * LocalContext.current.resources.displayMetrics.density
            val pageHeight = scope.maxHeight.value * LocalContext.current.resources.displayMetrics.density
            
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            textZoom = 100
                        }
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                hasError = true
                            }
                        }
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isEditMode && config != null) {
                val elements = listOf(
                    InvoiceWizardConfig.KEY_SHOP_NAME,
                    InvoiceWizardConfig.KEY_TAGLINE,
                    InvoiceWizardConfig.KEY_ADDRESS,
                    InvoiceWizardConfig.KEY_PHONE,
                    InvoiceWizardConfig.KEY_GSTIN,
                    InvoiceWizardConfig.KEY_LOGO,
                    InvoiceWizardConfig.KEY_QR_CODE,
                    InvoiceWizardConfig.KEY_THANK_YOU,
                    InvoiceWizardConfig.KEY_SIGNATURE_BLOCK,
                    InvoiceWizardConfig.KEY_STAMP,
                    InvoiceWizardConfig.KEY_WATERMARK,
                    InvoiceWizardConfig.KEY_GOD_IMAGE,
                    InvoiceWizardConfig.KEY_AUTHORIZED_LABEL
                )
                
                elements.forEach { key ->
                    val layout = config.getLayout(key)
                    if (layout.visible) {
                        val isSelectable = if (editorMode == EditorMode.GUIDED) key == selectedKey else true
                        
                        DraggableElementOverlay(
                            key = key,
                            layout = layout,
                            pageWidth = pageWidth,
                            pageHeight = pageHeight,
                            isSelected = selectedKey == key,
                            isSelectable = isSelectable,
                            onSelect = { onElementSelect(key) },
                            onMove = { dx, dy -> 
                                if (isSelectable) {
                                    val newX = (layout.xPercent + (dx / pageWidth) * 100).coerceIn(0f, 100f)
                                    val newY = (layout.yPercent + (dy / pageHeight) * 100).coerceIn(0f, 100f)
                                    onElementMove(key, newX, newY)
                                }
                            },
                            onDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableElementOverlay(
    key: String,
    layout: ElementLayout,
    pageWidth: Float,
    pageHeight: Float,
    isSelected: Boolean,
    isSelectable: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val x = (layout.xPercent / 100f) * pageWidth
    val y = (layout.yPercent / 100f) * pageHeight
    val width = (layout.widthPercent / 100f) * pageWidth
    val height = (layout.heightPercent / 100f) * pageHeight
    
    val density = LocalContext.current.resources.displayMetrics.density

    // Account for border in overlay
    val borderThicknessPx = when(layout.borderThickness) {
        "THIN" -> 1 * density
        "MEDIUM" -> 2 * density
        "THICK" -> 4 * density
        else -> 0f
    }
    
    val paddingPx = when(layout.padding) {
        "COMPACT" -> 2 * density
        "NORMAL" -> 6 * density
        "SPACIOUS" -> 12 * density
        else -> 0f
    }

    // Touch target padding: Add extra 8dp for easier grabbing
    val touchBufferPx = 8 * density

    Box(
        modifier = Modifier
            .offset { IntOffset((x - (if(isSelectable) touchBufferPx else 0f)).roundToInt(), (y - (if(isSelectable) touchBufferPx else 0f)).roundToInt()) }
            .size(
                width = if (width > 0) ((width + (if(isSelectable) touchBufferPx * 2 else 0f)) / density).dp else 40.dp,
                height = if (height > 0) ((height + (if(isSelectable) touchBufferPx * 2 else 0f)) / density).dp else 20.dp
            )
            .pointerInput(isSelectable) {
                if (!isSelectable) return@pointerInput
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() }
                )
            }
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFF16A34A), RoundedCornerShape(2.dp)) else Modifier)
            .then(if (isSelectable && !isSelected) Modifier.border(1.dp, Color.Blue.copy(alpha = 0.2f), RoundedCornerShape(2.dp)) else Modifier)
            .zIndex(if (isSelected) 100f else layout.zIndex.toFloat())
            .then(if (layout.borderThickness != "NONE") {
                val bw = when(layout.borderThickness) {
                    "THIN" -> 1.dp
                    "MEDIUM" -> 2.dp
                    "THICK" -> 4.dp
                    else -> 0.dp
                }
                val br = when(layout.cornerStyle) {
                    "SLIGHTLY_ROUNDED" -> 4.dp
                    "ROUNDED" -> 12.dp
                    else -> 0.dp
                }
                Modifier.padding(touchBufferPx.dp / density).border(bw, Color.Black, RoundedCornerShape(br))
            } else Modifier)
            .clickable(enabled = isSelectable) { onSelect() }
    ) {
        if (isSelected) {
            // Drag handle icon in corner
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(24.dp)
                    .background(Color(0xFF16A34A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OpenWith, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Surface(
                color = Color(0xFF16A34A),
                contentColor = Color.White,
                shape = RoundedCornerShape(bottomEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopStart).offset(x = (if(isSelectable) 8.dp else 0.dp), y = (if(isSelectable) 8.dp else 0.dp))
            ) {
                Text(
                    text = getFriendlyName(key),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ElementEditorControls(
    key: String,
    config: InvoiceWizardConfig,
    viewModel: InvoiceProfileViewModel
) {
    val layout = config.getLayout(key)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(getFriendlyName(key), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { viewModel.selectElement(null) }) {
                    Icon(Icons.Default.Close, null)
                }
            }
            
            ToggleRow("Show on Invoice", layout.visible) { v -> viewModel.updateElementLayout(key) { it.copy(visible = v) } }
            
            if (layout.visible) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Recommended Position
                Button(
                    onClick = { viewModel.applyLayoutChange(key) { it.copy(xPercent = getDefaultX(key), yPercent = getDefaultY(key)) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Place, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Move to Recommended Position")
                }

                // Move Controls
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Element Position", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    // Position Presets
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PresetButton("TL", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 5f, yPercent = 5f) } }
                        PresetButton("TC", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 40f, yPercent = 5f) } }
                        PresetButton("TR", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 80f, yPercent = 5f) } }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PresetButton("BL", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 5f, yPercent = 85f) } }
                        PresetButton("BC", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 40f, yPercent = 85f) } }
                        PresetButton("BR", Modifier.weight(1f)) { viewModel.applyLayoutChange(key) { it.copy(xPercent = 80f, yPercent = 85f) } }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { viewModel.applyLayoutChange(key) { it.copy(xPercent = (it.xPercent - 1).coerceAtLeast(0f)) } }) {
                            Icon(Icons.Default.ArrowBack, "Left")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.applyLayoutChange(key) { it.copy(yPercent = (it.yPercent - 1).coerceAtLeast(0f)) } }) {
                                Icon(Icons.Default.ArrowUpward, "Up")
                            }
                            IconButton(onClick = { viewModel.applyLayoutChange(key) { it.copy(yPercent = (it.yPercent + 1).coerceAtMost(100f)) } }) {
                                Icon(Icons.Default.ArrowDownward, "Down")
                            }
                        }
                        IconButton(onClick = { viewModel.applyLayoutChange(key) { it.copy(xPercent = (it.xPercent + 1).coerceAtMost(100f)) } }) {
                            Icon(Icons.Default.ArrowForward, "Right")
                        }
                    }
                }

                // Size Controls
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            viewModel.applyLayoutChange(key) { 
                                val step = if (key.contains("TABLE") || key.contains("BOX")) 5f else 2f
                                it.copy(widthPercent = (it.widthPercent - step).coerceAtLeast(5f)) 
                            }
                        }) { Icon(Icons.Default.Remove, null) }
                        
                        Text("Adjust", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        
                        IconButton(onClick = { 
                            viewModel.applyLayoutChange(key) { 
                                val step = if (key.contains("TABLE") || key.contains("BOX")) 5f else 2f
                                it.copy(widthPercent = (it.widthPercent + step).coerceAtMost(100f)) 
                            }
                        }) { Icon(Icons.Default.Add, null) }
                    }
                }

                if (key in listOf(InvoiceWizardConfig.KEY_SHOP_NAME, InvoiceWizardConfig.KEY_TAGLINE, InvoiceWizardConfig.KEY_ADDRESS, InvoiceWizardConfig.KEY_PHONE)) {
                    Text("Text Alignment", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    AlignmentRow(selected = layout.alignment) { a -> viewModel.applyLayoutChange(key) { it.copy(alignment = a) } }
                }

                if (key in listOf(InvoiceWizardConfig.KEY_LOGO, InvoiceWizardConfig.KEY_GOD_IMAGE, InvoiceWizardConfig.KEY_SIGNATURE_BLOCK, InvoiceWizardConfig.KEY_STAMP, InvoiceWizardConfig.KEY_WATERMARK)) {
                    if (key != InvoiceWizardConfig.KEY_QR_CODE) {
                        ShapeRow("Image Shape", layout.shape, listOf("ORIGINAL", "ROUNDED", "CIRCLE", "DIAMOND")) { s -> viewModel.applyLayoutChange(key) { it.copy(shape = s) } }
                    } else {
                        // QR Code specific settings
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("QR Settings", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        
                        ShapeRow("QR Size", config.qrSizeOption, listOf("SMALL", "MEDIUM", "LARGE")) { s ->
                             viewModel.onWizardConfigChanged { it.copy(qrSizeOption = s) }
                        }
                        
                        ToggleRow("Show 'SCAN TO PAY'", config.showQrLabel) { v ->
                            viewModel.onWizardConfigChanged { it.copy(showQrLabel = v) }
                        }
                        
                        if (config.showQrLabel) {
                            ShapeRow("Label Style", config.qrLabelStyle, listOf("NORMAL", "BOLD")) { s ->
                                viewModel.onWizardConfigChanged { it.copy(qrLabelStyle = s) }
                            }
                            
                            ShapeRow("Label Colour", config.qrLabelColorOption, listOf("DEFAULT", "BLACK", "TEMPLATE")) { s ->
                                viewModel.onWizardConfigChanged { it.copy(qrLabelColorOption = s) }
                            }
                        }
                    }
                }

                // Text Style Controls
                if (key in listOf(
                        InvoiceWizardConfig.KEY_SHOP_NAME,
                        InvoiceWizardConfig.KEY_TAGLINE,
                        InvoiceWizardConfig.KEY_ADDRESS,
                        InvoiceWizardConfig.KEY_PHONE,
                        InvoiceWizardConfig.KEY_GSTIN,
                        InvoiceWizardConfig.KEY_THANK_YOU,
                        InvoiceWizardConfig.KEY_FOOTER,
                        InvoiceWizardConfig.KEY_AUTHORIZED_LABEL
                    )) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Text Style", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = layout.fontWeight == "NORMAL",
                            onClick = { viewModel.applyLayoutChange(key) { it.copy(fontWeight = "NORMAL") } },
                            label = { Text("Normal") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = layout.fontWeight == "BOLD",
                            onClick = { viewModel.applyLayoutChange(key) { it.copy(fontWeight = "BOLD") } },
                            label = { Text("Bold") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    ShapeRow("Text Colour", layout.textColorOption, listOf("DEFAULT", "BLACK", "TEMPLATE")) { s ->
                        viewModel.applyLayoutChange(key) { it.copy(textColorOption = s) }
                    }
                }

                // Section Style Controls (for Customer/Bill Info - Legacy, now template-fixed)
                // Removed structural element editing logic as per new requirements

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Position & Size", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                TextButton(
                    onClick = { viewModel.resetElement(key) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset This Element")
                }
            }
        }
    }
}

private fun getFriendlyName(key: String): String {
    return when(key) {
        InvoiceWizardConfig.KEY_SHOP_NAME -> "Shop Name"
        InvoiceWizardConfig.KEY_TAGLINE -> "Tagline"
        InvoiceWizardConfig.KEY_ADDRESS -> "Business Address"
        InvoiceWizardConfig.KEY_PHONE -> "Phone Numbers"
        InvoiceWizardConfig.KEY_GSTIN -> "GSTIN Number"
        InvoiceWizardConfig.KEY_LOGO -> "Company Logo"
        InvoiceWizardConfig.KEY_GOD_IMAGE -> "God Image"
        InvoiceWizardConfig.KEY_BILL_INFO -> "Bill Information"
        InvoiceWizardConfig.KEY_CUSTOMER_INFO -> "Customer Details"
        InvoiceWizardConfig.KEY_PRODUCT_TABLE -> "Product Table"
        InvoiceWizardConfig.KEY_TOTALS_BOX -> "Totals Box"
        InvoiceWizardConfig.KEY_QR_CODE -> "QR Code"
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK -> "Signature"
        InvoiceWizardConfig.KEY_STAMP -> "Company Stamp"
        InvoiceWizardConfig.KEY_THANK_YOU -> "Thank You Text"
        InvoiceWizardConfig.KEY_FOOTER -> "Footer Strip"
        InvoiceWizardConfig.KEY_WATERMARK -> "Watermark"
        else -> key
    }
}

private fun getDefaultX(key: String): Float {
    return when (key) {
        InvoiceWizardConfig.KEY_GOD_IMAGE -> 40f
        InvoiceWizardConfig.KEY_SHOP_NAME -> 10f
        InvoiceWizardConfig.KEY_TAGLINE -> 10f
        InvoiceWizardConfig.KEY_ADDRESS -> 5f
        InvoiceWizardConfig.KEY_PHONE -> 5f
        InvoiceWizardConfig.KEY_GSTIN -> 5f
        InvoiceWizardConfig.KEY_LOGO -> 80f
        InvoiceWizardConfig.KEY_PRODUCT_TABLE -> 5f
        InvoiceWizardConfig.KEY_QR_CODE -> 5f
        InvoiceWizardConfig.KEY_THANK_YOU -> 25f
        InvoiceWizardConfig.KEY_TOTALS_BOX -> 60f
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK -> 70f
        InvoiceWizardConfig.KEY_STAMP -> 55f
        InvoiceWizardConfig.KEY_FOOTER -> 0f
        InvoiceWizardConfig.KEY_WATERMARK -> 20f
        else -> 0f
    }
}

private fun getDefaultY(key: String): Float {
    return when (key) {
        InvoiceWizardConfig.KEY_GOD_IMAGE -> 2f
        InvoiceWizardConfig.KEY_SHOP_NAME -> 12f
        InvoiceWizardConfig.KEY_TAGLINE -> 20f
        InvoiceWizardConfig.KEY_ADDRESS -> 5f
        InvoiceWizardConfig.KEY_PHONE -> 15f
        InvoiceWizardConfig.KEY_GSTIN -> 18f
        InvoiceWizardConfig.KEY_LOGO -> 5f
        InvoiceWizardConfig.KEY_PRODUCT_TABLE -> 30f
        InvoiceWizardConfig.KEY_QR_CODE -> 75f
        InvoiceWizardConfig.KEY_THANK_YOU -> 80f
        InvoiceWizardConfig.KEY_TOTALS_BOX -> 70f
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK -> 90f
        InvoiceWizardConfig.KEY_STAMP -> 88f
        InvoiceWizardConfig.KEY_FOOTER -> 97f
        InvoiceWizardConfig.KEY_WATERMARK -> 30f
        else -> 0f
    }
}

@Composable
fun BusinessDetailsSection(profile: CompanyProfileEntity, onUpdate: ((CompanyProfileEntity) -> CompanyProfileEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = profile.companyName,
            onValueChange = { newValue -> onUpdate { it.copy(companyName = newValue) } },
            label = { Text("Company/Shop Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = profile.tagline,
            onValueChange = { newValue -> onUpdate { it.copy(tagline = newValue) } },
            label = { Text("Tagline / Footer Text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = profile.proprietorName,
            onValueChange = { newValue -> onUpdate { it.copy(proprietorName = newValue) } },
            label = { Text("Proprietor Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = profile.marketName,
                onValueChange = { newValue -> onUpdate { it.copy(marketName = newValue) } },
                label = { Text("Market Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = profile.city,
                onValueChange = { newValue -> onUpdate { it.copy(city = newValue) } },
                label = { Text("City") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = profile.address,
            onValueChange = { newValue -> onUpdate { it.copy(address = newValue) } },
            label = { Text("Complete Address") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = profile.mobile1,
                onValueChange = { input ->
                    val sanitized = input.filter { it.isDigit() }.take(10)
                    onUpdate { it.copy(mobile1 = sanitized) }
                },
                label = { Text("Mobile 1") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = profile.mobile1.isNotEmpty() && profile.mobile1.length != 10,
                supportingText = {
                    if (profile.mobile1.isNotEmpty() && profile.mobile1.length != 10) {
                        Text("Enter a valid 10-digit mobile number.", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )
            
            OutlinedTextField(
                value = profile.mobile2,
                onValueChange = { input ->
                    val sanitized = input.filter { it.isDigit() }.take(10)
                    onUpdate { it.copy(mobile2 = sanitized) }
                },
                label = { Text("Mobile 2 (Optional)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = profile.mobile2.isNotEmpty() && profile.mobile2.length != 10,
                supportingText = {
                    if (profile.mobile2.isNotEmpty() && profile.mobile2.length != 10) {
                        Text("Mobile 2 must contain 10 digits.", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )
        }
        
        OutlinedTextField(
            value = profile.gstNumber,
            onValueChange = { newValue -> onUpdate { it.copy(gstNumber = newValue) } },
            label = { Text("GSTIN (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun BrandingAssetsSection(profile: CompanyProfileEntity, viewModel: InvoiceProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                AssetPicker("Company Logo", profile.logoPath, onRemove = { viewModel.removeAsset("logo") }) { viewModel.saveAssetLocally(it, "logo") }
            }
            Box(Modifier.weight(1f)) {
                AssetPicker("God Image", profile.godImagePath, onRemove = { viewModel.removeAsset("god") }) { viewModel.saveAssetLocally(it, "god") }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                AssetPicker("Signature", profile.signaturePath, onRemove = { viewModel.removeAsset("signature") }) { viewModel.saveAssetLocally(it, "signature") }
            }
            Box(Modifier.weight(1f)) {
                AssetPicker("Company Stamp", profile.stampPath, onRemove = { viewModel.removeAsset("stamp") }) { viewModel.saveAssetLocally(it, "stamp") }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
             Box(Modifier.weight(1f)) {
                AssetPicker("UPI QR Code", profile.upiQrPath, onRemove = { viewModel.removeAsset("upi_qr") }) { viewModel.saveAssetLocally(it, "upi_qr") }
            }
            Box(Modifier.weight(1f)) {
                AssetPicker("Watermark Image", profile.customTemplatePath, onRemove = { viewModel.removeAsset("watermark") }) { viewModel.saveAssetLocally(it, "watermark") }
            }
        }
    }
}

@Composable
fun ToggleRow(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium)
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}

@Composable
fun SliderRow(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text("%.2f".format(value), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max)
    }
}

@Composable
fun AlignmentRow(label: String = "Alignment", selected: String, options: List<String> = listOf("LEFT", "CENTER", "RIGHT"), onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { align ->
                val isSelected = selected == align
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(align) },
                    label = { Text(align) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ShapeRow(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = selected == option
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(option) },
                    label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title, 
        style = MaterialTheme.typography.titleMedium, 
        fontWeight = FontWeight.ExtraBold, 
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun TemplateSelectionSection(selectedId: String, onSelected: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(BillTemplateType.entries) { template ->
            TemplateCard(template, selectedId == template.name) { onSelected(template.name) }
        }
    }
}

@Composable
fun PageStyleControls(config: InvoiceWizardConfig, viewModel: InvoiceProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Invoice Page Style", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            ShapeRow("Invoice Background", config.pageBackgroundOption, listOf("DEFAULT", "WHITE", "SOFT_TINT")) { s ->
                viewModel.onWizardConfigChanged { it.copy(pageBackgroundOption = s) }
            }
            
            ShapeRow("Page Border", config.pageBorderThickness, listOf("NONE", "THIN", "MEDIUM")) { s ->
                viewModel.onWizardConfigChanged { it.copy(pageBorderThickness = s) }
            }
            
            ShapeRow("Page Corners", config.pageCornerStyle, listOf("SQUARE", "SLIGHTLY_ROUNDED", "ROUNDED")) { s ->
                viewModel.onWizardConfigChanged { it.copy(pageCornerStyle = s) }
            }
            
            TextButton(
                onClick = { viewModel.resetPageStyle() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Page Style")
            }
        }
    }
}

@Composable
fun PresetButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TemplateCard(template: BillTemplateType, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "file:///android_asset/templates/${template.name.lowercase()}.png",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                template.displayName, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                fontSize = 12.sp, 
                textAlign = TextAlign.Center, 
                maxLines = 1,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (template.isPremium) "Premium" else "Free", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                color = if (template.isPremium) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )
        }
    }
}
