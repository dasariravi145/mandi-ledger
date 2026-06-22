package com.dasariravi145.agrolynch.ui.screens.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.findActivity
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    data: ReceiptData,
    viewModel: ReceiptViewModel,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.companyProfile.collectAsState()
    val previewFile by viewModel.generatedPdfFile.collectAsState()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Trigger PDF generation for actual renderer logic
    LaunchedEffect(profile) {
        // Since we refactored the service, the PDF generation logic in ViewModel 
        // will now use the new rendering engine.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.generatedPdfFile.value?.let { file ->
                                val uri = com.dasariravi145.agrolynch.util.PdfGenerator.getUriFromFile(context, file)
                                com.dasariravi145.agrolynch.util.PdfActionManager.sharePdf(context, uri)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.generatedPdfFile.value?.let { file ->
                                val uri = com.dasariravi145.agrolynch.util.PdfGenerator.getUriFromFile(context, file)
                                val activity = context.findActivity()
                                if (activity != null) {
                                    android.util.Log.d("PRINT_DEBUG", "context=${context::class.java.name}, activity=${activity::class.java.name}")
                                    activity.runOnUiThread {
                                        com.dasariravi145.agrolynch.util.PdfPrintHelper.print(activity, uri)
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Print requires active screen", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Print")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Actual PDF Preview or visual card
            if (previewFile != null) {
                com.dasariravi145.agrolynch.ui.screens.template.PdfPreviewCard(previewFile!!)
                Spacer(Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    profile?.let { pr ->
                        Text(
                            text = pr.companyName.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        if (pr.tagline.isNotEmpty()) {
                            Text(
                                text = pr.tagline,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "${pr.address}, ${pr.village}\nMob: ${pr.mobile1} | GST: ${pr.gstNumber}",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            lineHeight = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("BILL TO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(data.partyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(data.partyType, fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DATE: ${dateFormat.format(Date(data.date))}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("NO: #${data.receiptId}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    TableHead()
                    data.items.forEach { item ->
                        TableRow(item.description, item.quantity, item.rate, item.amount)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(modifier = Modifier.align(Alignment.End).width(200.dp)) {
                        TotalItem("SUB TOTAL", data.totalAmount)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = Color(0xFF1B5E20), shape = RoundedCornerShape(4.dp)) {
                            Row(Modifier.padding(8.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("GRAND TOTAL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("₹${Formatter.formatCurrency(data.totalAmount)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                        Column {
                            profile?.upiQrPath?.let {
                                Text("SCAN TO PAY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Box(Modifier.size(70.dp).border(1.dp, Color.LightGray))
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HorizontalDivider(Modifier.width(120.dp), thickness = 0.5.dp)
                            Text("Authorized Signature", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TableHead() {
    Surface(color = Color(0xFFF1F8E9)) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            Text("Description", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Qty", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Rate", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Amount", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun TableRow(desc: String, qty: String, rate: String, amount: String) {
    Row(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(desc, modifier = Modifier.weight(2f), fontSize = 11.sp)
        Text(qty, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center)
        Text(rate, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center)
        Text("₹$amount", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
private fun TotalItem(label: String, value: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text("₹${Formatter.formatCurrency(value)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
