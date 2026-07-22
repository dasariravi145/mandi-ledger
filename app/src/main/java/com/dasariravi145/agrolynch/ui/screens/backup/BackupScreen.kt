package com.dasariravi145.agrolynch.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

sealed class RestoreAction {
    data class LocalUri(val uri: android.net.Uri) : RestoreAction()
    data class LocalId(val id: String) : RestoreAction()
    data class CloudPath(val path: String) : RestoreAction()
    object LatestCloud : RestoreAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onBackClick: () -> Unit,
    onOpenArchive: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val backupHistory by viewModel.backupHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPremiumLockedDialog by remember { mutableStateOf(false) }
    var premiumLockedMessage by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var showRestoreConfirmDialog by remember { mutableStateOf<RestoreAction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<BackupEntity?>(null) }
    var showPermanentDeleteDialog by remember { mutableStateOf<BackupEntity?>(null) }

    val latestLocal by remember(backupHistory) { derivedStateOf { backupHistory.find { it.id == "SLOT_LOCAL" } } }
    val latestCloud by remember(backupHistory) { derivedStateOf { backupHistory.find { it.id == "SLOT_CLOUD" } } }
    val latestSafety by remember(backupHistory) { derivedStateOf { backupHistory.find { it.id == "SLOT_SAFETY" } } }

    // SAF Launchers
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onRestoreSelected(RestoreAction.LocalUri(uri), context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchCloudBackups()
    }
    
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val showRestoreDialog by viewModel.showRestoreDialog.collectAsState()

