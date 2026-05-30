package com.dasariravi145.agrolynch.ui.screens.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showPremiumLockedDialog by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    LaunchedEffect(Unit) {
        viewModel.message.collect {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup & Reports / బ్యాకప్") },
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
                    text = "Generate Backup Reports (PDF)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackupBox(
                        title = "Manual",
                        icon = Icons.Default.PictureAsPdf,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.createLocalBackup("MANUAL") }
                    )
                    BackupBox(
                        title = "Weekly",
                        icon = Icons.Default.DateRange,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.createLocalBackup("WEEKLY") }
                    )
                    BackupBox(
                        title = "Monthly",
                        icon = Icons.Default.CalendarMonth,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.createLocalBackup("MONTHLY") }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Cloud Backup (Premium)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                BackupActionCard(
                    title = "Upload Last Backup to Cloud",
                    icon = Icons.Default.CloudUpload,
                    description = "Secure your data on Firebase servers.",
                    isPremium = isPremium,
                    isLoading = isLoading,
                    onClick = {
                        if (isPremium) {
                            val lastLocal = backupHistory.find { it.type == "LOCAL" }
                            if (lastLocal != null) {
                                viewModel.uploadToCloud(File(lastLocal.filePath))
                            } else {
                                viewModel.createLocalBackup("MANUAL")
                            }
                        } else {
                            showPremiumLockedDialog = true
                        }
                    }
                )
            }

            item {
                Text(
                    text = "Backup History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                    onDelete = { viewModel.deleteBackup(backup.id) }
                )
            }
        }
    }

    if (showPremiumLockedDialog) {
        PremiumFeatureLockedDialog(
            onDismiss = { showPremiumLockedDialog = false },
            onUpgradeClick = {
                showPremiumLockedDialog = false
                onUpgradeClick()
            }
        )
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
fun BackupHistoryItem(backup: BackupEntity, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (backup.type == "CLOUD") Icons.Default.CloudDone else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (backup.type == "CLOUD") Color(0xFF2563EB) else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = backup.fileName, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Text(
                    text = "${backup.reportType} | ${dateFormat.format(Date(backup.timestamp))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BackupActionCard(
    title: String,
    icon: ImageVector,
    description: String,
    isLoading: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = if (isPremium) Color(0xFF16A34A) else Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (!isPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, contentDescription = "Premium", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
