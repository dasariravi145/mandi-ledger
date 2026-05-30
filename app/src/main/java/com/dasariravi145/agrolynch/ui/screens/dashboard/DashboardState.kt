package com.dasariravi145.agrolynch.ui.screens.dashboard

import com.dasariravi145.agrolynch.domain.model.DashboardSummary

data class DashboardState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary = DashboardSummary(),
    val error: String? = null
)
