package com.k.sekiro.musico.playmusic.data.exchange

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * Writes a downloaded audio file into MediaStore - the app's first MediaStore *write* (every other
 * `SongsRepository`/`DataHelperFunctions` path only reads or deletes). Verifies size + sha256
 * while copying, rolling back (deleting the row/file) on any mismatch so a corrupt or interrupted
 * transfer never leaves a half-written or unverified file behind.
 *
 * Returns the inserted MediaStore `_ID`, which is what `Song.id` will be once the file is picked
 * up by the app's existing rescan pipeline (`SongsRepository.startObservingSongChanges` /
 * `getAllStorageSongs()`). Deliberately narrow in scope: the caller (`TransferService`) is
 * responsible for syncing that id into Room (e.g. `getAllStorageSongs()` + `addSong(...)`) before
 * referencing it in a `PlaylistSong` - this class only owns the byte-copy/verify concern, so it
 * doesn't duplicate `DataHelperFunctions.getSongsByUri`'s cursor-mapping logic.
 */
class MediaImporter(private val context: Context) {

    /**
     * @param expectedSize the manifest's `TransferSong.sizeBytes`.
     * @param expectedSha256 the manifest's `TransferSong.sha256`, lowercase hex.
     * @param onProgress cumulative bytes written so far.
     */
    suspend fun importAudio(
        displayName: String,
        mime: String,
        expectedSize: Long,
        expectedSha256: String,
        body: InputStream,
        onProgress: (bytesWritten: Long) -> Unit = {},
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            importViaMediaStore(displayName, mime, expectedSize, expectedSha256, body, onProgress)
        } else {
            importViaLegacyFile(displayName, mime, expectedSize, expectedSha256, body, onProgress)
        }
    }

    private fun importViaMediaStore(
        displayName: String,
        mime: String,
        expectedSize: Long,
        expectedSha256: String,
        body: InputStream,
        onProgress: (Long) -> Unit,
    ): Result<Long> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusiCo")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: return Result.failure(IllegalStateException("MediaStore insert returned a null Uri"))

        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
            resolver.delete(uri, null, null)
            return Result.failure(IllegalStateException("could not open an output stream for $uri"))
        }
        val verified = copyAndVerify(outputStream, expectedSize, expectedSha256, body, onProgress)
        if (verified.isFailure) {
            resolver.delete(uri, null, null)
            return Result.failure(verified.exceptionOrNull() ?: IllegalStateException("import failed"))
        }

        resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        return Result.success(ContentUris.parseId(uri))
    }

    private suspend fun importViaLegacyFile(
        displayName: String,
        mime: String,
        expectedSize: Long,
        expectedSha256: String,
        body: InputStream,
        onProgress: (Long) -> Unit,
    ): Result<Long> {
        @Suppress("DEPRECATION")
        val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusiCo")
        if (!musicDir.exists() && !musicDir.mkdirs()) {
            return Result.failure(IllegalStateException("could not create $musicDir"))
        }
        val target = uniqueFile(musicDir, displayName)

        val verified = copyAndVerify(target.outputStream(), expectedSize, expectedSha256, body, onProgress)
        if (verified.isFailure) {
            target.delete()
            return Result.failure(verified.exceptionOrNull() ?: IllegalStateException("import failed"))
        }

        return suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime)) { _, scannedUri ->
                val id = scannedUri?.let { runCatching { ContentUris.parseId(it) }.getOrNull() }
                if (id != null) {
                    continuation.resume(Result.success(id))
                } else {
                    target.delete()
                    continuation.resume(Result.failure(IllegalStateException("media scan did not return a usable id for $target")))
                }
            }
        }
    }

    /** Copies [body] into [sink] (closing it either way) while hashing, verifying size + sha256. */
    private fun copyAndVerify(
        sink: java.io.OutputStream,
        expectedSize: Long,
        expectedSha256: String,
        body: InputStream,
        onProgress: (Long) -> Unit,
    ): Result<Unit> {
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L
        try {
            sink.use { out ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = body.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    written += read
                    onProgress(written)
                }
            }
        } catch (ex: Exception) {
            return Result.failure(ex)
        }
        if (written != expectedSize) {
            return Result.failure(
                IllegalStateException("size mismatch: expected $expectedSize bytes, got $written")
            )
        }
        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            return Result.failure(
                IllegalStateException("sha256 mismatch: expected $expectedSha256, got $actualSha256")
            )
        }
        return Result.success(Unit)
    }

    /** MediaStore auto-uniquifies `DISPLAY_NAME` on the API 29+ path; the legacy path needs to do it itself. */
    private fun uniqueFile(dir: File, displayName: String): File {
        val dot = displayName.lastIndexOf('.')
        val base = if (dot > 0) displayName.substring(0, dot) else displayName
        val ext = if (dot > 0) displayName.substring(dot) else ""
        var candidate = File(dir, displayName)
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base (${suffix++})$ext")
        }
        return candidate
    }
}
