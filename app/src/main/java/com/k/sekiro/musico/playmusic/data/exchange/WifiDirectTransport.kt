package com.k.sekiro.musico.playmusic.data.exchange

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.coroutines.resume

/** Wi-Fi credentials for the private link one device stood up for the other to join. */
data class TransferWifiCredentials(
    val ssid: String,
    val passphrase: String,
    val host: String,
)

sealed interface WifiGroupResult {
    data class Success(val credentials: TransferWifiCredentials) : WifiGroupResult
    data class Failed(val reason: String) : WifiGroupResult
}

/**
 * Sender-side and receiver-side halves of the private Wi-Fi link a transfer session runs over.
 * Constructed and owned by `TransferService` for the duration of one session - not a Koin
 * singleton (it holds live join/group state for exactly one transfer).
 *
 * Sender: [createGroup] tries `WifiP2pManager.createGroup` (Wi-Fi Direct Group Owner, address
 * `192.168.49.1`) first, falling back to [WifiManager.startLocalOnlyHotspot] when Wi-Fi Direct is
 * unavailable/blocked ([shouldFallBackToLocalOnlyHotspot] decides which, kept as a pure function).
 *
 * Receiver: [joinGroup] uses `WifiNetworkSpecifier` + `ConnectivityManager.requestNetwork` on API
 * 29+, binding the process to that network so the transfer's Ktor client routes over this link
 * rather than the device's normal Wi-Fi/cellular route - unbind on [leaveGroup]. API 24-28 falls
 * back to the deprecated `WifiManager.addNetwork`/`enableNetwork` pair.
 *
 * Real `WifiP2pManager`/`ConnectivityManager` behavior here can't be exercised off-device (needs
 * two physical devices - design doc Limitation §4); this class is deliberately thin so only the
 * two top-level pure functions below need to carry any unit-tested logic.
 */
class WifiDirectTransport(private val context: Context) {

    companion object {
        private const val GROUP_INFO_RETRY_ATTEMPTS = 5
        private const val GROUP_INFO_RETRY_DELAY_MS = 200L
    }

    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val channel = wifiP2pManager?.initialize(context, context.mainLooper, null)

    private var boundNetwork: Network? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    // ---- Sender ---------------------------------------------------------------------------

    /**
     * Never throws - every platform call inside is guarded, since `WifiP2pManager`/`WifiManager`
     * APIs can fail synchronously (a thrown exception) as well as asynchronously (a callback), and
     * an uncaught one here would otherwise crash the whole app. Checks [WifiManager.isWifiEnabled]
     * first - Wi-Fi Direct and local-only-hotspot both need the Wi-Fi radio on, and failing fast
     * with a clear reason beats attempting (and failing partway through) the full fallback chain.
     */
    suspend fun createGroup(): WifiGroupResult {
        if (!wifiManager.isWifiEnabled) {
            return WifiGroupResult.Failed("Wi-Fi is turned off - turn it on to send songs")
        }
        return try {
            val manager = wifiP2pManager
            val ch = channel
            if (manager == null || ch == null) {
                startLocalOnlyHotspot()
            } else {
                when (val outcome = attemptCreateGroup(manager, ch)) {
                    is GroupCreationOutcome.Success -> WifiGroupResult.Success(outcome.credentials)
                    is GroupCreationOutcome.Failed ->
                        if (shouldFallBackToLocalOnlyHotspot(outcome.reason)) {
                            startLocalOnlyHotspot()
                        } else {
                            WifiGroupResult.Failed(wifiP2pFailureReasonName(outcome.reason))
                        }
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            WifiGroupResult.Failed(ex.message ?: ex.javaClass.simpleName)
        }
    }

    private suspend fun attemptCreateGroup(
        manager: WifiP2pManager,
        ch: WifiP2pManager.Channel,
    ): GroupCreationOutcome {
        val failureReason = suspendCancellableCoroutine<Int?> { continuation ->
            manager.createGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onFailure(reason: Int) {
                    if (continuation.isActive) continuation.resume(reason)
                }
            })
        }
        if (failureReason != null) return GroupCreationOutcome.Failed(failureReason)

        // createGroup's onSuccess() only means the framework accepted the request - the group's
        // info (and the interface behind it) can take a moment to actually become queryable.
        // Observed on-device: requestGroupInfo can return null for a few hundred ms right after
        // onSuccess() even though the group goes on to form correctly - treating one null as a
        // hard failure was triggering an unnecessary, conflicting fallback to
        // startLocalOnlyHotspot() while the P2P group was still coming up (and that fallback then
        // failed too, since the radio was still busy with the P2P interface). Retry briefly
        // instead of giving up on the first null.
        repeat(GROUP_INFO_RETRY_ATTEMPTS) { attempt ->
            val group = requestGroupInfoOnce(manager, ch)
            if (group != null) {
                return GroupCreationOutcome.Success(
                    TransferWifiCredentials(
                        ssid = group.networkName,
                        passphrase = group.passphrase,
                        host = "192.168.49.1",
                    )
                )
            }
            if (attempt < GROUP_INFO_RETRY_ATTEMPTS - 1) delay(GROUP_INFO_RETRY_DELAY_MS)
        }
        return GroupCreationOutcome.Failed(WifiP2pManager.ERROR)
    }

