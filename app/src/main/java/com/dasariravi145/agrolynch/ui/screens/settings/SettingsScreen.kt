package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onViewProfile: () -> Unit,
    onViewBackup: () -> Unit,
    onViewSubscription: () -> Unit,
    onLanguageChanged: () -> Unit,
    onLogout: () -> Unit
) {
    val languageCode by viewModel.languageCode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings / సెట్టింగులు") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Account Section
            SettingsSectionTitle("Account / ఖాతా")
            SettingsItem(
                title = "User Profile",
                subtitle = "View and edit profile details",
                icon = Icons.Default.Person,
                onClick = onViewProfile
            )
            SettingsItem(
                title = "Subscription",
                subtitle = if (isPremium) "Premium Member" else "Upgrade for more features",
                icon = Icons.Default.Star,
                iconColor = if (isPremium) Color(0xFFFFD700) else Color.Gray,
                onClick = onViewSubscription
            )

            // Preferences Section
            SettingsSectionTitle("Preferences / ప్రాధాన్యతలు")
            SettingsItem(
                title = "Language / భాష",
                subtitle = getLanguageName(languageCode),
                icon = Icons.Default.Language,
                onClick = { showLanguageDialog = true }
            )
            SettingsToggleItem(
                title = "Dark Mode / డార్క్ మోడ్",
                subtitle = "Toggle app theme",
                icon = Icons.Default.BrightnessMedium,
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleTheme(it) }
            )

            // Data Section
            SettingsSectionTitle("Data & Security / డేటా మరియు భద్రత")
            SettingsItem(
                title = "Change PIN / పిన్ మార్చు",
                subtitle = "Security PIN for app access",
                icon = Icons.Default.Lock,
                onClick = { showChangePinDialog = true }
            )
            SettingsItem(
                title = "Backup & Reports",
                subtitle = "Manage local and cloud backups",
                icon = Icons.Default.Backup,
                onClick = onViewBackup
            )
            SettingsToggleItem(
                title = "Auto Backup",
                subtitle = "Automatic weekly PDF reports",
                icon = Icons.Default.CloudSync,
                checked = isAutoBackupEnabled,
                onCheckedChange = { viewModel.toggleAutoBackup(it) }
            )

            // System Section
            SettingsSectionTitle("System / సిస్టమ్")
            SettingsItem(
                title = "About App",
                subtitle = "Version 1.0.0",
                icon = Icons.Default.Info,
                onClick = { }
            )
            SettingsItem(
                title = "Logout / లాగ్అవుట్",
                subtitle = "Sign out from your account",
                icon = Icons.Default.Logout,
                iconColor = MaterialTheme.colorScheme.error,
                onClick = {
                    viewModel.logout()
                    onLogout()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentCode = languageCode,
            onDismiss = { showLanguageDialog = false },
            onSelect = { 
                viewModel.updateLanguage(it)
                showLanguageDialog = false
                onLanguageChanged()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Security PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("New 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm New PIN") },
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
                        error = "PIN must be 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onConfirm(pin)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        title = { Text("Select Language") },
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
