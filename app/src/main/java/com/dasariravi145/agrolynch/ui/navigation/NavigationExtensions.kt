package com.dasariravi145.agrolynch.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import timber.log.Timber

fun NavController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    val currentRoute = currentDestination?.route
    if (currentRoute == route) {
        Timber.w("Navigation: Attempted to navigate to the same route: $route. Ignoring.")
        return
    }
    
    try {
        Timber.d("Navigation: Navigating from $currentRoute to $route")
        navigate(route, builder)
    } catch (e: Exception) {
        Timber.e(e, "Navigation: Error navigating to $route")
    }
}
