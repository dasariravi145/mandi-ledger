package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.data.local.entity.AccountBookArchiveEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveListScreen(
    viewModel: ArchiveListViewModel,
    onArchiveClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val archives by viewModel.archives.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Book Archive") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search by name, phone or ID") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            TabRow(selectedTabIndex = when(filterType) {
                "ALL" -> 0
                "FARMER" -> 1
                else -> 2
            }) {
                Tab(selected = filterType == "ALL", onClick = { viewModel.setFilter("ALL") }, text = { Text("All") })
                Tab(selected = filterType == "FARMER", onClick = { viewModel.setFilter("FARMER") }, text = { Text("Farmers") })
                Tab(selected = filterType == "BUYER", onClick = { viewModel.setFilter("BUYER") }, text = { Text("Buyers") })
            }

            if (archives.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No archived accounts yet.", color = Color.Gray)
                        Text("Settled histories will appear here.", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archives, key = { it.archiveId }) { archive ->
                        ArchiveItem(
                            archive = archive,
                            onClick = { onArchiveClick(archive.archiveId) },
                            onRestore = { viewModel.restoreArchive(archive.archiveId) },
                            onPermanentDelete = { viewModel.permanentDelete(archive.archiveId) },
                            onExportPdf = { viewModel.exportPdf(context, archive.archiveId) },
                            onExportExcel = { viewModel.exportExcel(context, archive.archiveId) }
                        )
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ArchiveItem(
    archive: AccountBookArchiveEntity,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = archive.partyName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if(archive.partyType == "FARMER") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = archive.partyType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if(archive.partyType == "FARMER") Color(0xFF2E7D32) else Color(0xFF1565C0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Archived: ${dateFormat.format(Date(archive.archivedAt))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Row {
                    Text(text = "Total: ₹${Formatter.formatCurrency(archive.totalAmount)}", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(Modifier.width(12.dp))
                    Text(text = "Settled: ${dateFormat.format(Date(archive.settlementDate))}", fontSize = 13.sp, color = Color.DarkGray)
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("View Details") },
                        onClick = { showMenu = false; onClick() },
                        leadingIcon = { Icon(Icons.Default.Visibility, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Restore History") },
                        onClick = { showMenu = false; onRestore() },
                        leadingIcon = { Icon(Icons.Default.SettingsBackupRestore, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export PDF") },
                        onClick = { showMenu = false; onExportPdf() },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Excel") },
                        onClick = { showMenu = false; onExportExcel() },
                        leadingIcon = { Icon(Icons.Default.TableChart, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Permanent Delete") },
                        onClick = { showMenu = false; onPermanentDelete() },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
