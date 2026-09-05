package com.k.sekiro.musico.playmusic.presenation.exchange.preparations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.k.sekiro.musico.playmusic.data.exchange.WifiDirectTransport

/** Which half of a transfer a checklist is being built for - the two need different things. */
enum class TransferRole { Sender, Receiver }

sealed interface RequirementAction {
    data class RequestPermission(val permission: String) : RequirementAction
    data object OpenWifiSettings : RequirementAction
    data object OpenLocationSettings : RequirementAction
}

/**
 * One row of the pre-transfer checklist: something that must be true before a Wi-Fi Direct
 * transfer can work, plus how the user fixes it.
 *
 * [blocking] false means the transfer still works without it (today only the notification
 * permission) - those rows are shown for visibility but must not gate the Next button, or a user
 * who permanently denied them would be locked out of transferring forever.
 */
data class TransferRequirement(
    val id: String,
    val title: String,
    val explanation: String,
    val action: RequirementAction,
    val blocking: Boolean = true,
)

object RequirementIds {
    const val WIFI = "wifi"
    const val LOCATION_SERVICES = "location_services"
    const val NEARBY_DEVICES = "nearby_devices"
    const val LOCATION_PERMISSION = "location_permission"
    const val STORAGE = "storage"
    const val NOTIFICATIONS = "notifications"
}

/**
 * The checklist for [role] on [sdkInt], in fix-order: device toggles first (a two-tap fix), then
 * runtime permissions, optional rows last.
 *
 * Pure and parameterized on [sdkInt] rather than reading [Build.VERSION] directly, so the whole
 * per-API matrix is unit-testable without an emulator - the API differences here are exactly what
 * makes this feature hard to get right:
 * - API ≤32 gates Wi-Fi P2P and scan results behind `ACCESS_FINE_LOCATION` **and** location
 *   services being switched on. API 33+ uses `NEARBY_WIFI_DEVICES`/`neverForLocation` instead and
 *   needs neither, which is why a transfer can work from a new phone and fail from an old one.
 * - `WRITE_EXTERNAL_STORAGE` (API ≤28) is receiver-only - it backs `MediaImporter`'s legacy write
 *   path, and a sender never writes anything.
 * - `POST_NOTIFICATIONS` (API 33+) only affects the transfer's progress/done notification.
 */
fun transferRequirements(
    role: TransferRole,
    sdkInt: Int = Build.VERSION.SDK_INT,
): List<TransferRequirement> = buildList {
    add(
        TransferRequirement(
            id = RequirementIds.WIFI,
            title = "Turn on Wi-Fi",
            explanation = "The two devices talk over their own direct Wi-Fi link. No internet is used.",
            action = RequirementAction.OpenWifiSettings,
        )
    )

    if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
        add(
            TransferRequirement(
                id = RequirementIds.LOCATION_SERVICES,
                title = "Turn on Location",
                explanation = "Android 12 and older require location services to find nearby " +
                    "devices over Wi-Fi. Your location is never read or shared.",
                action = RequirementAction.OpenLocationSettings,
            )
        )
        add(
            TransferRequirement(
                id = RequirementIds.LOCATION_PERMISSION,
                title = "Allow location access",
                explanation = "Android 12 and older require this permission to connect over Wi-Fi Direct.",
                action = RequirementAction.RequestPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            )
        )
    } else {
        add(
            TransferRequirement(
                id = RequirementIds.NEARBY_DEVICES,
                title = "Allow nearby devices",
                explanation = "Needed to connect to the other device over Wi-Fi Direct.",
                action = RequirementAction.RequestPermission(Manifest.permission.NEARBY_WIFI_DEVICES),
            )
        )
    }

    if (role == TransferRole.Receiver && sdkInt <= Build.VERSION_CODES.P) {
        add(
            TransferRequirement(
                id = RequirementIds.STORAGE,
                title = "Allow storage access",
                explanation = "Needed to save the songs you receive.",
                action = RequirementAction.RequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            )
        )
    }

    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        add(
            TransferRequirement(
                id = RequirementIds.NOTIFICATIONS,
                title = "Allow notifications",
                explanation = "Optional - lets you follow the transfer's progress while you use other apps.",
                action = RequirementAction.RequestPermission(Manifest.permission.POST_NOTIFICATIONS),
                blocking = false,
            )
        )
    }
}

fun TransferRequirement.isSatisfied(context: Context): Boolean = when (val action = action) {
    is RequirementAction.RequestPermission ->
        ContextCompat.checkSelfPermission(context, action.permission) == PackageManager.PERMISSION_GRANTED

    RequirementAction.OpenWifiSettings -> !WifiDirectTransport.wifiRadioOff(context)
    RequirementAction.OpenLocationSettings -> !WifiDirectTransport.locationServicesRequiredButOff(context)
}

/**
 * What still stands between the user and a working transfer. Empty means the checklist has nothing
 * to say and the caller should go straight ahead - see the callers in `PlaylistQrExportScreen` and
 * the QR-scan route, which check this at tap/scan time rather than showing the screen every time.
 */
fun unsatisfiedBlocking(context: Context, role: TransferRole): List<TransferRequirement> =
    transferRequirements(role).filter { it.blocking && !it.isSatisfied(context) }
