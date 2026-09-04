package com.k.sekiro.musico.playmusic.presenation.request_permission_screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * The Wi-Fi/location/legacy-storage permissions an audio transfer needs, for the current API
 * level: `NEARBY_WIFI_DEVICES` (API 33+, declared `neverForLocation` in the manifest),
 * `ACCESS_FINE_LOCATION` (API ≤32, required for Wi-Fi P2P discovery below API 33), and
 * `WRITE_EXTERNAL_STORAGE` (API ≤28, for `MediaImporter`'s legacy write path).
 */
fun transferPermissions(): Array<String> = listOfNotNull(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    },
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) Manifest.permission.WRITE_EXTERNAL_STORAGE else null,
).toTypedArray()

/**
 * On-demand permission gate for the audio-transfer flow. Unlike [MultiplePermissionRequest] /
 * [PermissionGate] (which wrap the entire app, gated on a fixed permission list checked on every
 * resume), this is parameterized and only mounted while a transfer is actually being started -
 * from "Send with songs" or right after a `MUSICO-XFER-1:` QR is recognized, not before scanning.
 *
 * Reuses [Context.shouldShowRationale] / [navigateToAppSettings] / [PermissionRationaleDialog]
 * from the app-wide gate rather than duplicating that mechanics.
 */
@Composable
fun TransferPermissionGate(
    onGranted: () -> Unit,
    onCancelled: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember { transferPermissions() }

    var granted by remember {
        mutableStateOf(
            permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        )
    }
    var showRationaleFor by remember { mutableStateOf<String?>(null) }
    var showGoToSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val denied = permissions.filter { result[it] != true }
        if (denied.isEmpty()) {
            granted = true
        } else {
            val first = denied.first()
            if (!context.shouldShowRationale(first)) {
                showGoToSettings = true
            } else {
                showRationaleFor = first
            }
        }
    }

    // transferPermissions() always returns at least one entry, so "already granted" and "need to
    // request" are the only two starting states - no empty-list case to handle.
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(permissions)
    }

    LaunchedEffect(granted) {
        if (granted) onGranted()
    }

    if (granted) {
        content()
        return
    }

    if (showGoToSettings) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text("Please go to settings and grant the permissions needed to send/receive songs")
                Button(onClick = { navigateToAppSettings(context) }) { Text("Go") }
                Button(onClick = onCancelled) { Text("Cancel") }
            }
        }
    }

    showRationaleFor?.let { permission ->
        PermissionRationaleDialog(
            permission = permission,
            onContinueClick = {
                showRationaleFor = null
                launcher.launch(arrayOf(permission))
            },
            onCancelClick = {
                showRationaleFor = null
                onCancelled()
            },
        )
    }
}
