package com.dasariravi145.agrolynch.ui.screens.report

import androidx.compose.runtime.Composable

// This file is deprecated. Using OutstandingAgingScreen in ReportDetailScreens.kt
@Composable
fun OutstandingReportScreen(
    viewModel: ReportViewModel,
    onBackClick: () -> Unit
) {
    OutstandingAgingScreen(viewModel, onBackClick)
}
