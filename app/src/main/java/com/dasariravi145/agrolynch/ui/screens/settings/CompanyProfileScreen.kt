package com.dasariravi145.agrolynch.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    viewModel: CompanyViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var companyName by remember { mutableStateOf("") }
    var proprietorName by remember { mutableStateOf("") }
    var mobile1 by remember { mutableStateOf("") }
    var mobile2 by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var billPrefix by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.let {
            companyName = it.companyName
            proprietorName = it.proprietorName
            mobile1 = it.mobile1
            mobile2 = it.mobile2
            address = it.address
            village = it.village
            district = it.district
            state = it.state
            gstNumber = it.gstNumber
            licenseNumber = it.licenseNumber
            billPrefix = it.billPrefix
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Company Profile & Branding") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val updated = (profile ?: CompanyProfileEntity()).copy(
                            companyName = companyName,
                            proprietorName = proprietorName,
                            mobile1 = mobile1,
                            mobile2 = mobile2,
                            address = address,
                            village = village,
                            district = district,
                            state = state,
                            gstNumber = gstNumber,
                            licenseNumber = licenseNumber,
                            billPrefix = billPrefix
                        )
                        viewModel.updateProfile(updated)
                    }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
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
            // Branding Section
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Branding Assets / బ్రాండింగ్", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AssetPicker(label = "Company Logo", path = profile?.logoPath) { viewModel.saveAssetLocally(context, it, "logo") }
                        AssetPicker(label = "God Image", path = profile?.godImagePath) { viewModel.saveAssetLocally(context, it, "god") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AssetPicker(label = "Signature", path = profile?.signaturePath) { viewModel.saveAssetLocally(context, it, "signature") }
                        AssetPicker(label = "Stamp", path = profile?.stampPath) { viewModel.saveAssetLocally(context, it, "stamp") }
                    }
                }
            }

            // Business Details
            SectionHeader("Business Details / వ్యాపార వివరాలు")
            OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = proprietorName, onValueChange = { proprietorName = it }, label = { Text("Proprietor Name") }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mobile1, onValueChange = { mobile1 = it }, label = { Text("Mobile 1") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = mobile2, onValueChange = { mobile2 = it }, label = { Text("Mobile 2") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }

            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / షాపు నంబర్") }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village/Mandi") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.weight(1f))
            }

            // Bill Settings
            SectionHeader("Bill Settings / బిల్ సెట్టింగులు")
            OutlinedTextField(value = billPrefix, onValueChange = { billPrefix = it }, label = { Text("Bill Prefix (e.g. SHOP1)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AssetPicker(label: String, path: String?, onUriSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUriSelected(it) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.FaintGray())
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (path != null) {
                Image(painter = rememberAsyncImagePainter(path), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
}

fun Color.Companion.FaintGray() = Color(0xFFF3F4F6)
