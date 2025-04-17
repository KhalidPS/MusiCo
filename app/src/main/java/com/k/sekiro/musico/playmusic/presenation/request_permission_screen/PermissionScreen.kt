package com.k.sekiro.musico.playmusic.presenation.request_permission_screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MultiplePermissionRequest(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDelayDone by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        mutableListOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE

            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            } else {
                null
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else null
        ).filterNotNull().toTypedArray()
    }
    var allPermissionsGranted by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember {
        mutableStateOf(false)
    }
    var currentRationalePermission by remember { mutableStateOf("") }
    var pendingPermissions by remember {
        mutableStateOf(listOf<String>())
    }
    Log.e("ks", "is all granted top  :$allPermissionsGranted")
    val multiplePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            val grantedList = permissionsResult.filterValues {
                it
            }.keys.toList()
            val deniedList = permissionsToRequest.filter {
                it !in
                        grantedList
            }
            if (deniedList.isEmpty()) {
                allPermissionsGranted = true
                onAllPermissionsGranted()
            } else {
                pendingPermissions = deniedList
                val firstDeniedPermission = deniedList.firstOrNull()
                if (firstDeniedPermission != null &&
                    !context.shouldShowRationale(firstDeniedPermission)
                ) {
// User has denied and asked not to be asked again, navigate to settings
                    //navigateToAppSettings(context)
                } else if (firstDeniedPermission != null) {
                    currentRationalePermission = firstDeniedPermission
                    showPermissionRationaleDialog = true
                }
            }
        }
    LaunchedEffect(lifecycleOwner.lifecycle) {

        launch {
            delay(200) /** this delay is for prevent the requestPermission screen appears as flicker (less than 1 sec)
            when open app for first time and all permissions already granted otherwise (permissions not granted)
            the screen will appear after 200 MS this is the only solution I found**/
            isDelayDone = true
        }

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                // Initial check for permissions
                val notGranted = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(context, it) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                if (notGranted.isNotEmpty()) {
                    pendingPermissions = notGranted
// Launch permission request only if there are permissions to ask for
                    multiplePermissionsLauncher.launch(notGranted.toTypedArray())
                } else {
                    allPermissionsGranted = true
                    onAllPermissionsGranted()
                }

                Log.e("ks", "is all granted1 :$allPermissionsGranted")

            }
        }
    }

// Prevent navigation if not all permissions are granted
    if (!allPermissionsGranted && isDelayDone) {
// You can display a loading indicator or a message here
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {


            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Please go to settings and grant the permissions")
                Button(onClick = { navigateToAppSettings(context) }) {
                    Text("Go")
                }
            }
        }
        Log.e("ks", "is all granted2 :$allPermissionsGranted")

    }
    if (showPermissionRationaleDialog &&
        currentRationalePermission.isNotEmpty()
    ) {
        PermissionRationaleDialog(
            permission = currentRationalePermission,
            onContinueClick = {
                showPermissionRationaleDialog = false
                multiplePermissionsLauncher.launch(
                    arrayOf(currentRationalePermission)
                )
            },
            onCancelClick = {
                showPermissionRationaleDialog = false
// Optionally handle if the user cancels the rationale
            }
        )
    }
}

@Composable
fun PermissionRationaleDialog(
    permission: String,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val rationaleText = when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "This app needs storage access to retrieve audio files."
        Manifest.permission.READ_MEDIA_AUDIO -> "This app needs audio files access to show all audios"
        Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK -> "This app needs to be run in the background for playing audios while app in background"
        Manifest.permission.POST_NOTIFICATIONS -> "This app needs to send notifications to keep playing sound while the app in background."
        else -> "This app requires this permission."
    }
    AlertDialog(
        onDismissRequest = onCancelClick,
        title = { Text("Permission Required") },
        text = { Text(rationaleText) },
        confirmButton = {
            Button(onClick = onContinueClick) {
                Text("Continue")
            }
        },
        dismissButton = {
            Button(onClick = onCancelClick) {
                Text("Cancel")
            }
        }
    )
}

fun Context.shouldShowRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        this as ComponentActivity,
        permission
    )
}

fun navigateToAppSettings(context: Context) {
    try {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", context.packageName, null)
                data = uri
            }
        context.startActivity(intent)

    } catch (ex: SecurityException) {
        Log.e("ks", "Ops... ${ex.message}")
    } catch (ex: Exception) {
        Log.e("ks", "Ops... ${ex.message}")

    }
}

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    var allPermissionsGranted by remember { mutableStateOf(false) }
    MultiplePermissionRequest(
        onAllPermissionsGranted = {
            allPermissionsGranted = true
            Log.e("ks", "onAllPermissionsGranted() has been called")
        }
    )
    if (allPermissionsGranted) {
        content()
    } else {
// While permissions are not granted, the user stays here.
// The MultiplePermissionRequest composable handles the UI for this state.
    }
}
