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
        )
        assertEquals(listOf(internalA, sdC), r.toAdd)
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `a song no longer on the device is deleted`() {
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, internalB),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = setOf(internalRoot),
        )
        assertEquals(listOf(internalB), r.toDelete)
        assertTrue(r.toAdd.isEmpty())
    }

    @Test
    fun `a mounted volume emptied of audio has its stale rows purged`() {
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = bothMounted,
        )
        assertEquals(setOf(sdC, sdD), r.toDelete.toSet())
    }

    @Test
    fun `new song on a mounted volume is added`() {
        val sdNew = song("/storage/1A2B-3C4D/Music/new.mp3", 9)
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC),
            scannedSongs = listOf(internalA, sdC, sdNew),
            mountedVolumeRoots = bothMounted,
        )
        assertEquals(listOf(sdNew), r.toAdd)
        assertTrue(r.toDelete.isEmpty())
    }

    @Test
    fun `adds and deletes happen in the same pass`() {
        val sdNew = song("/storage/1A2B-3C4D/Music/new.mp3", 9)
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, internalB),
            scannedSongs = listOf(internalA, sdNew),
            mountedVolumeRoots = bothMounted,
        )
        assertEquals(listOf(sdNew), r.toAdd)
        assertEquals(listOf(internalB), r.toDelete)
    }

    @Test
    fun `SD card not mounted - its rows are NOT deleted`() {
        // Card ejected: getExternalFilesDirs stops reporting it and the scan can't see its files.
        // They are unreadable, not deleted - wiping them would cascade their playlist rows away.
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC, sdD),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = setOf(internalRoot),
        )
        assertTrue("SD rows must survive while the card is unmounted", r.toDelete.isEmpty())
    }

    @Test
    fun `moved or renamed file - same id, new path - is upserted, not deleted then re-added`() {
        val sdCMoved = song("/storage/1A2B-3C4D/Albums/c.mp3", 3) // same id, different path
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, sdC),
            scannedSongs = listOf(internalA, sdCMoved),
            mountedVolumeRoots = bothMounted,
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
        )
        assertTrue(r.toDelete.isEmpty())
        assertTrue(r.toAdd.isEmpty())
    }

    @Test
    fun `a row outside slash-storage is never reconcile-deleted`() {
        val legacy = song("/data/media/0/x.mp3", 11)
        val r = reconcileLibrary(
            roomSongs = listOf(internalA, legacy),
            scannedSongs = listOf(internalA),
            mountedVolumeRoots = bothMounted,
        )
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
