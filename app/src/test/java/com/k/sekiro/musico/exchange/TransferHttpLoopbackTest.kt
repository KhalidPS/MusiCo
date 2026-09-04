package com.k.sekiro.musico.exchange

import com.k.sekiro.musico.playmusic.data.exchange.TransferHttpClient
import com.k.sekiro.musico.playmusic.data.exchange.TransferHttpServer
import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import com.k.sekiro.musico.playmusic.domain.exchange.TransferSong
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Drives [TransferHttpServer] and [TransferHttpClient] against each other over real `127.0.0.1`
 * sockets - the "Loopback HTTP" verification bucket from the design doc. No Android framework or
 * device/emulator involved (the server's byte source is a plain lambda, not `ContentResolver`).
 */
class TransferHttpLoopbackTest {

    private val port = 28_988
    private val token = "test-token-123"
    private lateinit var server: TransferHttpServer
    private lateinit var client: TransferHttpClient

    private val songBytes = ByteArray(50_000) { (it % 251).toByte() }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private val manifest = TransferManifest(
        name = "Loopback Mix",
        songs = listOf(
            TransferSong(
                title = "Track One",
                artist = "Artist",
                album = "Album",
                durationMs = 180_000,
                sizeBytes = songBytes.size.toLong(),
                mime = "audio/mpeg",
                sha256 = sha256(songBytes),
                fileName = "track1.mp3",
            ),
        ),
    )

    @Before
    fun startServer() = runBlocking {
        server = TransferHttpServer(
            manifest = manifest,
            openSong = { index -> if (index == 0) ByteArrayInputStream(songBytes) else null },
            token = token,
            port = port,
        )
        server.start()
        client = TransferHttpClient(baseUrl = "http://127.0.0.1:$port", token = token)
        awaitServerReady()
    }

    @After
    fun stopServer() {
        client.close()
        server.stop()
    }

    private suspend fun awaitServerReady() {
        repeat(50) {
            try {
                client.fetchManifest()
                return
            } catch (_: Exception) {
                delay(50)
            }
        }
        fail("server did not become ready within the timeout")
    }

    @Test
    fun `fetches the manifest`() = runBlocking {
        assertEquals(manifest, client.fetchManifest())
    }

    @Test
    fun `downloads a song's full bytes`() = runBlocking {
        val out = ByteArrayOutputStream()
        client.downloadSong(index = 0, sink = out)
        assertArrayEquals(songBytes, out.toByteArray())
    }

    @Test
    fun `wrong token is rejected`() = runBlocking {
        val badClient = TransferHttpClient(baseUrl = "http://127.0.0.1:$port", token = "wrong-token")
        try {
            badClient.fetchManifest()
            fail("expected failure for a wrong bearer token")
        } catch (_: IllegalStateException) {
            // expected - fetchManifest() checks response.status.isSuccess() and throws
        } finally {
            badClient.close()
        }
    }

    @Test
    fun `range resume downloads only the remaining bytes`() = runBlocking {
        val resumeFrom = 20_000L
        val out = ByteArrayOutputStream()
        client.downloadSong(index = 0, sink = out, resumeFromBytes = resumeFrom)
        assertArrayEquals(songBytes.copyOfRange(resumeFrom.toInt(), songBytes.size), out.toByteArray())
    }

    @Test
    fun `progress callback reports increasing cumulative bytes ending at the full size`() = runBlocking {
        val progress = mutableListOf<Long>()
        client.downloadSong(index = 0, sink = ByteArrayOutputStream(), onProgress = { progress.add(it) })
        assertTrue(progress.isNotEmpty())
        assertEquals(songBytes.size.toLong(), progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `server progress callback reports increasing cumulative bytes ending at the full size`() = runBlocking {
        val progress = mutableListOf<Pair<Int, Long>>()
        val progressServer = TransferHttpServer(
            manifest = manifest,
            openSong = { index -> if (index == 0) ByteArrayInputStream(songBytes) else null },
            token = token,
            port = port + 1,
            onProgress = { index, bytesSent, _ -> progress.add(index to bytesSent) },
        )
        progressServer.start()
        val progressClient = TransferHttpClient(baseUrl = "http://127.0.0.1:${port + 1}", token = token)
        try {
            repeat(50) {
                try {
                    progressClient.fetchManifest()
                    return@repeat
                } catch (_: Exception) {
                    delay(50)
                }
            }
            progressClient.downloadSong(index = 0, sink = ByteArrayOutputStream())
        } finally {
            progressClient.close()
            progressServer.stop()
        }

        assertTrue(progress.isNotEmpty())
        assertTrue(progress.all { (index, _) -> index == 0 })
        assertEquals(songBytes.size.toLong(), progress.last().second)
        assertTrue(progress.map { it.second }.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `unknown song index returns not found without throwing the server`() = runBlocking {
        val out = ByteArrayOutputStream()
        try {
            client.downloadSong(index = 5, sink = out)
            fail("expected failure for an out-of-range index")
        } catch (_: IllegalStateException) {
            // expected - server responds 404, client's check(isSuccess()) throws
        }
    }
}