    if (showRestoreDialog != null) {
        val preview = showRestoreDialog!!
        
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            title = { Text("Restore Backup") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (preview.isOlderThanLocal) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "Warning: This backup is older than your current data.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Accounts settled after this backup may become pending again.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text("Source: ${preview.type}", fontSize = 12.sp, color = Color.Gray)
                    Text("File: ${preview.fileName}", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(4.dp))
                    Text("Records found in backup:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Farmers: ${preview.farmerCount}", fontSize = 13.sp)
                            Text("Arrivals: ${preview.arrivalCount}", fontSize = 13.sp)
                        }
                        Column {
                            Text("Buyers: ${preview.buyerCount}", fontSize = 13.sp)
                            Text("Sales: ${preview.saleCount}", fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("This will replace all your current app data with the data from this backup.")
                    Text("A safety backup will be created automatically.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmRestore(preview, context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Create Safety Backup & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            val finalMsg = when(msg) {
                "local_backup_saved" -> context.getString(R.string.local_backup_saved)
                "backup_complete_success" -> "Cloud backup successful!"
                "restore_success" -> "Data restored successfully! Previous state saved in Safety Backup."
                else -> msg
            }
            snackbarHostState.showSnackbar(finalMsg)
        }
    }

    if (showRestoreConfirmDialog != null) {
        val action = showRestoreConfirmDialog!!
        val displayName = when(action) {
            is RestoreAction.LocalUri -> "External File"
            is RestoreAction.LocalId -> {
                val b = backupHistory.find { it.id == action.id }
                b?.fileName ?: "Backup"
            }
            is RestoreAction.CloudPath -> "Cloud Backup"
            is RestoreAction.LatestCloud -> "Latest Cloud Backup"
        }

        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            title = { Text("Restore Data?") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Restoring $displayName will replace your current data.")
                    Text("A safety backup will be created automatically.", fontWeight = FontWeight.Bold)
                    Text("Current transactions not in this backup will be lost.", color = Color.Red)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when(action) {
                            is RestoreAction.LocalUri -> viewModel.restoreFromUri(action.uri, context)
                            is RestoreAction.LocalId -> viewModel.restoreLocalBackup(action.id)
                            is RestoreAction.CloudPath -> viewModel.restoreFromStoragePath(action.path)
                            is RestoreAction.LatestCloud -> viewModel.restoreLatestCloud()
                        }
                        showRestoreConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog != null) {
        val backup = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Backup?") },
            text = { Text("Are you sure you want to delete this backup slot?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentDelete(backup.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
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
                SectionTitle("Data Backup")
            }

    val currentSafety = latestSafety
    val currentLocal = latestLocal
    val currentCloud = latestCloud

    // UNDO BANNER
    if (currentSafety != null && currentSafety.timestamp > (currentLocal?.timestamp ?: 0)) {
         item {
             Card(
                 modifier = Modifier.fillMaxWidth(),
                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
             ) {
                 Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                     Spacer(Modifier.width(12.dp))
                     Column(Modifier.weight(1f)) {
                         Text("Restore Completed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                         Text("Undo this restore by using the Safety Backup below.", fontSize = 12.sp)
                     }
                     TextButton(onClick = { viewModel.onRestoreSelected(RestoreAction.LocalId("SLOT_SAFETY"), context) }) {
                         Text("UNDO")
                     }
                 }
             }
         }
    }

    // LOCAL BACKUP SLOT
    item {
        BackupSlotCard(
            title = "Local Backup",
            backup = currentLocal,
            icon = Icons.Default.SaveAlt,
            color = Color(0xFF16A34A),
            onAction = { viewModel.createLocalBackup("MANUAL") },
            actionLabel = if (currentLocal == null) "Create Backup" else "Update Backup",
            onRestore = { viewModel.onRestoreSelected(RestoreAction.LocalId("SLOT_LOCAL"), context) },
            onDelete = { showDeleteConfirmDialog = currentLocal },
            extraAction = {
                TextButton(onClick = {
                    if (isPremium) {
                        currentLocal?.let { viewModel.uploadToCloud(File(it.filePath), it.reportType, it.id) }
                    } else {
                        premiumLockedMessage = "Cloud upload is a premium feature."
                        showPremiumLockedDialog = true
                    }
                }, enabled = currentLocal != null) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Upload to Cloud")
                }
            }
        )
    }

    // CLOUD BACKUP SLOT
    item {
        val isOutdated = currentCloud != null && currentLocal != null && currentLocal.timestamp > currentCloud.timestamp
        BackupSlotCard(
            title = "Cloud Backup",
            backup = currentCloud,
            icon = Icons.Default.CloudUpload,
            color = Color(0xFF1D4ED8),
            onAction = { 
                if (isPremium) viewModel.performManualBackup()
                else {
                    premiumLockedMessage = "Cloud Backup is a premium feature."
                    showPremiumLockedDialog = true
                }
            },
            actionLabel = if (currentCloud == null) "Setup Cloud Backup" else "Update Cloud Backup",
            onRestore = { viewModel.onRestoreSelected(RestoreAction.LatestCloud, context) },
            onDelete = { showDeleteConfirmDialog = currentCloud },
            extraAction = if (isOutdated) {
                {
                    Text(
                        "Cloud backup may be outdated. New changes have not been backed up.",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else null
        )
    }

    // SAFETY BACKUP SLOT
    item {
        BackupSlotCard(
            title = "Safety Backup",
            backup = currentSafety,
            icon = Icons.Default.Security,
            color = Color(0xFFEAB308),
            onAction = { viewModel.createLocalBackup("PRE_RESTORE_SAFETY") },
            actionLabel = "Refresh Safety Snapshot",
            onRestore = { viewModel.onRestoreSelected(RestoreAction.LocalId("SLOT_SAFETY"), context) },
            onDelete = { showDeleteConfirmDialog = currentSafety }
        )
    }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle("Advanced Actions")
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Restore File", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = onOpenArchive,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Inventory, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Archive", fontSize = 12.sp)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Data Maintenance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.recalculateStock() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Icon(Icons.Default.Calculate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recalculate Stock")
                        }
                    }
                }
            }
        }
    }

    if (showPremiumLockedDialog) {
        PremiumFeatureLockedDialog(
            message = premiumLockedMessage,
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
fun BackupSlotCard(
    title: String,
    backup: BackupEntity?,
    icon: ImageVector,
    color: Color,
    onAction: () -> Unit,
    actionLabel: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    extraAction: @Composable (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (backup != null) {
                        Text("Last Updated: ${dateFormat.format(Date(backup.timestamp))}", fontSize = 12.sp, color = Color.Gray)
                        Text("Size: ${Formatter.formatFileSize(backup.size)} | Status: ${backup.status}", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Text("No backup found in this slot", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                if (backup != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }
                
                if (backup != null && backup.status == "SUCCESS") {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Restore Data", fontSize = 12.sp)
                    }
                }
            }
            
            if (extraAction != null) {
                Spacer(Modifier.height(8.dp))
                extraAction()
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
