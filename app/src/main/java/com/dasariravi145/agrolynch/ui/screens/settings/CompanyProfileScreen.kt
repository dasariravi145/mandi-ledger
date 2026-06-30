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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.BillTemplateType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    viewModel: CompanyViewModel,
    onBack: () -> Unit,
    onEditTemplate: () -> Unit,
    onDesignTemplate: (String) -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var companyName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var proprietorName by remember { mutableStateOf("") }
    var mobile1 by remember { mutableStateOf("") }
    var mobile2 by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var marketName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var billPrefix by remember { mutableStateOf("") }
    var defaultTemplate by remember { mutableStateOf("GK_FRUITS_CLASSIC") }

    var mobile1Error by remember { mutableStateOf<String?>(null) }
    var mobile2Error by remember { mutableStateOf<String?>(null) }

    val currentTemplate = remember(defaultTemplate) { BillTemplateType.fromId(defaultTemplate) }

    LaunchedEffect(profile) {
        profile?.let {
            companyName = it.companyName
            tagline = it.tagline
            proprietorName = it.proprietorName
            mobile1 = it.mobile1
            mobile2 = it.mobile2
            address = it.address
            village = it.village
            district = it.district
            state = it.state
            pincode = it.pincode
            gstNumber = it.gstNumber
            marketName = it.marketName
            city = it.city
            upiId = it.upiId
            billPrefix = it.billPrefix
            defaultTemplate = it.defaultTemplate
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            val finalMsg = if (msg == "profile_updated_success") context.getString(R.string.profile_updated_success) else msg
            snackbarHostState.showSnackbar(finalMsg)
        }
    }

    fun validate(): Boolean {
        var isValid = true
        if (mobile1.length != 10) {
            mobile1Error = "Mobile Number must be exactly 10 digits"
            isValid = false
        } else {
            mobile1Error = null
        }

        if (mobile2.isNotEmpty() && mobile2.length != 10) {
            mobile2Error = "Alternate Mobile Number must be exactly 10 digits"
            isValid = false
        } else if (mobile2.isNotEmpty() && mobile1 == mobile2) {
            mobile2Error = "Mobile 1 and Mobile 2 cannot be the same"
            isValid = false
        } else {
            mobile2Error = null
        }

        return isValid
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Professional Bill Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (validate()) {
                            val updated = (profile ?: CompanyProfileEntity()).copy(
                                companyName = companyName,
                                tagline = tagline,
                                proprietorName = proprietorName,
                                mobile1 = mobile1,
                                mobile2 = mobile2,
                                address = address,
                                village = village,
                                district = district,
                                state = state,
                                pincode = pincode,
                                gstNumber = gstNumber,
                                marketName = marketName,
                                city = city,
                                upiId = upiId,
                                billPrefix = billPrefix,
                                defaultTemplate = defaultTemplate
                            )
                            viewModel.updateProfile(updated)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fix validation errors before saving")
                            }
                        }
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
            // 1. Professional Setup Entry
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onDesignTemplate("CURRENT") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Professional Invoice Setup", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                        Text("Click here to design your invoice with logo, signature and themes", fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            SectionHeader("Business Details")
            OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text(stringResource(R.string.company_name)) }, modifier = Modifier.fillMaxWidth())
            
            // Dynamic Tagline Label
            val taglineLabel = when(currentTemplate) {
                BillTemplateType.ROYAL_HERITAGE_MANDI -> "Royal Footer Text"
                BillTemplateType.PREMIUM_FRUIT_GALLERY -> "Footer Thank You Text"
                BillTemplateType.DIAMOND_BUSINESS_ELITE -> "Business Tagline"
                else -> "Company Tagline"
            }
            if (currentTemplate != BillTemplateType.COMPACT_THERMAL_PRINT) {
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text(taglineLabel) }, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(value = proprietorName, onValueChange = { proprietorName = it }, label = { Text(stringResource(R.string.proprietor_name)) }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = marketName, onValueChange = { marketName = it }, label = { Text("Market Name") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.weight(1f))
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = mobile1,
                    onValueChange = { 
                        val digits = it.trim().filter { char -> char.isDigit() }
                        if (digits.length <= 10) {
                            mobile1 = digits
                            if (digits.length == 10) mobile1Error = null
                        }
                    },
                    label = { Text(stringResource(R.string.mobile_1)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = mobile1Error != null,
                    supportingText = { mobile1Error?.let { Text(it) } }
                )
                OutlinedTextField(
                    value = mobile2,
                    onValueChange = { 
                        val digits = it.trim().filter { char -> char.isDigit() }
                        if (digits.length <= 10) {
                            mobile2 = digits
                            if (digits.length == 10 || digits.isEmpty()) mobile2Error = null
                        }
                    },
                    label = { Text(stringResource(R.string.mobile_2)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = mobile2Error != null,
                    supportingText = { mobile2Error?.let { Text(it) } }
                )
            }

            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(stringResource(R.string.address)) }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text(stringResource(R.string.village)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.weight(1f))
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { if(it.length <= 6) pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // GSTIN is now optional for all templates
            val gstLabel = "GSTIN (Optional)"
            OutlinedTextField(value = gstNumber, onValueChange = { gstNumber = it }, label = { Text(gstLabel) }, modifier = Modifier.fillMaxWidth())
            
            OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (for payments)") }, modifier = Modifier.fillMaxWidth())

            // Bill Settings
            SectionHeader(stringResource(R.string.bill_settings))
            OutlinedTextField(value = billPrefix, onValueChange = { billPrefix = it }, label = { Text(stringResource(R.string.bill_prefix)) }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AssetPicker(label: String, path: String?, helperText: String? = null, onUriSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUriSelected(it) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6))
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
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (helperText != null) {
            Text(helperText, fontSize = 8.sp, color = Color.Gray.copy(alpha = 0.7f), textAlign = TextAlign.Center, lineHeight = 10.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
}
