package com.k.sekiro.musico.exchange

import android.net.wifi.p2p.WifiP2pManager
import com.k.sekiro.musico.playmusic.data.exchange.shouldFallBackToLocalOnlyHotspot
import com.k.sekiro.musico.playmusic.data.exchange.wifiP2pFailureReasonName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the small pure decision/parsing logic factored out of [com.k.sekiro.musico.playmusic
 * .data.exchange.WifiDirectTransport] - the real `WifiP2pManager`/`ConnectivityManager` calls
 * around it can't be exercised off-device (needs two physical devices).
 */
class WifiDirectTransportTest {

    @Test
    fun `every known Wi-Fi Direct failure reason falls back to local-only hotspot`() {
        assertTrue(shouldFallBackToLocalOnlyHotspot(WifiP2pManager.ERROR))
        assertTrue(shouldFallBackToLocalOnlyHotspot(WifiP2pManager.P2P_UNSUPPORTED))
        assertTrue(shouldFallBackToLocalOnlyHotspot(WifiP2pManager.BUSY))
    }

    @Test
    fun `an unrecognized failure reason still falls back`() {
        assertTrue(shouldFallBackToLocalOnlyHotspot(-999))
    }

    @Test
    fun `failure reason names are readable`() {
        assertEquals("ERROR", wifiP2pFailureReasonName(WifiP2pManager.ERROR))
        assertEquals("P2P_UNSUPPORTED", wifiP2pFailureReasonName(WifiP2pManager.P2P_UNSUPPORTED))
        assertEquals("BUSY", wifiP2pFailureReasonName(WifiP2pManager.BUSY))
        assertEquals("UNKNOWN(-999)", wifiP2pFailureReasonName(-999))
    }
}
