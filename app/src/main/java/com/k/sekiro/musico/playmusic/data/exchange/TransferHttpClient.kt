package com.k.sekiro.musico.playmusic.data.exchange

import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.Json
import java.io.OutputStream

/**
 * Fetches the other device's [TransferManifest] and streams individual track bytes from it. Owned
 * and torn down by `TransferService` for the duration of one transfer session - not a long-lived
 * singleton (matches [TransferHttpServer]).
 */
class TransferHttpClient(
    private val baseUrl: String,
    private val token: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO)

    suspend fun fetchManifest(): TransferManifest {
        val response: HttpResponse = client.get("$baseUrl/manifest") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        check(response.status.isSuccess()) { "manifest fetch failed: ${response.status}" }
        return json.decodeFromString(TransferManifest.serializer(), response.bodyAsText())
    }

    /**
     * Streams song [index]'s bytes into [sink]. [resumeFromBytes] adds a `Range: bytes=<n>-`
     * header to resume a partial download; [onProgress] reports cumulative bytes written
     * (including [resumeFromBytes]).
     */
    suspend fun downloadSong(
        index: Int,
        sink: OutputStream,
        resumeFromBytes: Long = 0,
        onProgress: (bytesWritten: Long) -> Unit = {},
    ) {
        client.prepareGet("$baseUrl/song/$index") {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (resumeFromBytes > 0) {
                header(HttpHeaders.Range, "bytes=$resumeFromBytes-")
            }
        }.execute { response ->
            check(response.status.isSuccess()) { "song $index download failed: ${response.status}" }
            val channel = response.bodyAsChannel()
            var total = resumeFromBytes
            val buffer = ByteArray(8 * 1024)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read <= 0) continue
                sink.write(buffer, 0, read)
                total += read
                onProgress(total)
            }
        }
    }

    /** Tells the sender this device has finished its whole download loop - see [TransferHttpServer]'s
     * `onDone`. Best-effort: caller already has its files by the time it calls this. */
    suspend fun ackDone() {
        val response: HttpResponse = client.get("$baseUrl/done") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        check(response.status.isSuccess()) { "done ack failed: ${response.status}" }
    }

    fun close() {
        client.close()
    }
}
