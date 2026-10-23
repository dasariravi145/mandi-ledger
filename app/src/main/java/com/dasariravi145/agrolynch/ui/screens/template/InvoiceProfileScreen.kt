package com.dasariravi145.agrolynch.ui.screens.template

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvoiceProfileScreen(
    viewModel: InvoiceProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewHtml by viewModel.previewHtml.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    var showDesignerMode by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler {
        if (showDesignerMode) {
            showDesignerMode = false
        } else if (state.isDirty) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
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

    if (showPremiumDialog) {
        com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog(
            title = "Customize Invoice (Premium)",
            message = "Visually customize invoice elements, drag & resize logo, QR, signature, adjust styling and use AI Design assistant! Upgrade to Premium to unlock.",
            onDismiss = { showPremiumDialog = false },
            onUpgradeClick = { showPremiumDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (showDesignerMode) {
        // Full screen overlay OUTSIDE the verticalScroll Column
        Box(modifier = Modifier.fillMaxSize().zIndex(999f)) {
            InvoiceDesignerScreen(
                viewModel = viewModel,
                onBack = { showDesignerMode = false }
            )
        }
    } else {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Preview
                    Box(Modifier.fillMaxWidth()) {
                        HtmlPreviewCard(
                            html = previewHtml ?: "", 
                            templateName = state.selectedTemplateId,
                            isEditMode = false,
                            config = state.wizardConfig,
                            selectedKey = state.selectedElementKey,
                            onElementSelect = { },
                            onElementMove = { _, _, _ -> },
                            onDragEnd = { }
                        )
                    }

                    SectionTitle("1. Select Invoice Template")
                    TemplateSelectionSection(state.selectedTemplateId) { viewModel.onTemplateSelected(it) }

                    // Customize Invoice Premium Feature Entry Point
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPremium) {
                                    showDesignerMode = true
                                } else {
                                    showPremiumDialog = true
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                        border = BorderStroke(1.dp, Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Customize Invoice Layout",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF312E81)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Drag, resize, style elements & AI Design assistant",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Surface(
                                color = if (isPremium) Color(0xFF6366F1) else Color(0xFFFF9800),
                                contentColor = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = if (isPremium) "DESIGN" else "PREMIUM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    SectionTitle("2. Business Details")
                    BusinessDetailsSection(state.profile) { viewModel.onBusinessDetailChanged(it) }

                    SectionTitle("3. Branding Assets")
                    BrandingAssetsSection(state.profile, viewModel)
                    
                    Spacer(modifier = Modifier.height(80.dp))
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
                    if (webView.tag != html) {
                        webView.tag = html
                        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
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
