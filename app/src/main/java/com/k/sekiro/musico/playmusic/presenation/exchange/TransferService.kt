package com.k.sekiro.musico.playmusic.presenation.exchange

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.data.exchange.MediaImporter
import com.k.sekiro.musico.playmusic.data.exchange.TransferHttpClient
import com.k.sekiro.musico.playmusic.data.exchange.TransferHttpServer
import com.k.sekiro.musico.playmusic.data.exchange.TransferPairingCodec
import com.k.sekiro.musico.playmusic.data.exchange.TransferPairingPayload
import com.k.sekiro.musico.playmusic.data.exchange.WifiDirectTransport
import com.k.sekiro.musico.playmusic.data.exchange.WifiGroupResult
import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import com.k.sekiro.musico.playmusic.domain.exchange.TransferSong
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.SongsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Owns one audio-transfer session end to end. Plain [Service] (not `MediaSessionService` - this
 * isn't media-session-related), foreground so Android doesn't kill an in-flight transfer when the
 * app backgrounds.
 *
 * Sender ([ACTION_ADVERTISE]): builds the outgoing [TransferManifest] for a playlist (hashing each
 * file - the receiver needs exact `sha256`/`sizeBytes` to verify what it downloads), stands up the
 * Wi-Fi group and [TransferHttpServer] itself.
 *
 * Receiver ([ACTION_DOWNLOAD]): the Wi-Fi join and manifest preview already happened in the
 * ViewModel (`connectAndPreviewTransfer`) before this service is even started - joining is a
 * process-wide [android.net.ConnectivityManager.bindProcessToNetwork], so it doesn't need to be
 * redone here. This service only needs `host`/`port`/`token` to talk to the sender, plus the
 * already-decided [TransferManifest] and the indices the user confirmed importing.
 */
class TransferService : Service() {

    private val playlistRepository: PlaylistRepository by inject()
    private val playlistSongRepository: PlaylistSongRepository by inject()
    private val songsRepository: SongsRepository by inject()
    private val mediaImporter: MediaImporter by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private var wifiDirectTransport: WifiDirectTransport? = null
    private var httpServer: TransferHttpServer? = null
    private var httpClient: TransferHttpClient? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var notificationTitle = "Transfer"
    private var lastNotifiedProgressIndex = -1
    private var lastNotifiedProgressPercent = -1
    @Volatile private var senderDoneHandled = false

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADVERTISE -> {
                val playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L)
                if (playlistId < 0) {
                    stopSelf()
                } else {
                    notificationTitle = "Sending songs"
                    startForeground(NOTIFICATION_ID, buildNotification("Preparing to send songs…"))
                    job?.cancel()
                    job = scope.launch { runAdvertise(playlistId) }
                }
            }

            ACTION_DOWNLOAD -> {
                notificationTitle = "Receiving songs"
                startForeground(NOTIFICATION_ID, buildNotification("Connecting…"))
                job?.cancel()
                job = scope.launch { runDownload(intent) }
            }

            ACTION_CANCEL -> cancelAndStop()

            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    // ---- Sender ---------------------------------------------------------------------------

    private suspend fun runAdvertise(playlistId: Long) {
        state.value = TransferState.Connecting
        lastNotifiedProgressIndex = -1
        lastNotifiedProgressPercent = -1
        senderDoneHandled = false

        val built = buildOutgoingManifest(playlistId)
        if (built == null) {
            fail("Couldn't read this playlist's songs")
            return
        }
        val (manifest, songUris) = built

        acquireWifiLock()
        val transport = WifiDirectTransport(this).also { wifiDirectTransport = it }
        val group = transport.createGroup()
        if (group !is WifiGroupResult.Success) {
            val reason = (group as? WifiGroupResult.Failed)?.reason ?: "unknown"
            fail("Couldn't start the Wi-Fi link: $reason")
            return
        }

        val token = randomToken()
        // The sender doesn't know upfront how many songs the receiver will end up requesting
        // (v1 has no gap-negotiation step - design doc's out-of-scope list) - `sentSongsCount`
        // is just "the nth distinct song streamed this session", not a position within a known
        // total, so `TransferState.Downloading.count` is reported as -1 ("unknown") below.
        var lastSentIndex = -1
        var sentSongsCount = 0
        val server = TransferHttpServer(
            manifest = manifest,
            openSong = { index -> songUris.getOrNull(index)?.let { contentResolver.openInputStream(it) } },
            token = token,
            onProgress = { index, bytesSent, bytesTotal ->
                synchronized(this@TransferService) {
                    if (index != lastSentIndex) {
                        lastSentIndex = index
                        sentSongsCount++
                    }
                    val title = manifest.songs.getOrNull(index)?.title.orEmpty()
                    state.value = TransferState.Downloading(
                        index = sentSongsCount - 1,
                        count = -1,
                        bytesDone = bytesSent,
                        bytesTotal = bytesTotal,
                        title = title,
                    )
                    maybeNotifyProgress("Sending", title, index, bytesSent, bytesTotal)
                }
            },
            onDone = {
                if (!senderDoneHandled) {
                    senderDoneHandled = true
                    val sentCount = synchronized(this@TransferService) { sentSongsCount }
                    scope.launch { senderDone(sentCount) }
                }
            },
        ).also { httpServer = it }
        if (!startServerWithRetry(server)) {
            fail("Couldn't start the local server - try again in a moment")
            return
        }

        val manifestJson = json.encodeToString(TransferManifest.serializer(), manifest)
        val payload = TransferPairingPayload(
            ssid = group.credentials.ssid,
            pass = group.credentials.passphrase,
            host = group.credentials.host,
            port = TRANSFER_PORT,
            tok = token,
            man = sha256Hex(manifestJson.toByteArray(Charsets.UTF_8)),
            trackCount = manifest.songs.size,
            totalBytes = manifest.songs.sumOf { it.sizeBytes },
        )
        state.value = TransferState.Advertising(TransferPairingCodec.encode(payload))
        updateNotification("Waiting for the other device…")
    }

    /**
     * [TransferHttpServer.start] can throw synchronously ("Machine is not on the network" /
     * `SocketException` from the underlying `ServerSocketChannel` bind) when the process's network
     * state hasn't settled yet - e.g. this same device just finished a *receive* session and its
     * Wi-Fi Direct group/process network binding ([WifiDirectTransport.leaveGroup]/[WifiDirectTransport.stopGroup])
     * is still tearing down when the user immediately turns around and starts sending. Unlike
     * [WifiDirectTransport.createGroup] (which never throws), this call had no safety net at all,
     * so that transient race crashed the whole app instead of just failing this attempt. Retry
     * briefly before giving up - same shape as [WifiDirectTransport]'s own group-info retry.
     */
    private suspend fun startServerWithRetry(server: TransferHttpServer): Boolean {
        repeat(SERVER_START_RETRY_ATTEMPTS) { attempt ->
            try {
                server.start()
                return true
            } catch (ex: Exception) {
                android.util.Log.w("TransferService", "server.start() failed (attempt ${attempt + 1})", ex)
                if (attempt < SERVER_START_RETRY_ATTEMPTS - 1) delay(SERVER_START_RETRY_DELAY_MS)
            }
        }
        return false
    }

    /** Builds the manifest this device offers for [playlistId], hashing every file - the receiver
     * verifies each download against these exact values. Returns `null` if the playlist has no
     * songs to offer. [TransferSong] order matches the returned `Uri` list 1:1 by index. */
    private suspend fun buildOutgoingManifest(playlistId: Long): Pair<TransferManifest, List<Uri>>? {
        val playlistWithSongs = playlistRepository.getPlaylistWithSongs(playlistId) ?: return null
        if (playlistWithSongs.songs.isEmpty()) return null

        val transferSongs = mutableListOf<TransferSong>()
        val uris = mutableListOf<Uri>()
        for (song in playlistWithSongs.songs) {
            val uri = Uri.parse(song.dataUri)
            val hashed = hashSong(uri) ?: continue
            transferSongs.add(
                TransferSong(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.duration,
                    sizeBytes = hashed.sizeBytes,
                    mime = hashed.mime,
                    sha256 = hashed.sha256,
                    fileName = song.name,
                )
            )
            uris.add(uri)
        }
        if (transferSongs.isEmpty()) return null
        return TransferManifest(name = playlistWithSongs.playlist.name, songs = transferSongs) to uris
    }

    private data class HashedFile(val sizeBytes: Long, val sha256: String, val mime: String)

    private fun hashSong(uri: Uri): HashedFile? {
        val stream = contentResolver.openInputStream(uri) ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        stream.use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
                size += read
            }
        }
        val mime = contentResolver.getType(uri) ?: "audio/mpeg"
        return HashedFile(size, digest.digest().joinToString("") { "%02x".format(it) }, mime)
    }

    // ---- Receiver -------------------------------------------------------------------------

    private suspend fun runDownload(intent: Intent) {
        val playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L)
        val manifestJson = intent.getStringExtra(EXTRA_MANIFEST_JSON)
        val wantedIndices = intent.getIntArrayExtra(EXTRA_WANTED_INDICES)
        val matchedSongIds = intent.getLongArrayExtra(EXTRA_MATCHED_SONG_IDS) ?: LongArray(0)
        val host = intent.getStringExtra(EXTRA_HOST)
        val port = intent.getIntExtra(EXTRA_PORT, -1)
        val token = intent.getStringExtra(EXTRA_TOKEN)

        if (playlistId < 0 || manifestJson == null || wantedIndices == null || host == null || port < 0 || token == null) {
            fail("Missing transfer details")
            return
        }
        val manifest = try {
            json.decodeFromString(TransferManifest.serializer(), manifestJson)
        } catch (_: Exception) {
            fail("Couldn't read the playlist manifest")
            return
        }

        state.value = TransferState.Connecting
        lastNotifiedProgressIndex = -1
        lastNotifiedProgressPercent = -1
        val client = TransferHttpClient(baseUrl = "http://$host:$port", token = token).also { httpClient = it }

        val newPlaylistId = resolveDestinationPlaylist(playlistId, manifest.name)
        var added = 0
        var skipped = 0

        for (matchedId in matchedSongIds) {
            playlistSongRepository.addPlaylistSongRef(PlaylistSong(playlistId = newPlaylistId, songId = matchedId))
        }

        for ((position, index) in wantedIndices.withIndex()) {
            val song = manifest.songs.getOrNull(index)
            if (song == null) {
                skipped++
                continue
            }
            state.value = TransferState.Downloading(
                index = position,
                count = wantedIndices.size,
                bytesDone = 0,
                bytesTotal = song.sizeBytes,
                title = song.title,
            )
            maybeNotifyProgress("Receiving", song.title, position, 0, song.sizeBytes)
            val ok = downloadOneTrack(client, index, song, newPlaylistId, position, wantedIndices.size)
            if (ok) added++ else skipped++
        }

        runCatching { client.ackDone() }
        state.value = TransferState.Verifying
        teardown()
        state.value = TransferState.Done(added = added, skipped = skipped)
        updateNotification("Received $added song${if (added == 1) "" else "s"}", ongoing = false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private suspend fun downloadOneTrack(
        client: TransferHttpClient,
        index: Int,
        song: TransferSong,
        destinationPlaylistId: Long,
        position: Int,
        total: Int,
    ): Boolean {
        val buffer = ByteArrayOutputStream()
        try {
            client.downloadSong(index = index, sink = buffer) { bytesWritten ->
                state.value = TransferState.Downloading(
                    index = position,
                    count = total,
                    bytesDone = bytesWritten,
                    bytesTotal = song.sizeBytes,
                    title = song.title,
                )
                maybeNotifyProgress("Receiving", song.title, position, bytesWritten, song.sizeBytes)
            }
        } catch (_: Exception) {
            return false
        }

        val imported = mediaImporter.importAudio(
            displayName = song.fileName,
            mime = song.mime,
            expectedSize = song.sizeBytes,
            expectedSha256 = song.sha256,
            body = buffer.toByteArray().inputStream(),
        )
        val newId = imported.getOrNull() ?: return false

        // Sync the newly-inserted MediaStore row into Room before referencing it in a
        // PlaylistSong - addSong() upserts, avoiding a dangling FK until the app's regular
        // rescan/ContentObserver pipeline would otherwise pick this file up.
        val storageSongs = songsRepository.getAllStorageSongs()
        val song0: Song = storageSongs.firstOrNull { it.id == newId } ?: return false
        songsRepository.addSong(song0)
        playlistSongRepository.addPlaylistSongRef(PlaylistSong(playlistId = destinationPlaylistId, songId = newId))
        return true
    }

    private suspend fun resolveDestinationPlaylist(requestedPlaylistId: Long, fallbackName: String): Long {
        playlistRepository.getPlaylist(requestedPlaylistId)?.let { return requestedPlaylistId }
        return playlistRepository.addPlaylist(
            com.k.sekiro.musico.playmusic.domain.model.Playlist(name = fallbackName.ifBlank { "Imported playlist" })
        )
    }

    // ---- Shared -----------------------------------------------------------------------------

    private suspend fun fail(reason: String) {
        state.value = TransferState.Failed(reason)
        teardown()
        updateNotification(reason, ongoing = false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    /** Reached once the receiver's `/done` ack tells this sender its download loop finished - see
     * [TransferHttpServer]'s `onDone`. Runs on [scope], not the ktor engine thread that invoked it,
     * since [teardown] stops that same server and would otherwise block on itself. */
    private suspend fun senderDone(sentCount: Int) {
        state.value = TransferState.Done(added = sentCount, skipped = 0)
        teardown()
        updateNotification("Sent $sentCount song${if (sentCount == 1) "" else "s"}", ongoing = false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun cancelAndStop() {
        job?.cancel()
        scope.launch {
            teardown()
            state.value = TransferState.Idle
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun teardown() {
        httpServer?.stop()
        httpServer = null
        httpClient?.close()
        httpClient = null
        wifiDirectTransport?.let {
            it.leaveGroup()
            it.stopGroup()
        }
        wifiDirectTransport = null
        releaseWifiLock()
    }

    @Suppress("DEPRECATION")
    private fun acquireWifiLock() {
        val manager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "musico:transfer")
            .apply { setReferenceCounted(false); acquire() }
    }

    private fun releaseWifiLock() {
        wifiLock?.let { if (it.isHeld) it.release() }
        wifiLock = null
    }

    override fun onDestroy() {
        // scope.cancel() below would cancel this teardown before it ever runs (launch only
        // schedules, it doesn't run synchronously) - needs its own short-lived scope, same
        // pattern as PlayerSessionService.onDestroy's post-cancel widget push.
        val server = httpServer
        val client = httpClient
        val transport = wifiDirectTransport
        val lock = wifiLock
        httpServer = null
        httpClient = null
        wifiDirectTransport = null
        wifiLock = null
        CoroutineScope(Dispatchers.IO).launch {
            server?.stop()
            client?.close()
            transport?.let {
                it.leaveGroup()
                it.stopGroup()
            }
            lock?.let { if (it.isHeld) it.release() }
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Transfers", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    /** [ongoing] false marks a terminal (done/failed) notification: no Cancel action, and
     * auto-cancel so the user can tap or swipe it away instead of it looking stuck mid-transfer. */
    private fun buildNotification(text: String, ongoing: Boolean = true): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_audio)
        if (ongoing) {
            val cancelIntent = Intent(this, TransferService::class.java).apply { action = ACTION_CANCEL }
            val cancelPendingIntent = android.app.PendingIntent.getService(
                this, 0, cancelIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setOngoing(true).addAction(0, "Cancel", cancelPendingIntent)
        } else {
            builder.setAutoCancel(true)
        }
        return builder.build()
    }

    private fun updateNotification(text: String, ongoing: Boolean = true) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text, ongoing))
    }

    /**
     * Updates the ongoing notification with per-song progress, throttled to once per 5 percentage
     * points (plus always on a song change or that song's completion) - a raw per-8KB-chunk
     * [updateNotification] call would otherwise hit `NotificationManager.notify()` hundreds of
     * times per song.
     */
    private fun maybeNotifyProgress(verb: String, title: String, index: Int, bytesDone: Long, bytesTotal: Long) {
        val percent = if (bytesTotal > 0) ((bytesDone * 100) / bytesTotal).toInt() else 0
        if (index != lastNotifiedProgressIndex || percent - lastNotifiedProgressPercent >= 5 || bytesDone >= bytesTotal) {
            lastNotifiedProgressIndex = index
            lastNotifiedProgressPercent = percent
            updateNotification("$verb $title ($percent%)")
        }
    }

    companion object {
        private const val CHANNEL_ID = "transfers"
        private const val NOTIFICATION_ID = 8_988
        const val TRANSFER_PORT = 8988
        private const val SERVER_START_RETRY_ATTEMPTS = 3
        private const val SERVER_START_RETRY_DELAY_MS = 500L

        private const val ACTION_ADVERTISE = "com.k.sekiro.musico.transfer.ADVERTISE"
        private const val ACTION_DOWNLOAD = "com.k.sekiro.musico.transfer.DOWNLOAD"
        const val ACTION_CANCEL = "com.k.sekiro.musico.transfer.CANCEL"

        private const val EXTRA_PLAYLIST_ID = "playlist_id"
        private const val EXTRA_MANIFEST_JSON = "manifest_json"
        private const val EXTRA_WANTED_INDICES = "wanted_indices"
        private const val EXTRA_MATCHED_SONG_IDS = "matched_song_ids"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_TOKEN = "token"

        /** Mirrors this service's progress for the ViewModel to collect into `UiState.transfer`. */
        val state = MutableStateFlow<TransferState>(TransferState.Idle)

        fun advertiseIntent(context: Context, playlistId: Long): Intent =
            Intent(context, TransferService::class.java).apply {
                action = ACTION_ADVERTISE
                putExtra(EXTRA_PLAYLIST_ID, playlistId)
            }

        fun downloadIntent(
            context: Context,
            playlistId: Long,
            manifestJson: String,
            wantedIndices: IntArray,
            matchedSongIds: LongArray,
            host: String,
            port: Int,
            token: String,
        ): Intent = Intent(context, TransferService::class.java).apply {
            action = ACTION_DOWNLOAD
            putExtra(EXTRA_PLAYLIST_ID, playlistId)
            putExtra(EXTRA_MANIFEST_JSON, manifestJson)
            putExtra(EXTRA_WANTED_INDICES, wantedIndices)
            putExtra(EXTRA_MATCHED_SONG_IDS, matchedSongIds)
            putExtra(EXTRA_HOST, host)
            putExtra(EXTRA_PORT, port)
            putExtra(EXTRA_TOKEN, token)
        }

        fun cancelIntent(context: Context): Intent =
            Intent(context, TransferService::class.java).apply { action = ACTION_CANCEL }

        private fun randomToken(): String {
            val bytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(bytes)
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
        }

        private fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
