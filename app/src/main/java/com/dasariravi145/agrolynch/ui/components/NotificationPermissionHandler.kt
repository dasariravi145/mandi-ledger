package com.dasariravi145.agrolynch.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import com.dasariravi145.agrolynch.util.WorkScheduler

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    
    // Safety check: Don't initialize launcher if ActivityResultRegistryOwner is not available
    // though the provider in MainActivity should ensure it is.
    val owner = LocalActivityResultRegistryOwner.current
    if (owner == null) {
        timber.log.Timber.e("NotificationPermissionHandler: LocalActivityResultRegistryOwner is null!")
        return
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            WorkScheduler.scheduleDailySummary(context)
        }
    }
    
    // Rest of the code...
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                try {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to launch notification permission request")
                }
            } else {
                WorkScheduler.scheduleDailySummary(context)
            }
        } else {
            WorkScheduler.scheduleDailySummary(context)
        }
    }
}