    private suspend fun requestGroupInfoOnce(
        manager: WifiP2pManager,
        ch: WifiP2pManager.Channel,
    ): WifiP2pGroup? = suspendCancellableCoroutine { continuation ->
        manager.requestGroupInfo(ch) { group: WifiP2pGroup? ->
            if (continuation.isActive) continuation.resume(group)
        }
    }

    /**
     * `WifiManager.startLocalOnlyHotspot` can throw `IllegalStateException` **synchronously**
     * ("Caller already has an active LocalOnlyHotspot request") rather than reporting failure via
     * the callback - specifically when this process already holds an unreleased reservation from
     * an earlier attempt. [localOnlyHotspotReservation] + closing it in [stopGroup] is what
     * prevents that; the try/catch here is a last-resort net for whatever this process doesn't
     * control (another caller in the same process, a stale reservation surviving process restart,
     * OEM quirks, ...), converting it into a graceful [WifiGroupResult.Failed] instead of a crash.
     */
    private suspend fun startLocalOnlyHotspot(): WifiGroupResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return WifiGroupResult.Failed("local-only hotspot needs API 26+")
        }
        return try {
            suspendCancellableCoroutine { continuation ->
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        if (!continuation.isActive) return
                        localOnlyHotspotReservation = reservation
                        val credentials = readLocalOnlyHotspotCredentials(reservation)
                        continuation.resume(
                            if (credentials == null) {
                                WifiGroupResult.Failed("no usable SSID/passphrase in the LOH reservation")
                            } else {
                                WifiGroupResult.Success(credentials)
                            }
                        )
                    }

                    override fun onFailed(reason: Int) {
                        if (continuation.isActive) {
                            continuation.resume(WifiGroupResult.Failed("local-only hotspot failed: $reason"))
                        }
                    }
                }, null)
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: IllegalStateException) {
            WifiGroupResult.Failed("Wi-Fi hotspot is busy - try again in a moment")
        } catch (ex: SecurityException) {
            WifiGroupResult.Failed("Missing permission for the Wi-Fi hotspot")
        }
    }

    @Suppress("DEPRECATION")
    private fun readLocalOnlyHotspotCredentials(
        reservation: WifiManager.LocalOnlyHotspotReservation,
    ): TransferWifiCredentials? {
        // Unlike Wi-Fi Direct (always 192.168.49.1, a fixed platform constant - confirmed against
        // real WifiP2pService logs), a local-only hotspot's gateway address isn't fixed or exposed
        // by SoftApConfiguration/WifiConfiguration - it's this device's own address on the AP
        // interface it just brought up, so look that up directly.
        val host = localHotspotGatewayAddress() ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val config = reservation.softApConfiguration
            val ssid = config.ssid ?: return null
            val passphrase = config.passphrase ?: return null
            return TransferWifiCredentials(ssid = ssid, passphrase = passphrase, host = host)
        }
        val config = reservation.wifiConfiguration ?: return null
        return TransferWifiCredentials(ssid = config.SSID, passphrase = config.preSharedKey, host = host)
    }

    /** This device's own IPv4 address on whatever local AP/hotspot interface just came up - that
     * address is the gateway other devices dial. Prefers a `.1`-ending address (the conventional
     * gateway suffix) since a device can have more than one active interface (e.g. mobile data). */
    private fun localHotspotGatewayAddress(): String? {
        val addresses = try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .filter { !it.startsWith("127.") }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
        return addresses.firstOrNull { it.endsWith(".1") } ?: addresses.firstOrNull()
    }

    /**
     * Awaits the group's actual removal (with a short timeout) rather than firing `removeGroup`
     * and returning immediately - a caller that turns around and calls [createGroup] again right
     * away (e.g. Cancel then "Send with songs" again) would otherwise race the still-tearing-down
     * `p2p-wlan0-0` interface, which is a real source of the bind race [TransferHttpServer]'s
     * wildcard-bind KDoc describes.
     */
    suspend fun stopGroup() {
        // Must close this explicitly - startLocalOnlyHotspot throws IllegalStateException on the
        // *next* call from this process until the current reservation is released, and the OS
        // doesn't reliably do that promptly on its own (see startLocalOnlyHotspot's KDoc).
        try {
            localOnlyHotspotReservation?.close()
        } catch (_: Exception) {
        }
        localOnlyHotspotReservation = null

        val manager = wifiP2pManager
        val ch = channel
        if (manager == null || ch == null) return
        try {
            withTimeoutOrNull(2_000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    manager.removeGroup(ch, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onFailure(reason: Int) {
                            // Nothing was there to remove, or the framework rejected it - either
                            // way don't block teardown on it.
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    })
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
        }
    }

    // ---- Receiver -------------------------------------------------------------------------

    /** Never throws - see [createGroup]'s KDoc; the same synchronous-exception risk applies here. */
    suspend fun joinGroup(credentials: TransferWifiCredentials): Boolean {
        if (!wifiManager.isWifiEnabled) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                joinViaNetworkSpecifier(credentials)
            } else {
                joinViaLegacyWifiConfiguration(credentials)
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            android.util.Log.e("WifiDirectTransport", "joinGroup failed", ex)
            false
        }
    }

    private suspend fun joinViaNetworkSpecifier(credentials: TransferWifiCredentials): Boolean {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(credentials.ssid)
            .setWpa2Passphrase(credentials.passphrase)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    boundNetwork = network
                    connectivityManager.bindProcessToNetwork(network)
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onUnavailable() {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            networkCallback = callback
            connectivityManager.requestNetwork(request, callback)
        }
    }

    @Suppress("DEPRECATION")
    private fun joinViaLegacyWifiConfiguration(credentials: TransferWifiCredentials): Boolean {
        val config = WifiConfiguration().apply {
            SSID = "\"${credentials.ssid}\""
            preSharedKey = "\"${credentials.passphrase}\""
        }
        val networkId = wifiManager.addNetwork(config)
        if (networkId == -1) return false
        wifiManager.disconnect()
        val enabled = wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
        return enabled
    }

    /** Unbinds [bindProcessToNetwork] and releases the network request - call on transfer cleanup.
     * Never throws (e.g. `unregisterNetworkCallback` on an already-unregistered callback throws). */
    fun leaveGroup() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            networkCallback = null
        }
        if (boundNetwork != null) {
            try {
                connectivityManager.bindProcessToNetwork(null)
            } catch (_: Exception) {
            }
            boundNetwork = null
        }
    }
}

private sealed interface GroupCreationOutcome {
    data class Success(val credentials: TransferWifiCredentials) : GroupCreationOutcome
    data class Failed(val reason: Int) : GroupCreationOutcome
}

/**
 * Pure, unit-testable: does a `WifiP2pManager.ActionListener.onFailure` reason justify falling
 * back to [WifiManager.startLocalOnlyHotspot] rather than surfacing the error directly? Every
 * reason does today (`ERROR`, `P2P_UNSUPPORTED`, `BUSY`) - Wi-Fi Direct being unavailable for any
 * reason should fall back, not fail the whole transfer outright.
 */
fun shouldFallBackToLocalOnlyHotspot(reason: Int): Boolean = true

/** Pure, unit-testable mapping from `WifiP2pManager`'s raw int failure reasons to a readable name. */
fun wifiP2pFailureReasonName(reason: Int): String = when (reason) {
    WifiP2pManager.ERROR -> "ERROR"
    WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
    WifiP2pManager.BUSY -> "BUSY"
    else -> "UNKNOWN($reason)"
}
