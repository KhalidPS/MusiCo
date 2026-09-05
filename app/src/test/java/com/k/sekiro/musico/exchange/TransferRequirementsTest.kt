package com.k.sekiro.musico.exchange

import android.Manifest
import com.k.sekiro.musico.playmusic.presenation.exchange.preparations.RequirementAction
import com.k.sekiro.musico.playmusic.presenation.exchange.preparations.RequirementIds
import com.k.sekiro.musico.playmusic.presenation.exchange.preparations.TransferRole
import com.k.sekiro.musico.playmusic.presenation.exchange.preparations.transferRequirements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-API-level requirement matrix is the whole point of this screen - getting it wrong is
 * what produced the original bug (an Android 11 device failing with no explanation because
 * location services were off). [transferRequirements] takes `sdkInt` precisely so every row can
 * be asserted here without a device.
 */
class TransferRequirementsTest {

    private fun ids(role: TransferRole, sdkInt: Int) =
        transferRequirements(role, sdkInt).map { it.id }

    @Test
    fun `location services and permission are required below API 33`() {
        for (sdkInt in listOf(24, 28, 29, 30, 31, 32)) {
            for (role in TransferRole.entries) {
                val ids = ids(role, sdkInt)
                assertTrue(
                    "API $sdkInt/$role must require the location toggle",
                    ids.contains(RequirementIds.LOCATION_SERVICES),
                )
                assertTrue(
                    "API $sdkInt/$role must require location permission",
                    ids.contains(RequirementIds.LOCATION_PERMISSION),
                )
                assertFalse(
                    "API $sdkInt/$role must not ask for NEARBY_WIFI_DEVICES",
                    ids.contains(RequirementIds.NEARBY_DEVICES),
                )
            }
        }
    }

    @Test
    fun `API 33 plus swaps location for nearby devices`() {
        for (role in TransferRole.entries) {
            val ids = ids(role, 33)
            assertTrue(ids.contains(RequirementIds.NEARBY_DEVICES))
            assertFalse(ids.contains(RequirementIds.LOCATION_SERVICES))
            assertFalse(ids.contains(RequirementIds.LOCATION_PERMISSION))
        }
    }

    @Test
    fun `legacy storage is receiver-only and only up to API 28`() {
        assertTrue(ids(TransferRole.Receiver, 28).contains(RequirementIds.STORAGE))
        assertFalse(
            "a sender never writes files, so it must not ask for storage",
            ids(TransferRole.Sender, 28).contains(RequirementIds.STORAGE),
        )
        assertFalse(ids(TransferRole.Receiver, 29).contains(RequirementIds.STORAGE))
    }

    @Test
    fun `notifications row exists only on API 33 plus and never blocks`() {
        assertFalse(ids(TransferRole.Sender, 32).contains(RequirementIds.NOTIFICATIONS))
        val notifications = transferRequirements(TransferRole.Sender, 33)
            .single { it.id == RequirementIds.NOTIFICATIONS }
        assertFalse(
            "blocking on notifications would lock out anyone who permanently denied them",
            notifications.blocking,
        )
        assertEquals(
            RequirementAction.RequestPermission(Manifest.permission.POST_NOTIFICATIONS),
            notifications.action,
        )
    }

    @Test
    fun `wifi is always required and listed first`() {
        for (sdkInt in listOf(24, 29, 33, 34)) {
            for (role in TransferRole.entries) {
                assertEquals(
                    "the Wi-Fi toggle is the quickest fix, so it leads the list",
                    RequirementIds.WIFI,
                    ids(role, sdkInt).first(),
                )
            }
        }
    }

    @Test
    fun `every blocking requirement is actionable`() {
        for (sdkInt in listOf(24, 28, 29, 32, 33, 34)) {
            for (role in TransferRole.entries) {
                transferRequirements(role, sdkInt).forEach { requirement ->
                    assertTrue(
                        "${requirement.id} needs a non-empty title and explanation to be useful",
                        requirement.title.isNotBlank() && requirement.explanation.isNotBlank(),
                    )
                }
            }
        }
    }
}
