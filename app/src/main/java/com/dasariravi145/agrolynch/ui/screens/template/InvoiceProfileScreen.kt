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
import androidx.compose.ui.platform.LocalContext
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceProfileScreen(
    viewModel: InvoiceProfileViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(profile) {
        viewModel.generateLivePreview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional Invoice Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(onClick = { 
                        viewModel.saveAll()
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, null)
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Live Preview Section
            if (previewFile != null) {
                PdfPreviewCard(previewFile!!)
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            HorizontalDivider()

            // 1. Business Information
            SectionTitle("1. Business Information")
            profile?.let { p ->
                BusinessInfoSection(p) { viewModel.updateProfile(it) }
            }

            // 2. Branding Assets
            SectionTitle("2. Branding Assets")
            BrandingAssetsSection(profile) { type, uri -> viewModel.saveAssetLocally(uri, type) }

            // 3. Template Selection
            SectionTitle("3. Template Selection")
            TemplateSelectionSection(profile?.defaultTemplate ?: "GK_FRUITS_CLASSIC") { 
                viewModel.updateProfile { p -> p.copy(defaultTemplate = it) }
            }
            
            Button(
                onClick = { viewModel.generateLivePreview() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Preview")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PdfPreviewCard(file: File) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.707f),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(64.dp), tint = Color.Red)
                Text("Pure PDF Preview Generated", fontWeight = FontWeight.Bold)
                Text("Click Save to apply this design", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun BusinessInfoSection(profile: CompanyProfileEntity, onUpdate: ((CompanyProfileEntity) -> CompanyProfileEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = profile.companyName, onValueChange = { v -> onUpdate { it.copy(companyName = v) } }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = profile.address, onValueChange = { v -> onUpdate { it.copy(address = v) } }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = profile.mobile1, onValueChange = { v -> onUpdate { it.copy(mobile1 = v) } }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = profile.gstNumber, onValueChange = { v -> onUpdate { it.copy(gstNumber = v) } }, label = { Text("GST Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = profile.tagline, onValueChange = { v -> onUpdate { it.copy(tagline = v) } }, label = { Text("Tagline") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun BrandingAssetsSection(profile: CompanyProfileEntity?, onAssetSelected: (String, android.net.Uri) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            AssetPicker("Company Logo", profile?.logoPath) { onAssetSelected("logo", it) }
            AssetPicker("God Image", profile?.godImagePath) { onAssetSelected("god", it) }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            AssetPicker("Signature", profile?.signaturePath) { onAssetSelected("signature", it) }
            AssetPicker("Company Stamp", profile?.stampPath) { onAssetSelected("stamp", it) }
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
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "file:///android_asset/templates/${template.name.lowercase()}.png",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(Modifier.height(8.dp))
            Text(template.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(if (template.isPremium) "Premium" else "Free", fontSize = 10.sp, color = if (template.isPremium) Color(0xFFFF9800) else Color(0xFF4CAF50))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
}
