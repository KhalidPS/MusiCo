package com.k.sekiro.musico.data.util

import com.k.sekiro.musico.playmusic.data.util.reconcileLibrary
import com.k.sekiro.musico.playmusic.data.util.volumeRootOf
import com.k.sekiro.musico.playmusic.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryReconcileTest {

    private fun song(path: String, id: Long) =
        Song(name = path.substringAfterLast('/'), path = path, id = id)

    private val internalA = song("/storage/emulated/0/Music/a.mp3", 1)
    private val internalB = song("/storage/emulated/0/Music/b.mp3", 2)
    private val sdC = song("/storage/1A2B-3C4D/Music/c.mp3", 3)
    private val sdD = song("/storage/1A2B-3C4D/Music/d.mp3", 4)

    @Test
    fun `empty room - everything scanned is added, nothing deleted`() {
        val r = reconcileLibrary(roomSongs = emptyList(), scannedSongs = listOf(internalA, sdC))
        assertEquals(listOf(internalA, sdC), r.toAdd)
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `song genuinely gone from a mounted volume is deleted`() {
        // primary storage was scanned (A present) and B is missing -> B really is gone
        val r = reconcileLibrary(roomSongs = listOf(internalA, internalB), scannedSongs = listOf(internalA))
        assertEquals(listOf(internalB), r.toDelete)
        assertTrue(r.toAdd.isEmpty())
    }

    @Test
    fun `SD card absent from the scan does NOT delete its songs (cascade-wipe guard)`() {
        // card ejected / still mounting: scan returns only internal songs
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA),
        )
        assertTrue("SD songs must not be reconcile-deleted when the card isn't in the scan", r.toDelete.isEmpty())
    }

    @Test
    fun `SD present in scan - a missing SD song IS deleted, a new SD song IS added`() {
        // card mounted: scan sees sdC but not sdD; sdEnew is brand new
        val sdEnew = song("/storage/1A2B-3C4D/Music/e.mp3", 5)
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA, sdC, sdEnew),
        )
        assertEquals(listOf(sdD), r.toDelete)
        assertEquals(listOf(sdEnew), r.toAdd)
    }

    @Test
    fun `volumeRootOf handles primary, self-primary, removable and non-storage paths`() {
        assertEquals("/storage/emulated/0", volumeRootOf("/storage/emulated/0/Music/x.mp3"))
        assertEquals("/storage/emulated/10", volumeRootOf("/storage/emulated/10/Music/x.mp3"))
        assertEquals("/storage/self/primary", volumeRootOf("/storage/self/primary/Music/x.mp3"))
        assertEquals("/storage/1A2B-3C4D", volumeRootOf("/storage/1A2B-3C4D/Music/x.mp3"))
        assertNull(volumeRootOf("/data/media/0/x.mp3"))
        assertNull(volumeRootOf(""))
    }
}
