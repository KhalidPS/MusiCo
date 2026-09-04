package com.k.sekiro.musico.playmusic.data.exchange

import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Serves this device's side of an audio transfer: the [manifest] (all of this device's tracks in
 * the exported playlist) and, on request, the raw bytes of each one. Runs only for the duration of
 * one transfer session, owned and torn down by `TransferService` - not a long-lived singleton.
 *
 * Binds the wildcard address (`0.0.0.0`), not the specific IP advertised in the pairing QR
 * (`TransferWifiCredentials.host`). There's an inherent race between `WifiP2pManager.createGroup`
 * reporting success and the `p2p-wlan0-0` interface actually having that literal address assigned
 * - binding to it directly intermittently throws `BindException: Cannot assign requested address`
 * (worse right after a quick cancel+retry, since `WifiDirectTransport.stopGroup`'s `removeGroup`
 * call doesn't wait for the old interface to finish tearing down). Binding wildcard sidesteps the
 * race entirely: the socket starts accepting on whatever interface comes up, whenever it does.
 *
 * [openSong] must open the bytes for `manifest.songs[index]` (the caller wires this to
 * `contentResolver.openInputStream(Uri.parse(song.dataUri))` - kept out of this class so it stays
 * plain-JVM-testable without an Android `ContentResolver`). Every route requires
 * `Authorization: Bearer <token>`; once a client's remote address has been accepted, requests from
 * any other address are rejected (Limitation §15 - one client at a time).
 *
 * [onProgress] reports cumulative bytes served for [manifest.songs]\[index\] (including
 * `rangeStart` on a `Range`-resume request) - the sender-side counterpart to
 * [TransferHttpClient.downloadSong]'s `onProgress`, which already reports the receiver's side.
 */
class TransferHttpServer(
    private val manifest: TransferManifest,
    private val openSong: (index: Int) -> InputStream?,
    private val token: String,
    private val port: Int = 8988,
    private val onProgress: (index: Int, bytesSent: Long, bytesTotal: Long) -> Unit = { _, _, _ -> },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var engine: EmbeddedServer<*, *>? = null
    private val activeClient = AtomicReference<String?>(null)

    fun start() {
        engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            routing {
                get("/manifest") {
                    if (!authorize(call)) return@get
                    call.respondText(
                        json.encodeToString(TransferManifest.serializer(), manifest),
                        ContentType.Application.Json,
                    )
                }
                get("/song/{index}") {
                    if (!authorize(call)) return@get
                    val index = call.parameters["index"]?.toIntOrNull()
                    if (index == null || index !in manifest.songs.indices) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    serveSong(call, index)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 200, timeoutMillis = 1_000)
        engine = null
    }

    private suspend fun authorize(call: ApplicationCall): Boolean {
        if (call.request.header(HttpHeaders.Authorization) != "Bearer $token") {
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }
        val remote = call.request.local.remoteHost
        val accepted = activeClient.updateAndGet { it ?: remote }
        if (accepted != remote) {
            call.respond(HttpStatusCode.TooManyRequests)
            return false
        }
        return true
    }

    private suspend fun serveSong(call: ApplicationCall, index: Int) {
        val song = manifest.songs[index]
        val rangeStart = parseRangeStart(call.request.header(HttpHeaders.Range))

        val stream = withContext(Dispatchers.IO) { openSong(index) }
        if (stream == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }
        stream.use { input ->
            if (rangeStart > 0) {
                withContext(Dispatchers.IO) { input.skip(rangeStart) }
            }
            call.response.header(HttpHeaders.AcceptRanges, "bytes")
            if (rangeStart > 0) {
                call.response.header(
                    HttpHeaders.ContentRange,
                    "bytes $rangeStart-${song.sizeBytes - 1}/${song.sizeBytes}",
                )
            }
            call.respondOutputStream(
                contentType = ContentType.parse(song.mime),
                status = if (rangeStart > 0) HttpStatusCode.PartialContent else HttpStatusCode.OK,
                contentLength = song.sizeBytes - rangeStart,
            ) {
                withContext(Dispatchers.IO) {
                    var sent = rangeStart
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        this@respondOutputStream.write(buffer, 0, read)
                        sent += read
                        onProgress(index, sent, song.sizeBytes)
                    }
                }
            }
        }
    }

    private fun parseRangeStart(header: String?): Long {
        if (header == null) return 0
        val match = Regex("""bytes=(\d+)-""").find(header) ?: return 0
        return match.groupValues[1].toLongOrNull() ?: 0
    }
}
