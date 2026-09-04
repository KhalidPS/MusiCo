package com.k.sekiro.musico.playmusic.presenation.exchange

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.k.sekiro.musico.playmusic.presenation.exchange.component.ImportPreviewDialog
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.navigateToAppSettings
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.shouldShowRationale
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    importPreview: PlaylistImportPreview?,
    onDecoded: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    onBack: () -> Unit,
    transferState: TransferState? = null,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    // Bumped whenever the user dismisses a preview, forcing a fresh analyzer + camera rebind.
    var scanGeneration by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) return@rememberLauncherForActivityResult
        onDecoded(text)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (hasPermission) {
            CameraPreview(
                scanGeneration = scanGeneration,
                onQrDecoded = onDecoded,
            )
            Text(
                text = "Point at a MusiCo playlist QR code",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .fillMaxWidth()
            )
        } else {
            PermissionRequestContent(
                permanentlyDenied = permissionRequested &&
                    !context.shouldShowRationale(Manifest.permission.CAMERA),
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { navigateToAppSettings(context) },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBackIos,
                    contentDescription = "back",
                    tint = if (hasPermission) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        TextButton(
            onClick = { fileImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            Text("Import from file", color = if (hasPermission) Color.White else MaterialTheme.colorScheme.primary)
        }

        if (importPreview != null) {
            ImportPreviewDialog(
                preview = importPreview,
                onConfirm = {
                    onConfirmImport()
                    onBack()
                },
                onDismiss = {
                    onDismissImport()
                    scanGeneration++
                },
                transferState = transferState,
            )
        }
    }
}

@Composable
private fun CameraPreview(
    scanGeneration: Int,
    onQrDecoded: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(scanGeneration) {
        var provider: ProcessCameraProvider? = null
        var cancelled = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (cancelled) return@addListener
            val cameraProvider = future.get()
            provider = cameraProvider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, QrCodeAnalyzer(onQrDecoded)) }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cancelled = true
            provider?.unbindAll()
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }
}


@Composable
private fun PermissionRequestContent(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Camera access needed",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "MusiCo uses the camera only to scan a playlist QR code. Nothing is recorded " +
                "or sent anywhere.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (permanentlyDenied) {
            Button(onClick = onOpenSettings) { Text("Open settings") }
        } else {
            Button(onClick = onGrant) { Text("Allow camera") }
        }
    }
}
