package com.k.sekiro.musico.player

import com.k.sekiro.musico.playmusic.presenation.player.QueueAnchor
import com.k.sekiro.musico.playmusic.presenation.player.resolveQueueAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueAnchorTest {

    private val a = "/storage/emulated/0/Music/a.mp3"
    private val b = "/storage/emulated/0/Music/b.mp3"
    private val c = "/storage/emulated/0/Music/c.mp3"
    private val d = "/storage/emulated/0/Music/d.mp3"

    @Test
    fun `the playing song surviving keeps its position at its new index`() {
        // "a" was deleted, so what used to be index 1 is now index 0.
        val anchor = resolveQueueAnchor(
            paths = listOf(b, c, d),
            playingPath = b,
            uiPath = b,
            previousIndex = 1,
        )

        assertEquals(QueueAnchor(index = 0, keepPosition = true), anchor)
    }

    @Test
    fun `the ui song is used when the player has nothing loaded yet`() {
        val anchor = resolveQueueAnchor(
            paths = listOf(a, b, c),
            playingPath = null,
            uiPath = c,
            previousIndex = 0,
        )

        assertEquals(QueueAnchor(index = 2, keepPosition = true), anchor)
    }

    /** The case this exists for: deleting the song that is playing used to make indexOf answer
     * -1, which is what reached setMediaItems and the pager. */
    @Test
    fun `deleting the playing song falls back to whatever took its place`() {
        val anchor = resolveQueueAnchor(
            paths = listOf(a, b, d),
            playingPath = c,
            uiPath = c,
            previousIndex = 2,
        )

        assertEquals(QueueAnchor(index = 2, keepPosition = false), anchor)
    }

    @Test
    fun `deleting the playing last song falls back to the new last song`() {
        val anchor = resolveQueueAnchor(
            paths = listOf(a, b),
            playingPath = d,
            uiPath = d,
            previousIndex = 3,
        )

        assertEquals(QueueAnchor(index = 1, keepPosition = false), anchor)
    }

    @Test
    fun `deleting everything before the playing song falls back to the first song`() {
        val anchor = resolveQueueAnchor(
            paths = listOf(d),
            playingPath = b,
            uiPath = b,
            previousIndex = 1,
        )

        assertEquals(QueueAnchor(index = 0, keepPosition = false), anchor)
    }

    @Test
    fun `an emptied library has nothing to anchor to`() {
        assertNull(
            resolveQueueAnchor(
                paths = emptyList(),
                playingPath = a,
                uiPath = a,
                previousIndex = 0,
            )
        )
    }

    @Test
    fun `a player with no current index falls back to the first song`() {
        val anchor = resolveQueueAnchor(
            paths = listOf(a, b),
            playingPath = c,
            uiPath = null,
            previousIndex = -1,
        )

        assertEquals(QueueAnchor(index = 0, keepPosition = false), anchor)
    }
}
