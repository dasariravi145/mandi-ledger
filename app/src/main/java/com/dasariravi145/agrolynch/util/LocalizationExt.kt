package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.annotation.StringRes
import timber.log.Timber

fun Context.getLocalizedText(@StringRes resId: Int, vararg formatArgs: Any): String {
    val keyName = resources.getResourceEntryName(resId)
    return try {
        val text = getString(resId, *formatArgs)
        // Simple heuristic: if text is same as key or looks like it's missing
        Timber.v("Localization: Loaded key='$keyName' value='$text'")
        text
    } catch (e: Exception) {
        Timber.e(e, "Localization: Missing Translation Key='$keyName'")
        ""
    }
}
