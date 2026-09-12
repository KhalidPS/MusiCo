package com.k.sekiro.musico.data.util

import com.k.sekiro.musico.playmusic.data.util.buildSongSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the `IS_RECORDING` fallback. Some OEM MediaProvider forks report SDK 30 without actually
 * adding the column, so the query throws instead of just omitting rows - and the whole library
 * disappears. `getSongsByUri` retries once with `excludeRecordings = false`; these assert that the
 * retry really does produce a selection the fork can run.
 */
class SongSelectionTest {

    private val isRecordingClause = "IFNULL(is_recording, 0) = 0"

    @Test
    fun `excludeRecordings true includes the IS_RECORDING clause`() {
        val (selection, _) = buildSongSelection(excludeRecordings = true)
        assertTrue(selection, selection.contains(isRecordingClause))
    }

    @Test
    fun `excludeRecordings false omits the IS_RECORDING clause - the OEM-fork retry`() {
        val (selection, _) = buildSongSelection(excludeRecordings = false)
        assertFalse(selection, selection.contains("is_recording"))
    }

    @Test
    fun `the recording clause is the only difference between the two selections`() {
        val (withRecordings, argsWith) = buildSongSelection(excludeRecordings = true)
        val (without, argsWithout) = buildSongSelection(excludeRecordings = false)

        assertEquals(without, withRecordings.replace(" AND $isRecordingClause", ""))
        // The clause takes no bind argument, so the retry must not shift the args array.
        assertEquals(argsWith.toList(), argsWithout.toList())
    }

    @Test
    fun `every placeholder has exactly one bind argument`() {
        listOf(true, false).forEach { excludeRecordings ->
            val (selection, args) = buildSongSelection(excludeRecordings)
            assertEquals(
                "placeholder/argument mismatch for excludeRecordings=$excludeRecordings",
                selection.count { it == '?' },
                args.size,
            )
        }
    }

    @Test
    fun `selection filters out the non-music flags and short clips`() {
        val (selection, args) = buildSongSelection(excludeRecordings = true)
        assertTrue(selection, selection.contains("is_music != 0"))
        assertTrue(selection, selection.contains("IFNULL(is_ringtone, 0) = 0"))
        assertTrue(selection, selection.contains("IFNULL(is_alarm, 0) = 0"))
        assertTrue(selection, selection.contains("IFNULL(is_notification, 0) = 0"))
        assertTrue(selection, selection.contains("duration > ?"))
        assertTrue(args.toList().toString(), args.contains("1463"))
    }

    @Test
    fun `no user-supplied value is interpolated into the selection string`() {
        // Everything variable goes through a ? placeholder; the SQL text itself is all constants.
        val (selection, args) = buildSongSelection(excludeRecordings = true)
        args.forEach { arg ->
            assertFalse("'$arg' was inlined into the selection", selection.contains(arg))
        }
    }
}
