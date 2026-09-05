package com.k.sekiro.musico.playmusic.presenation.exchange.preparations

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Deep links to the system screens the transfer checklist can't change itself: an app can't turn
 * the Wi-Fi radio (API 29+) or location services on programmatically, so every toggle row hands
 * the user off to Settings and the checklist re-checks when they come back.
 *
 * Both are best-effort - an OEM can ship without the panel activity or block the intent, and a
 * missing settings screen must not take the app down mid-transfer-setup.
 */
fun openWifiSettings(context: Context) {
    // The API 29+ panel is a slide-up sheet over this app rather than a full trip into Settings,
    // so the user stays in context - but it doesn't exist below 29, and not every OEM has it.
    val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_WIFI)
    } else {
        null
    }
    if (panel != null && startSettings(context, panel)) return
    startSettings(context, Intent(Settings.ACTION_WIFI_SETTINGS))
}

fun openLocationSettings(context: Context) {
    startSettings(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

private fun startSettings(context: Context, intent: Intent): Boolean = try {
    if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (_: Exception) {
    false
}

/**
 * Walks the `ContextWrapper` chain to the hosting [Activity], or null if there isn't one.
 *
 * `Context.shouldShowRationale` casts straight to `ComponentActivity` and throws on anything else;
 * this lets the checklist ask the question without risking that crash.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
