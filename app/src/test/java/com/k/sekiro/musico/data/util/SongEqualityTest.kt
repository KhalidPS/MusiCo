package com.k.sekiro.musico.data.util

import com.k.sekiro.musico.playmusic.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `Song.equals` used to cast `other` to the unrelated `SongUi` after only a `javaClass` check, so
 * comparing any two distinct `Song`s threw `ClassCastException`. Path is the identity here because
 * a rescan re-reads the same file into a fresh instance.
 */
class SongEqualityTest {

    private fun song(path: String, id: Long = 0, name: String = "n") =
        Song(name = name, path = path, id = id)

    @Test
    fun `same path is equal even when the other fields differ`() {
        assertEquals(song("/storage/emulated/0/a.mp3", id = 1, name = "A"),
            song("/storage/emulated/0/a.mp3", id = 2, name = "B"))
    }

    @Test
    fun `different paths are not equal even when the ids match`() {
        assertNotEquals(song("/storage/emulated/0/a.mp3", id = 1),
            song("/storage/emulated/0/b.mp3", id = 1))
    }

    @Test
    fun `comparing against an unrelated type returns false instead of throwing`() {
        // The old body cast `other` to SongUi after only a javaClass check; anything that is not
        // a Song reaching equals() must now simply be unequal.
        assertFalse(song("/storage/emulated/0/a.mp3", id = 1).equals("/storage/emulated/0/a.mp3"))
    }

    @Test
    fun `comparing against null returns false`() {
        assertFalse(song("/storage/emulated/0/a.mp3").equals(null))
    }

    @Test
    fun `equal songs have equal hash codes so set and map lookups work`() {
        val a = song("/storage/emulated/0/a.mp3", id = 1)
        val b = song("/storage/emulated/0/a.mp3", id = 2)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a in setOf(b))
    }
}
