package com.dasariravi145.agrolynch.ui.screens.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dasariravi145.agrolynch.ads.BannerAdView
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val backupHistory by viewModel.backupHistory.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPremiumLockedDialog by remember { mutableStateOf(false) }
    var premiumLockedMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.fetchCloudBackups()
    }
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(isPremium) {
        android.util.Log.d("BACKUP", "BACKUP_PREMIUM_STATUS: $isPremium")
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            val finalMsg = when(msg) {
                "local_backup_saved" -> context.getString(R.string.local_backup_saved)
                "backup_complete_success" -> "Cloud Synced"
                "restore_success" -> "Data restored successfully! Please restart the app for full effect."
                else -> msg
            }
            snackbarHostState.showSnackbar(finalMsg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_reports)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isPremium) {
                item { BannerAdView() }
            }

            item {
                Text(
                    text = "Backup Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackupBox(
                        title = "Manual",
                        icon = Icons.Default.Backup,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            if (isPremium) viewModel.performManualBackup() 
                            else viewModel.createLocalBackup("MANUAL")
                        }
                    )
                    BackupBox(
                        title = "Weekly",
                        icon = if (isPremium) Icons.Default.DateRange else Icons.Default.Lock,
                        color = if (isPremium) Color(0xFF2563EB) else Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            if (isPremium) viewModel.createLocalBackup("WEEKLY") 
                            else {
                                premiumLockedMessage = "Weekly automatic backups require a Premium Subscription."
                                showPremiumLockedDialog = true
                            }
                        }
                    )
                    BackupBox(
                        title = "Monthly",
                        icon = if (isPremium) Icons.Default.CalendarMonth else Icons.Default.Lock,
                        color = if (isPremium) Color(0xFF7C3AED) else Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            if (isPremium) viewModel.createLocalBackup("MONTHLY") 
                            else {
                                premiumLockedMessage = "Monthly automatic backups require a Premium Subscription."
                                showPremiumLockedDialog = true
                            }
                        }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.backup_sync),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPremium) {
                        TextButton(onClick = { viewModel.restoreLatestCloud() }) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore Latest", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (backupHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No backup history found", color = Color.Gray)
                    }
                }
            }

            items(backupHistory) { backup ->
                BackupHistoryItem(
                    backup = backup,
                    isPremium = isPremium,
                    onDelete = { viewModel.deleteBackup(backup.id) },
                    onRestore = { 
                        if (isPremium) viewModel.restoreBackup(backup.id)
                        else {
                            premiumLockedMessage = "Premium subscription required to restore cloud backup."
                            showPremiumLockedDialog = true
                        }
                    },
                    onUpload = {
                        if (isPremium) {
                            viewModel.uploadToCloud(File(backup.filePath), backup.reportType, backup.id)
                        } else {
                            premiumLockedMessage = "Cloud upload is a premium feature."
                            showPremiumLockedDialog = true
                        }
                    }
                )
            }

            if (isPremium && cloudBackups.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Remote Cloud Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(cloudBackups) { path ->
                    CloudBackupItem(
                        path = path,
                        isPremium = isPremium,
                        onRestore = { 
                            if (isPremium) viewModel.restoreFromStoragePath(path)
                            else {
                                premiumLockedMessage = "Premium subscription required to restore cloud backup."
                                showPremiumLockedDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showPremiumLockedDialog) {
        PremiumFeatureLockedDialog(
            message = premiumLockedMessage.ifEmpty { "This feature requires a Premium Subscription." },
            onDismiss = { showPremiumLockedDialog = false },
            onUpgradeClick = {
                showPremiumLockedDialog = false
                onUpgradeClick()
            }
        )
    }
    
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun CloudBackupItem(path: String, isPremium: Boolean, onRestore: () -> Unit) {
    val fileName = path.substringAfterLast("/")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF16A34A))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = fileName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton(onClick = onRestore) {
                Text(if (isPremium) "Restore" else "Restore Premium")
            }
        }
    }
}

@Composable
fun BackupBox(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BackupHistoryItem(backup: BackupEntity, isPremium: Boolean, onDelete: () -> Unit, onRestore: () -> Unit, onUpload: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (backup.type == "CLOUD") Icons.Default.CloudDone else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (backup.type == "CLOUD") Color(0xFF16A34A) else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = backup.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(8.dp))
                        
                        Surface(
                            color = if (backup.type == "CLOUD") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = backup.type,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (backup.type == "CLOUD") Color(0xFF2E7D32) else Color(0xFF1565C0)
                            )
                        }
                    }
                    Text(
                        text = "${backup.reportType} | ${dateFormat.format(Date(backup.timestamp))} | ${Formatter.formatFileSize(backup.size)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (backup.type == "CLOUD") {
                    TextButton(
                        onClick = onRestore,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(if (isPremium) Icons.Default.SettingsBackupRestore else Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPremium) "Restore Data" else "Restore Premium", fontSize = 12.sp)
                    }
                } else {
                    TextButton(
                        onClick = onUpload,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Upload to Cloud", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
