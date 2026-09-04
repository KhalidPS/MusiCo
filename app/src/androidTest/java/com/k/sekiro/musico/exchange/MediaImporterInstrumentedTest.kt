package com.k.sekiro.musico.exchange

import android.content.ContentUris
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.k.sekiro.musico.playmusic.data.exchange.MediaImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.security.MessageDigest

/**
 * Exercises [MediaImporter]'s real `MediaStore` `IS_PENDING` insert/clear/rollback on-device -
 * the app's first `MediaStore` write, so this needs a real `ContentResolver`, not a JVM unit test.
 * Cleans up every row it inserts.
 */
@RunWith(AndroidJUnit4::class)
class MediaImporterInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val importer = MediaImporter(context)
    private val insertedIds = mutableListOf<Long>()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun rowExists(id: Long): Boolean {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            id,
        )
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    @After
    fun cleanUp() {
        for (id in insertedIds) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                id,
            )
            context.contentResolver.delete(uri, null, null)
        }
        insertedIds.clear()
    }

    @Test
    fun importAudio_happyPath_insertsAVisibleNonPendingRow() = runBlocking {
        val bytes = ByteArray(10_000) { (it % 200).toByte() }
        val fileName = "musico_import_test_${System.currentTimeMillis()}.mp3"

        val result = importer.importAudio(
            displayName = fileName,
            mime = "audio/mpeg",
            expectedSize = bytes.size.toLong(),
            expectedSha256 = sha256(bytes),
            body = ByteArrayInputStream(bytes),
        )

        assertTrue("import should succeed, was: ${result.exceptionOrNull()}", result.isSuccess)
        val id = result.getOrThrow()
        insertedIds.add(id)

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            id,
        )
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.IS_PENDING, MediaStore.Audio.Media.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val pendingCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PENDING)
            assertEquals(0, cursor.getInt(pendingCol))
        } ?: throw AssertionError("expected a queryable row for $uri")

        context.contentResolver.openInputStream(uri)?.use { stream ->
            assertEquals(bytes.toList(), stream.readBytes().toList())
        } ?: throw AssertionError("expected to be able to read back $uri")
    }

    @Test
    fun importAudio_sha256Mismatch_rollsBackTheInsertedRow() = runBlocking {
        val bytes = ByteArray(5_000) { it.toByte() }
        val fileName = "musico_import_test_shafail_${System.currentTimeMillis()}.mp3"

        val result = importer.importAudio(
            displayName = fileName,
            mime = "audio/mpeg",
            expectedSize = bytes.size.toLong(),
            expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            body = ByteArrayInputStream(bytes),
        )

        assertTrue(result.isFailure)
        // Nothing to add to insertedIds/clean up - a successful rollback means no row survives.
    }

    @Test
    fun importAudio_sizeMismatch_rollsBackTheInsertedRow() = runBlocking {
        val bytes = ByteArray(5_000) { it.toByte() }
        val fileName = "musico_import_test_sizefail_${System.currentTimeMillis()}.mp3"

        val result = importer.importAudio(
            displayName = fileName,
            mime = "audio/mpeg",
            expectedSize = bytes.size.toLong() + 1,
            expectedSha256 = sha256(bytes),
            body = ByteArrayInputStream(bytes),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun importAudio_duplicateDisplayName_bothImportsSucceedWithDifferentIds() = runBlocking {
        val bytes = ByteArray(2_000) { it.toByte() }
        val fileName = "musico_import_test_dup_${System.currentTimeMillis()}.mp3"

        val first = importer.importAudio(
            displayName = fileName,
            mime = "audio/mpeg",
            expectedSize = bytes.size.toLong(),
            expectedSha256 = sha256(bytes),
            body = ByteArrayInputStream(bytes),
        )
        assertTrue(first.isSuccess)
        insertedIds.add(first.getOrThrow())

        val second = importer.importAudio(
            displayName = fileName,
            mime = "audio/mpeg",
            expectedSize = bytes.size.toLong(),
            expectedSha256 = sha256(bytes),
            body = ByteArrayInputStream(bytes),
        )
        assertTrue(second.isSuccess)
        insertedIds.add(second.getOrThrow())

        assertNotEquals(first.getOrThrow(), second.getOrThrow())
        assertTrue(rowExists(first.getOrThrow()))
        assertTrue(rowExists(second.getOrThrow()))
    }
}
