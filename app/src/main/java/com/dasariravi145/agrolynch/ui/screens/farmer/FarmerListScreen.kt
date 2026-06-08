package com.dasariravi145.agrolynch.ui.screens.farmer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerListScreen(
    viewModel: FarmerViewModel,
    onAddClick: () -> Unit,
    onFarmerClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Timber.d("FarmerListScreen: Initializing...")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val farmers by viewModel.filteredFarmers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.farmers)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_farmer))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { query ->
                    Timber.v("FarmerListScreen: Search query: $query")
                    viewModel.onSearchQueryChange(query)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_farmers_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (state.isLoading && farmers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (farmers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_farmers_found))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = farmers,
                        key = { it.id }
                    ) { farmer ->
                        FarmerItem(farmer = farmer, onClick = { 
                            Timber.d("FarmerListScreen: Farmer clicked: ${farmer.id}")
                            onFarmerClick(farmer.id) 
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FarmerItem(farmer: FarmerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = farmer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = farmer.village, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Text(
                text = farmer.mobileNumber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
