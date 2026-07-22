package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.BuildConfig

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onViewProfile: () -> Unit,
    onViewCompanyProfile: () -> Unit,
    onViewBillSettings: () -> Unit,
    onViewBackup: () -> Unit,
    onViewSubscription: () -> Unit,
    onViewDeveloperOptions: () -> Unit,
    onLanguageChanged: () -> Unit,
    onLogout: () -> Unit
) {
    val languageCode by viewModel.languageCode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val isPremiumPopupEnabled by viewModel.isPremiumPopupEnabled.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.syncMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    
    // Developer options trigger
    var versionTapCount by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // Business Section
            SettingsSectionTitle(stringResource(R.string.business))
            SettingsItem(
                title = "Professional Invoice Setup",
                subtitle = "Manage shop name, address, logo and design",
                icon = Icons.Default.AutoFixHigh,
                onClick = { onViewCompanyProfile() }
            )
            SettingsItem(
                title = stringResource(R.string.bill_settings),
                subtitle = stringResource(R.string.bill_prefix),
                icon = Icons.Default.FormatListNumbered,
                onClick = onViewBillSettings
            )

            // Account Section
            SettingsSectionTitle("Cloud Backup — Premium")
            
            val userPhone = viewModel.userPhone.collectAsState().value
            val lastBackup = viewModel.lastBackupDate.collectAsState().value
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Cloud Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(userPhone, fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = if (isPremium) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isPremium) "PREMIUM" else "FREE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPremium) Color(0xFF2E7D32) else Color.Gray
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Last Cloud Backup: $lastBackup", fontSize = 13.sp)
                    
                    if (isPremium) {
                        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.syncNow() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                            ) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Backup to Cloud", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onViewBackup() }, // Open Backup screen for selection
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SettingsBackupRestore, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore from Cloud", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onViewSubscription,
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Upgrade to Premium for Cloud Backup")
                        }
                    }
                }
            }

            SettingsItem(
                title = stringResource(R.string.user_profile),
                subtitle = stringResource(R.string.user_profile_sub),
                icon = Icons.Default.Person,
                onClick = onViewProfile
            )
            SettingsItem(
                title = stringResource(R.string.subscription),
                subtitle = if (isPremium) stringResource(R.string.premium_member) else stringResource(R.string.upgrade_sub),
                icon = Icons.Default.Star,
                iconColor = if (isPremium) Color(0xFFFFD700) else Color.Gray,
                onClick = onViewSubscription
            )

            // Preferences Section
            SettingsSectionTitle(stringResource(R.string.preferences))
            SettingsItem(
                title = stringResource(R.string.language),
                subtitle = getLanguageName(languageCode),
                icon = Icons.Default.Language,
                onClick = { showLanguageDialog = true }
            )
            SettingsToggleItem(
                title = stringResource(R.string.dark_mode),
                subtitle = stringResource(R.string.toggle_theme),
                icon = Icons.Default.BrightnessMedium,
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleTheme(it) }
            )
            SettingsToggleItem(
                title = stringResource(R.string.show_premium_offers),
                subtitle = stringResource(R.string.show_premium_offers_sub),
                icon = Icons.Default.NotificationsActive,
                checked = isPremiumPopupEnabled,
                onCheckedChange = { viewModel.togglePremiumPopup(it) }
            )

            // Data Section
            SettingsSectionTitle(stringResource(R.string.data_security))
            SettingsItem(
                title = stringResource(R.string.change_pin),
                subtitle = stringResource(R.string.change_pin_sub),
                icon = Icons.Default.Lock,
                onClick = { showChangePinDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.backup_reports),
                subtitle = stringResource(R.string.backup_reports_sub),
                icon = Icons.Default.Backup,
                onClick = onViewBackup
            )
            SettingsToggleItem(
                title = stringResource(R.string.auto_backup),
                subtitle = stringResource(R.string.auto_backup_sub),
                icon = Icons.Default.CloudSync,
                checked = isAutoBackupEnabled,
                onCheckedChange = { viewModel.toggleAutoBackup(it) }
            )

            // System Section
            SettingsSectionTitle(stringResource(R.string.system))
            SettingsItem(
                title = stringResource(R.string.about_app),
                subtitle = "Version 1.0.0",
                icon = Icons.Default.Info,
                onClick = { 
                    if (BuildConfig.DEBUG) {
                        versionTapCount++
                        if (versionTapCount >= 7) {
                            versionTapCount = 0
                            onViewDeveloperOptions()
                        }
                    }
                }
            )
            SettingsItem(
                title = stringResource(R.string.logout),
                subtitle = stringResource(R.string.sign_out_sub),
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = MaterialTheme.colorScheme.error,
                onClick = {
                    viewModel.logout()
                    onLogout()
                }
            )

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionTitle("Testing / Debug")
                
                var isTestingPremium by remember { mutableStateOf(isPremium) }

                Button(
                    onClick = { 
                        // Note: SettingsViewModel doesn't currently have access to PremiumStateManager easily or UserRepository
                        // I might need to add a method to SettingsViewModel to handle this testing toggle
                        viewModel.togglePremiumTesting()
                        isTestingPremium = !isTestingPremium
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingPremium) Color(0xFF1B5E20) else Color.DarkGray
                    )
                ) {
                    Text(if (isTestingPremium) "Premium Active (Testing)" else "Enable Premium (Testing)", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onViewDeveloperOptions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open All Developer Options")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentCode = languageCode,
            onDismiss = { showLanguageDialog = false },
            onSelect = { selectedCode ->
                viewModel.updateLanguage(selectedCode) {
                    onLanguageChanged()
                }
                showLanguageDialog = false
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { newPin ->
                viewModel.updatePin(newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
fun ChangePinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val pinErrorDigits = stringResource(R.string.pin_error_digits)
    val pinErrorMatch = stringResource(R.string.pin_error_match)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text(stringResource(R.string.new_pin_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text(stringResource(R.string.confirm_pin_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        error = pinErrorDigits
                    } else if (pin != confirmPin) {
                        error = pinErrorMatch
                    } else {
                        onConfirm(pin)
                    }
                }
            ) {
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                LanguageOption("English", "en", currentCode == "en") { onSelect("en") }
                LanguageOption("తెలుగు (Telugu)", "te", currentCode == "te") { onSelect("te") }
                LanguageOption("हिन्दी (Hindi)", "hi", currentCode == "hi") { onSelect("hi") }
                LanguageOption("தமிழ் (Tamil)", "ta", currentCode == "ta") { onSelect("ta") }
                LanguageOption("ಕನ್ನಡ (Kannada)", "kn", currentCode == "kn") { onSelect("kn") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun LanguageOption(name: String, code: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name, fontSize = 16.sp)
    }
}

fun getLanguageName(code: String): String {
    return when(code) {
        "te" -> "తెలుగు (Telugu)"
        "hi" -> "हिन्दी (Hindi)"
        "ta" -> "தமிழ் (Tamil)"
        "kn" -> "ಕನ್ನಡ (Kannada)"
        else -> "English"
    }
}
