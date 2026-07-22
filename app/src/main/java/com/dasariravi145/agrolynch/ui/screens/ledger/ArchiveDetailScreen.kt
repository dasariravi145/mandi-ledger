package com.dasariravi145.agrolynch.ui.screens.ledger

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.BackupData
import com.dasariravi145.agrolynch.util.Formatter
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailScreen(
    archiveId: String,
    viewModel: ArchiveListViewModel,
    gson: Gson,
    onBack: () -> Unit
) {
    val archive by viewModel.selectedArchive.collectAsState()
    val snapshot by viewModel.selectedSnapshot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(archiveId) {
        viewModel.loadArchiveDetails(archiveId, gson)
    }

    DisposableEffect(archiveId) {
        onDispose {
            viewModel.clearSelectedArchive()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(archive?.partyName ?: "Archive Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(archive?.partyType ?: "", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { archive?.let { viewModel.exportPdf(context, it.archiveId) } },
                        enabled = archive != null && snapshot != null && !isLoading
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                    }
                }
            )
        }
    ) { padding ->
        if (archive == null || snapshot == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("Archive details not found.", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ArchiveSummaryHeader(archive!!)
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.restoreArchive(archive!!.archiveId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) {
                                Icon(Icons.Default.SettingsBackupRestore, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore")
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportExcel(context, archive!!.archiveId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TableChart, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Excel")
                            }
                        }
                    }
                    
                    item {
                        Button(
                            onClick = { viewModel.permanentDelete(archive!!.archiveId); onBack() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.DeleteForever, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Permanent Delete")
                        }
                    }

                    // Arrivals Section
                    if (snapshot!!.arrivals.isNotEmpty()) {
                        item { SectionHeader("Arrivals") }
                        items(snapshot!!.arrivals) { arrival ->
                            ArchiveArrivalItem(arrival)
                        }
                    }

                    // Sales Section
                    if (snapshot!!.sales.isNotEmpty()) {
                        item { SectionHeader("Sales") }
                        items(snapshot!!.sales) { sale ->
                            ArchiveSaleItem(sale)
                        }
                    }

                    // Payments Section
                    if (snapshot!!.payments.isNotEmpty()) {
                        item { SectionHeader("Payments") }
                        items(snapshot!!.payments) { payment ->
                            ArchivePaymentItem(payment)
                        }
                    }
                    
                    item { Spacer(Modifier.height(32.dp)) }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveSummaryHeader(archive: AccountBookArchiveEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Total Amount", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("₹${Formatter.formatCurrency(archive.totalAmount)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Settled Date", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(dateFormat.format(Date(archive.settlementDate)), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Archive ID: ${archive.archiveId.take(8).uppercase()}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                Text("Status: ${archive.status}", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ArchiveArrivalItem(arrival: ArrivalEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(arrival.productName, fontWeight = FontWeight.Bold)
                Text(Formatter.formatDate(arrival.date), fontSize = 11.sp, color = Color.Gray)
            }
            Text("${arrival.grade} | ${arrival.quantity} ${arrival.unit} @ ₹${arrival.purchaseRate}", fontSize = 13.sp)
            Text("Net Amount: ₹${Formatter.formatCurrency(arrival.netAmount)}", fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
        }
    }
}

@Composable
fun ArchiveSaleItem(sale: SaleEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Sale: ${sale.productName}", fontWeight = FontWeight.Bold)
                Text(Formatter.formatDate(sale.date), fontSize = 11.sp, color = Color.Gray)
            }
            Text("Total Amount: ₹${Formatter.formatCurrency(sale.totalNetAmount)}", fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
        }
    }
}

@Composable
fun ArchivePaymentItem(payment: PaymentEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Payment: ${payment.paymentMode}", fontWeight = FontWeight.Bold)
                Text(Formatter.formatDate(payment.date), fontSize = 11.sp, color = Color.Gray)
            }
            Text("Amount: ₹${Formatter.formatCurrency(payment.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        }
    }
}
