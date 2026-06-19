package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSettingsScreen(
    viewModel: BillSettingsViewModel,
    onBack: () -> Unit
) {
    val seriesList by viewModel.seriesList.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Number Series") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(seriesList) { series ->
                SeriesCard(series = series, onSave = viewModel::updateSeries)
            }
        }
    }
}

@Composable
fun SeriesCard(
    series: BillNumberSeriesEntity,
    onSave: (BillNumberSeriesEntity) -> Unit
) {
    var prefix by remember(series) { mutableStateOf(series.prefix) }
    var currentNumber by remember(series) { mutableStateOf(series.currentNumber.toString()) }
    var resetYearly by remember(series) { mutableStateOf(series.resetYearly) }
    var financialYearEnabled by remember(series) { mutableStateOf(series.financialYearEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = series.seriesType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it },
                label = { Text("Prefix") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentNumber,
                onValueChange = { currentNumber = it },
                label = { Text("Next Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = resetYearly, onCheckedChange = { resetYearly = it })
                Text("Reset Yearly")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = financialYearEnabled, onCheckedChange = { financialYearEnabled = it })
                Text("Financial Year Prefix (FY24-25)")
            }

            Button(
                onClick = {
                    onSave(series.copy(
                        prefix = prefix,
                        currentNumber = currentNumber.toLongOrNull() ?: series.currentNumber,
                        resetYearly = resetYearly,
                        financialYearEnabled = financialYearEnabled,
                        updatedAt = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }
        }
    }
}
