package com.dasariravi145.agrolynch.ui.screens.template

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.BillTemplateType
import com.dasariravi145.agrolynch.ui.screens.settings.AssetPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceProfileScreen(
    viewModel: InvoiceProfileViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val previewHtml by viewModel.previewHtml.collectAsState()
    val scrollState = rememberScrollState()

    // Automatically refresh preview when profile or template changes
    LaunchedEffect(profile) {
        if (profile != null) {
            viewModel.generateLivePreview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional Invoice Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { 
                            viewModel.saveAll()
                            onBack()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Done, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Template Preview
            SectionTitle("Selected Template Preview")
            
            if (previewHtml != null) {
                HtmlPreviewCard(
                    html = previewHtml!!, 
                    templateName = profile?.defaultTemplate ?: "Default"
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading Preview...", color = Color.Gray)
                    }
                }
            }

            // TASK 1 & 3: Template Selection (No more business info fields here)
            SectionTitle("1. Select Invoice Template")
            TemplateSelectionSection(profile?.defaultTemplate ?: "GK_FRUITS_CLASSIC") { 
                viewModel.updateProfile { p -> p.copy(defaultTemplate = it) }
            }

            // TASK 1: Branding Assets
            SectionTitle("2. Branding Assets")
            BrandingAssetsSection(profile) { type, uri -> viewModel.saveAssetLocally(uri, type) }
            
            Button(
                onClick = { viewModel.generateLivePreview() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Force Refresh Preview")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HtmlPreviewCard(html: String, templateName: String) {
    var hasError by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(650.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color(0xFF1B5E20)) // Professional Green Border
    ) {
        if (hasError || html.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.ImageNotSupported, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Preview not available", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text(templateName, color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Check your branding assets and try again.", textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            setSupportZoom(false)
                            textZoom = 100
                        }
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                hasError = true
                            }
                        }
                    }
                },
                update = { webView ->
                    // viewport width=794 without initial-scale allows loadWithOverviewMode to fit it to screen width
                    val styledHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=794">
                            <style>
                                * { -webkit-print-color-adjust: exact; }
                                body { 
                                    margin: 0; 
                                    padding: 0; 
                                    background-color: white; 
                                    display: flex; 
                                    justify-content: center;
                                }
                                .preview-wrapper {
                                    background-color: white;
                                    width: 794px; /* Standard A4 width at 96dpi */
                                    min-height: 1123px; /* Standard A4 height */
                                    box-sizing: border-box;
                                    overflow: hidden;
                                }
                                /* Ensure images fit correctly */
                                img { max-width: 100%; height: auto; }
                            </style>
                        </head>
                        <body>
                            <div class="preview-wrapper">
                                $html
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BrandingAssetsSection(profile: CompanyProfileEntity?, onAssetSelected: (String, android.net.Uri) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                AssetPicker("Company Logo", profile?.logoPath) { onAssetSelected("logo", it) }
            }
            Box(Modifier.weight(1f)) {
                AssetPicker("God Image", profile?.godImagePath) { onAssetSelected("god", it) }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                AssetPicker("Signature", profile?.signaturePath) { onAssetSelected("signature", it) }
            }
            Box(Modifier.weight(1f)) {
                AssetPicker("Company Stamp", profile?.stampPath) { onAssetSelected("stamp", it) }
            }
        }
        AssetPicker("UPI QR Code", profile?.upiQrPath) { onAssetSelected("upi_qr", it) }
    }
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
