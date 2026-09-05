package com.k.sekiro.musico.playmusic.presenation.exchange.preparations

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.PermissionRationaleDialog
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.navigateToAppSettings

/**
 * Everything that has to be true before a transfer can work, as a checklist the user can act on -
 * shown only when something is actually missing (callers check [unsatisfiedBlocking] first).
 *
 * This exists because the platform's own failures here are unreadable: with location services off,
 * an API ≤32 device fails to create or find a Wi-Fi Direct group and the only signal was a generic
 * "couldn't connect". [onNext] resumes whatever the user was doing, so nothing has to be re-done.
 */
@Composable
fun TransferPreparationsScreen(
    role: TransferRole,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requirements = remember(role) { transferRequirements(role) }

    val satisfied = remember { mutableStateMapOf<String, Boolean>() }
    fun refresh() = requirements.forEach { satisfied[it.id] = it.isSatisfied(context) }
    remember { refresh(); true }

    // The toggle rows are fixed in the Settings app, so the answer only changes while this screen
    // is away - re-check on every return rather than polling. Same shape as the app-wide gate.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // "Never ask again" is indistinguishable from "never asked" until a request actually comes
    // back denied, so a row only offers Settings after we've seen that happen.
    val needsSettings = remember { mutableStateMapOf<String, Boolean>() }
    var pendingRequestId by remember { mutableStateOf<String?>(null) }
    var rationaleFor by remember { mutableStateOf<TransferRequirement?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val requestedId = pendingRequestId
        pendingRequestId = null
        refresh()
        if (granted || requestedId == null) return@rememberLauncherForActivityResult
        val requirement = requirements.firstOrNull { it.id == requestedId }
            ?: return@rememberLauncherForActivityResult
        val permission = (requirement.action as? RequirementAction.RequestPermission)?.permission
            ?: return@rememberLauncherForActivityResult
        val activity = context.findActivity()
        val canAskAgain = activity != null &&
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (canAskAgain) rationaleFor = requirement else needsSettings[requirement.id] = true
    }

    fun act(requirement: TransferRequirement) {
        when (val action = requirement.action) {
            RequirementAction.OpenWifiSettings -> openWifiSettings(context)
            RequirementAction.OpenLocationSettings -> openLocationSettings(context)
            is RequirementAction.RequestPermission -> {
                if (needsSettings[requirement.id] == true) {
                    navigateToAppSettings(context)
                } else {
                    pendingRequestId = requirement.id
                    permissionLauncher.launch(action.permission)
                }
            }
        }
    }

    BackHandler { onBack() }

    val allBlockingDone = requirements.filter { it.blocking }.all { satisfied[it.id] == true }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBackIos, contentDescription = "back")
            }
            Text("Preparations", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(
                Icons.Filled.WifiTethering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (role == TransferRole.Sender) {
                    "A few things are needed before you can send songs."
                } else {
                    "A few things are needed before you can receive songs."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))

            requirements.forEach { requirement ->
                RequirementRow(
                    requirement = requirement,
                    satisfied = satisfied[requirement.id] == true,
                    needsSettings = needsSettings[requirement.id] == true,
                    onAction = { act(requirement) },
                )
                Spacer(Modifier.height(8.dp))
            }

            // The one thing the checklist can't turn into a checkbox: on API 29-30 the join itself
            // is gated behind a system dialog the user has to catch and accept.
            if (role == TransferRole.Receiver &&
                Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.R
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "When connecting, Android will ask you to confirm joining the other " +
                        "device's network - accept that prompt when it appears.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = onNext,
            enabled = allBlockingDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text("NEXT")
        }
    }

    rationaleFor?.let { requirement ->
        val permission = (requirement.action as? RequirementAction.RequestPermission)?.permission
        if (permission == null) {
            rationaleFor = null
        } else {
            PermissionRationaleDialog(
                permission = permission,
                onContinueClick = {
                    rationaleFor = null
                    pendingRequestId = requirement.id
                    permissionLauncher.launch(permission)
                },
                onCancelClick = { rationaleFor = null },
            )
        }
    }
}

@Composable
private fun RequirementRow(
    requirement: TransferRequirement,
    satisfied: Boolean,
    needsSettings: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    requirement.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (!requirement.blocking) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "optional",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                requirement.explanation,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.size(12.dp))

        AnimatedContent(
            targetState = satisfied,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "requirement-${requirement.id}",
        ) { isSatisfied ->
            if (isSatisfied) {
                // Mirrors the transfer screens' "done" pop so the whole feature animates alike.
                var visible by remember { mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(visible = visible, enter = scaleIn(spring()) + fadeIn()) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "ready",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                TextButton(onClick = onAction) {
                    Text(if (needsSettings) "SETTINGS" else "OPEN")
                }
            }
        }
    }
}
