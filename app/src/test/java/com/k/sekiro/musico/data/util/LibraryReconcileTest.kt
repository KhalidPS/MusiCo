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

    private val internalRoot = "/storage/emulated/0"
    private val sdRoot = "/storage/1A2B-3C4D"
    private val bothMounted = setOf(internalRoot, sdRoot)

    @Test
    fun `empty room - everything scanned is added, nothing deleted`() {
        val r = reconcileLibrary(
            roomSongs = emptyList(),
            scannedSongs = listOf(internalA, sdC),
            mountedVolumeRoots = bothMounted,
            allowDeletes = true,
        )
        assertEquals(listOf(internalA, sdC), r.toAdd)
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `allowDeletes false - a genuinely gone song is NOT deleted (add-only automatic sync)`() {
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, internalB),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = setOf(internalRoot),
            allowDeletes = false,
        )
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `allowDeletes true - a song gone from a mounted volume IS deleted`() {
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, internalB),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = setOf(internalRoot),
            allowDeletes = true,
        )
        assertEquals(listOf(internalB), r.toDelete)
        assertTrue(r.toAdd.isEmpty())
    }

    @Test
    fun `SD card not mounted - its rows are NOT deleted even on an explicit rescan`() {
        // card ejected: only internal is mounted, scan returns only internal songs
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = setOf(internalRoot),
            allowDeletes = true,
        )
        assertTrue("SD rows must survive while the card is unmounted", r.toDelete.isEmpty())
    }

    @Test
    fun `mounted volume emptied of audio - its stale rows ARE purged on rescan`() {
        // user deleted every song on the SD card; the card is still mounted
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = bothMounted,
            allowDeletes = true,
        )
        assertEquals(setOf(sdC, sdD), r.toDelete.toSet())
    }

    @Test
    fun `partial index of a mounted volume does NOT delete the not-yet-scanned rows on an automatic sync`() {
        // MediaProvider has indexed only sdC so far; sdD not yet seen
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA, sdC),
            mountedVolumeRoots = bothMounted,
            allowDeletes = false,
        )
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `moved or renamed file - same id, new path - is upserted, not deleted then re-added`() {
        val sdCMoved = song("/storage/1A2B-3C4D/Albums/c.mp3", 3) // same id, different path
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC),
            scannedSongs = listOf(internalA, sdCMoved),
            mountedVolumeRoots = bothMounted,
            allowDeletes = true,
        )
        assertTrue("moved file must not be reconcile-deleted (cascade guard)", r.toDelete.isEmpty())
        assertEquals(listOf(sdCMoved), r.toAdd) // upsert handles the path change in place
    }

    @Test
    fun `completely empty scan is treated as a failed query - nothing added or deleted`() {
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC),
            scannedSongs = emptyList(),
            mountedVolumeRoots = bothMounted,
            allowDeletes = true,
        )
        assertTrue(r.toDelete.isEmpty())
        assertTrue(r.toAdd.isEmpty())
    }

    @Test
    fun `new song on a mounted volume is added`() {
        val sdNew = song("/storage/1A2B-3C4D/Music/new.mp3", 9)
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC),
            scannedSongs = listOf(internalA, sdC, sdNew),
            mountedVolumeRoots = bothMounted,
            allowDeletes = false,
        )
        assertEquals(listOf(sdNew), r.toAdd)
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `volumeRootOf handles primary, removable and non-storage paths`() {
        assertEquals("/storage/emulated/0", volumeRootOf("/storage/emulated/0/Music/x.mp3"))
        assertEquals("/storage/emulated/10", volumeRootOf("/storage/emulated/10/Music/x.mp3"))
        assertEquals("/storage/1A2B-3C4D", volumeRootOf("/storage/1A2B-3C4D/Music/x.mp3"))
        assertNull(volumeRootOf("/data/media/0/x.mp3"))
        assertNull(volumeRootOf(""))
    }
}
