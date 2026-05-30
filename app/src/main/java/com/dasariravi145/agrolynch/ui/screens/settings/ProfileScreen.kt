package com.dasariravi145.agrolynch.ui.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.ui.components.AgroButton
import com.dasariravi145.agrolynch.ui.components.AgroTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null && !hasInitialized) {
            name = user!!.name
            location = user!!.location
            hasInitialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Header
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (isPremium) {
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Premium",
                            modifier = Modifier.padding(6.dp),
                            tint = Color.Black
                        )
                    }
                }
            }

            Text(
                text = if (isPremium) "Premium Member" else "Standard User",
                style = MaterialTheme.typography.labelLarge,
                color = if (isPremium) Color(0xFF16A34A) else Color.Gray
            )

            HorizontalDivider()

            // Form Fields
            AgroTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name"
            )

            AgroTextField(
                value = user?.phoneNumber ?: "",
                onValueChange = { },
                label = "Phone Number",
                readOnly = true
            )

            AgroTextField(
                value = location,
                onValueChange = { location = it },
                label = "Location / Village"
            )

            Spacer(modifier = Modifier.height(24.dp))

            AgroButton(
                text = "Update Profile",
                onClick = { viewModel.updateProfile(name, location) },
                isLoading = isLoading,
                enabled = name.isNotBlank() && (name != user?.name || location != user?.location)
            )
        }
    }
}

@Composable
fun AgroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = readOnly,
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}
