package com.k.sekiro.musico.playmusic.presenation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.model.IsSelectedFromPlaylist_KEY
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.RecentSongsIds_KEY
import com.k.sekiro.musico.playmusic.domain.exchange.MatchResult
import com.k.sekiro.musico.playmusic.domain.exchange.SongMatcher
import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import com.k.sekiro.musico.playmusic.domain.exchange.TransferMatchResult
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.SongsRepository
import com.k.sekiro.musico.playmusic.data.exchange.TransferHttpClient
import com.k.sekiro.musico.playmusic.data.exchange.TransferPairingCodec
import com.k.sekiro.musico.playmusic.data.exchange.TransferPairingPayload
import com.k.sekiro.musico.playmusic.data.exchange.TransferWifiCredentials
import com.k.sekiro.musico.playmusic.data.exchange.WifiDirectTransport
import com.k.sekiro.musico.playmusic.data.util.reconcileLibrary
import com.k.sekiro.musico.playmusic.presenation.exchange.PlaylistImportPreview
import com.k.sekiro.musico.playmusic.presenation.exchange.PlaylistQrCodec
import com.k.sekiro.musico.playmusic.presenation.exchange.TransferOffer
import com.k.sekiro.musico.playmusic.presenation.exchange.TransferService
import com.k.sekiro.musico.playmusic.presenation.exchange.TransferState
import com.k.sekiro.musico.playmusic.presenation.model.DeletionType
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.playmusic.presenation.model.toPlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.player.MediaControllerManager
import com.k.sekiro.musico.playmusic.presenation.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.player.onChangPlayType
import com.k.sekiro.musico.playmusic.presenation.player.playOrPause
import com.k.sekiro.musico.playmusic.presenation.player.service.CUSTOM_COMMAND_CANCEL_SLEEP_TIMER_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.service.CUSTOM_COMMAND_START_SLEEP_TIMER_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.service.CUSTOM_COMMAND_START_SLEEP_TIMER_END_OF_TRACK_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.presenation.player.service.SLEEP_TIMER_DURATION_ARG
import com.k.sekiro.musico.playmusic.presenation.player.startProgressUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ViewModel(
    private val songsRepository: SongsRepository,
    private val playlistRepository: PlaylistRepository,
    private val playlistSongRepository: PlaylistSongRepository,
    private val dataSaver: SimpleDataSaver,
    private val controllerManager: MediaControllerManager,
    private val savedStateHandle: SavedStateHandle,
    private val context: Context,
) : ViewModel() {


    /* @OptIn(SavedStateHandleSaveableApi::class)
    var duration by savedStateHandle.saveable{ mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable{ mutableFloatStateOf(0f) }
    var durationString by savedStateHandle.saveable{ mutableStateOf("00:00") }*/

    private val stateKey = "uiState"


    /**
     * Coalesces MediaStore change notifications into library rescans.
     *
     * Every change fires the observer, and a rescan is expensive (a full MediaStore query plus a
     * per-song album-art URI check, then Room writes) - an audio transfer importing 14 files used
     * to kick off 14 overlapping rescans. Two things keep that bounded: [debounce] collapses rapid
     * bursts, and the single sequential collector below plus a 1-slot [BufferOverflow.DROP_OLDEST]
     * buffer means at most one rescan runs while at most one more sits queued, no matter how many
     * changes arrive in the meantime.
     */
    private val librarySyncRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private companion object {
        /** Long enough to swallow a burst of writes, short enough that a song added by another app
         * still shows up while the user is looking at the screen. */
        const val LIBRARY_RESCAN_DEBOUNCE_MS = 800L
    }

    private val _state = MutableStateFlow<UiState>(UiState())
    val state = _state
        .onStart {
            Log.e("ks", "heyyyy I'm in onStart flow")
            getAllSongsFromLocal()
            songsRepository.startObservingSongChanges { librarySyncRequests.tryEmit(Unit) }
            getPlayLists()
            getRecentPlaylistSongs()
            getPlaylistsWithSongs()
            getSavedLastPlayedPlaylistSongs()

        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000L
            ),
            UiState()
        )

    private val _events = Channel<UiEvents>()
    val events = _events.receiveAsFlow()

    private val isSelectedSongFromPlaylist = MutableStateFlow(false)
    private val currentPlayedPlaylistSong = MutableStateFlow(emptyList<SongUi>())

    private val currentPlayedPlaylistId = MutableStateFlow(-1L)

    init {
        controllerManager.setViewModel(this)
        controllerManager.setCoroutineScope(viewModelScope)
        viewModelScope.launch {
            isSelectedSongFromPlaylist.update {
                dataSaver.suspendGet(IsSelectedFromPlaylist_KEY, false)
            }
        }
        viewModelScope.launch {
            TransferService.state.collectLatest { transferState ->
                _state.update { it.copy(transfer = transferState) }
                if (transferState is TransferState.Done || transferState is TransferState.Failed) {
                    activeReceiveTransport?.leaveGroup()
                    activeReceiveTransport = null
                }
            }
        }
        // Plain collect, not collectLatest: rescans run to completion one at a time. See
        // librarySyncRequests' KDoc for how bursts are coalesced.
        viewModelScope.launch {
            librarySyncRequests
                .debounce(LIBRARY_RESCAN_DEBOUNCE_MS)
                .collect { syncLibraryWithStorage() }
        }
        viewModelScope.launch {
            // The sleep timer's countdown and pause() call run on the service, not here, so
            // they keep working while the app is backgrounded and this ViewModel is gone -
            // this just mirrors that state back for the UI to render.
            PlayerSessionService.sleepTimerState.collect { timer ->
                _state.update { it.copy(sleepTimer = timer) }
            }
        }

    }

    /*    private val _state = savedStateHandle.getStateFlow(stateKey, UiState())
    val state = _state
        .onStart {
            getAllSongsFromLocal()
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000
            )
        )*/


    fun updatePlayedSong(index: Int) {
        Log.e("ks", "is")
        val actualSongs =
            if (isSelectedSongFromPlaylist.value) currentPlayedPlaylistSong.value else _state.value.songs
        val size = actualSongs.size
        val validateIndex = if (index < 0) 0 else if (index >= size) size - 1 else index
        _state.update {
            it.copy(
                playedSong = if (
                    actualSongs.isNotEmpty()
                ) {
                    actualSongs[validateIndex]
                } else {
                    return
                }
            )
        }
    }

    fun getPlayedSong() = _state.value.playedSong

    fun getSongs() = _state.value.songs

    fun updateProgress(progress: Float) {
        _state.update {
            it.copy(
                sliderProgress = progress
            )
        }
    }


    fun updateIsPlaying(isPlaying: Boolean) {
        _state.update {
            it.copy(
                isPlaying = isPlaying
            )
        }
    }

    fun updatePlayType(type: PlayType) {
        _state.update {
            it.copy(
                playType = type
            )
        }
    }

    fun updateDuration(duration: Long) {
        _state.update {
            it.copy(

            )
        }
    }

    fun updateSongs(songs: List<SongUi>) {
        _state.update {
            it.copy(
                songs = songs
            )
        }
    }

    fun onSelectSong(song: SongUi) {
        Log.e("ks", "onSelectedSong")
        _state.update {
            it.copy(
                selectedSongs = it.selectedSongs.toMutableList().apply {
                    if (contains(song)) {
                        remove(song)
                    } else {
                        add(song)
                    }
                }
            )
        }

        _state.update { it.copy(selectModeEnabled = it.selectedSongs.isNotEmpty()) }
    }


    fun onCancelAllSelectedSongs() {
        _state.update {
            it.copy(
                selectedSongs = emptyList(),
                selectModeEnabled = false
            )
        }
    }

    private fun getAllSongsFromLocal() {
        viewModelScope.launch { syncLibraryWithStorage() }
    }

    /** User-triggered re-scan from the empty-library screen. Ignored while a scan is already
     * running or queued, so repeated Retry taps don't stack overlapping MediaStore scans. */
    fun rescanLibrary() {
        if (_state.value.isLibraryLoading) return
        _state.update { it.copy(isLibraryLoading = true) }
        viewModelScope.launch { syncLibraryWithStorage() }
    }

    /** In-flight [syncLibraryWithStorage] calls. `isLibraryLoading` is cleared only when this
     * reaches zero, so a short sync finishing cannot flip the UI to "No audio found" while a
     * longer one is still populating Room. Every caller is a `viewModelScope.launch` (main
     * dispatcher), so a plain Int needs no synchronisation. */
    private var runningSyncCount = 0

    /** Reconciles Room against MediaStore. Suspends until done so the caller in [init] can keep
     * rescans strictly sequential - see [librarySyncRequests]. */
    private suspend fun syncLibraryWithStorage() {
        runningSyncCount++
        try {
            withContext(Dispatchers.IO) {
                val roomSongs = songsRepository.getSongsFromRoom()
                if (roomSongs.isNotEmpty()) {
                    _state.update { it.copy(songs = roomSongs.map { it.toSongUi() }) }
                }

                val reconcile = reconcileLibrary(roomSongs, songsRepository.getAllStorageSongs())
                if (reconcile.toDelete.isNotEmpty()) songsRepository.deleteSongs(reconcile.toDelete)
                if (reconcile.toAdd.isNotEmpty()) songsRepository.addSongs(reconcile.toAdd)

                // Re-read from Room: playlists and their song relationships key off Room ids, so
                // downstream state must reflect Room, not the raw scan.
                _state.update {
                    it.copy(songs = songsRepository.getSongsFromRoom().map { it.toSongUi() })
                }
            }
        } finally {
            if (--runningSyncCount == 0) {
                _state.update { it.copy(isLibraryLoading = false) }
            }
        }
    }


    private fun getPlayLists() {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.getAllPlaylists().collect { list ->
                Log.e("ks", "listSiz : ${list.size}")
                _state.update {
                    it.copy(
                        playlists = list
                    )
                }
            }

        }
    }

    fun onAddToNewPlaylist(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistId = playlistRepository.addPlaylist(Playlist(name = playlistName))
            Log.e("ks", "onAddPlaylist id: $playlistId")
            for (song in _state.value.selectedSongs) {
                playlistSongRepository.addPlaylistSongRef(
                    playlistSongRef = PlaylistSong(
                        playlistId = playlistId,
                        songId = song.id
                    )
                )
            }

            onCancelAllSelectedSongs()
            _events.send(UiEvents.Message("added successfully to $playlistName playlist"))
            // getPlayLists()
        }
    }

    fun addNewPlaylist(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.addPlaylist(Playlist(name = playlistName))
            _events.send(UiEvents.Message("$playlistName has been added successfully"))
            /*            val playlistsWithSongs = playlistRepository.getPlaylistWithSongs().map {
                            it.toPlaylistWithSongsUi()
                        }
                        _state.update {
                            it.copy(
                                playlistsWithSongs = playlistsWithSongs,
                                playlists = playlistsWithSongs.map { it.playlist }
                            )
                        }*/
        }
    }


    fun onAddToExistPlaylist(playlist: Playlist) {

        viewModelScope.launch(Dispatchers.IO) {
            for (song in _state.value.selectedSongs) {
                playlistSongRepository.addPlaylistSongRef(
                    playlistSongRef = PlaylistSong(
                        playlistId = playlist.id,
                        songId = song.id
                    )
                )
            }

            onCancelAllSelectedSongs()
            _events.send(UiEvents.Message("added successfully to ${playlist.name} playlist"))
            // getPlayLists()
        }

    }

    private fun getPlaylistsWithSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.getPlaylistWithSongs().collect { playlistsWithSongs ->
                _state.update {
                    it.copy(
                        playlistsWithSongs = playlistsWithSongs.map { it.toPlaylistWithSongsUi() }
                    )
                }
            }

        }
    }


    fun addToRecent(songId: Long) {
        /*       val playlist =  _state.value.playlistsWithSongs.find { it.playlist.name == "Recent" }!!
                val song = playlist.songs.find { it.id == songId }*/
        val playlistSong = PlaylistSong(songId = songId, playlistId = 2)
        viewModelScope.launch(Dispatchers.IO) {
            playlistSongRepository.addPlaylistSongRef(playlistSong)
            /*            if (song == null) {
                            playlistSongRepository.addPlaylistSongRef(playlistSong)
                            Log.e("ks","song in in null check : $song")
                        }else{
                            Log.e("ks","song in in else null check : $song")

                            playlistSongRepository.deletePlaylistSongRef(playlistSong)
                            playlistSongRepository.addPlaylistSongRef(playlistSong)
                //ZonedDateTime.now()
                //LocalDateTime.now()
                // kotlinX datetime
            }*/
        }

    }

    fun addSingleSongToPlaylist(songId: Long,playlist: Playlist?,playlistName:String?){
        viewModelScope.launch(Dispatchers.IO) {
            if (playlistName == null && playlist?.id != null){
                playlistSongRepository.addPlaylistSongRef(PlaylistSong(playlist.id,songId))
                _events.send(UiEvents.Message("added successfully to ${playlist.name} playlist"))
            }else if (playlist?.id == null && playlistName != null){
                val playlistId1 = playlistRepository.addPlaylist(Playlist(playlistName))
                playlistSongRepository.addPlaylistSongRef(PlaylistSong(playlistId1,songId))
                _events.send(UiEvents.Message("added successfully to $playlistName playlist"))

            }

        }

    }

    // --- Offline playlist exchange (QR / file import) ---------------------------------------

    /** Resolved match awaiting the user's confirm in the import-preview dialog. */
    private var pendingImport: MatchResult? = null

    /**
     * Decodes a scanned QR string (or the text of an imported `.json` file), matches it against
     * the local library and shows the import-preview dialog. Called from the scan screen.
     */
    fun preparePlaylistImport(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val export = PlaylistQrCodec.decode(raw)
            if (export == null) {
                _events.send(UiEvents.Message("Couldn't read this playlist code"))
                return@launch
            }
            clearFinishedTransferState()
            val library = songsRepository.getSongsFromRoom()
            val result = SongMatcher.matchAll(export, library)
            pendingImport = result
            _state.update {
                it.copy(
                    importPreview = PlaylistImportPreview(
                        name = result.playlistName,
                        matchedCount = result.matched.size,
                        totalCount = result.matched.size + result.unmatched.size,
                        unmatchedTitles = result.unmatched.map { fp ->
                            fp.title.ifBlank { "Unknown" }
                        }
                    )
                )
            }
        }
    }

    /** Creates the playlist from the matched songs. Called when the user confirms the preview. */
    fun confirmPlaylistImport() {
        val result = pendingImport ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val name = uniquePlaylistName(result.playlistName.ifBlank { "Imported playlist" })
            val playlistId = playlistRepository.addPlaylist(Playlist(name = name))
            for (song in result.matched) {
                playlistSongRepository.addPlaylistSongRef(
                    PlaylistSong(playlistId = playlistId, songId = song.id)
                )
            }
            pendingImport = null
            _state.update { it.copy(importPreview = null) }
            _events.send(
                UiEvents.Message(
                    "Imported \"$name\" — ${result.matched.size} of " +
                        "${result.matched.size + result.unmatched.size} songs"
                )
            )
        }
    }

    fun dismissPlaylistImport() {
        pendingImport = null
        _state.update { it.copy(importPreview = null) }
    }

    // --- Audio transfer (v2 of the exchange above - sends the actual bytes) -----------------

    private val transferJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Resolved manifest + match + still-joined Wi-Fi link, awaiting the user's confirm. */
    private var pendingTransfer: PendingTransfer? = null

    /**
     * The joined link handed off to `TransferService` by [confirmTransferImport], kept only so its
     * `NetworkCallback`/ephemeral network request can be released once the download reaches a
     * terminal state - the service can't do it, since it owns no transport on the receive path.
     */
    private var activeReceiveTransport: WifiDirectTransport? = null

    private data class PendingTransfer(
        val manifest: TransferManifest,
        val matchResult: TransferMatchResult,
        val payload: TransferPairingPayload,
        val wifiDirectTransport: WifiDirectTransport,
    )

    /**
     * Decodes a scanned `MUSICO-XFER-1:` QR, joins the sender's private Wi-Fi link, fetches and
     * verifies its manifest, matches it against the local library and shows the (extended)
     * import-preview dialog. Called from the scan screen instead of [preparePlaylistImport] when
     * [com.k.sekiro.musico.playmusic.data.exchange.TransferPairingCodec.isTransferPayload] is true.
     */
    fun connectAndPreviewTransfer(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = TransferPairingCodec.decode(raw)
            if (payload == null) {
                _events.send(UiEvents.Message("Couldn't read this transfer code"))
                return@launch
            }
            clearFinishedTransferState()

            // Same platform requirement the sender hits in createGroup: on API ≤32 an app can hold
            // ACCESS_FINE_LOCATION and still see no Wi-Fi scan results while the OS location toggle
            // is off, so the join can never match the sender's SSID.
            if (WifiDirectTransport.locationServicesRequiredButOff(context)) {
                _events.send(
                    UiEvents.Message("Turn on Location - Android 12 and older need it to connect over Wi-Fi Direct")
                )
                return@launch
            }

            val transport = WifiDirectTransport(context)
            val joined = transport.joinGroup(
                TransferWifiCredentials(ssid = payload.ssid, passphrase = payload.pass, host = payload.host)
            )
            if (!joined) {
                // API 29-30 shows a system "Connect to this network?" dialog before the join can
                // succeed - missing or declining it is the most common cause of a failed join on
                // exactly this range (API 31+ handles the same WifiNetworkSpecifier flow without
                // that extra step, per the design doc's Limitation §5).
                val hint = if (android.os.Build.VERSION.SDK_INT in
                    android.os.Build.VERSION_CODES.Q..android.os.Build.VERSION_CODES.R
                ) {
                    " - check for a \"Connect to this network?\" prompt from Android, accept it quickly, then try again"
                } else ""
                _events.send(UiEvents.Message("Couldn't connect to the other device's Wi-Fi$hint"))
                return@launch
            }

            val client = TransferHttpClient(baseUrl = "http://${payload.host}:${payload.port}", token = payload.tok)
            val manifest = try {
                client.fetchManifest()
            } catch (_: Exception) {
                transport.leaveGroup()
                _events.send(UiEvents.Message("Couldn't fetch the playlist from the other device"))
                return@launch
            } finally {
                client.close()
            }

            val manifestJson = transferJson.encodeToString(TransferManifest.serializer(), manifest)
            val actualHash = sha256Hex(manifestJson.toByteArray(Charsets.UTF_8))
            if (actualHash != payload.man) {
                transport.leaveGroup()
                _events.send(UiEvents.Message("The playlist data didn't match - try scanning again"))
                return@launch
            }

            val library = songsRepository.getSongsFromRoom()
            val result = SongMatcher.matchAll(manifest, library)
            pendingTransfer = PendingTransfer(manifest, result, payload, transport)
            _state.update {
                it.copy(
                    importPreview = PlaylistImportPreview(
                        name = result.playlistName,
                        matchedCount = result.matched.size,
                        totalCount = result.matched.size + result.unmatched.size,
                        unmatchedTitles = result.unmatched.map { s -> s.title.ifBlank { "Unknown" } },
                        transferOffer = TransferOffer(
                            newTrackCount = result.unmatched.size,
                            totalBytes = result.unmatched.sumOf { s -> s.sizeBytes },
                        ),
                    )
                )
            }
        }
    }

    /** Adds the matched songs immediately and starts [TransferService] downloading the rest. */
    fun confirmTransferImport() {
        val pending = pendingTransfer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val name = uniquePlaylistName(pending.matchResult.playlistName.ifBlank { "Imported playlist" })
            val playlistId = playlistRepository.addPlaylist(Playlist(name = name))

            val unmatchedSet = pending.matchResult.unmatched.toHashSet()
            val wantedIndices = pending.manifest.songs
                .withIndex()
                .filter { (_, song) -> song in unmatchedSet }
                .map { it.index }
                .toIntArray()
            val matchedIds = pending.matchResult.matched.map { it.id }.toLongArray()

            ContextCompat.startForegroundService(
                context,
                TransferService.downloadIntent(
                    context = context,
                    playlistId = playlistId,
                    manifestJson = transferJson.encodeToString(TransferManifest.serializer(), pending.manifest),
                    wantedIndices = wantedIndices,
                    matchedSongIds = matchedIds,
                    host = pending.payload.host,
                    port = pending.payload.port,
                    token = pending.payload.tok,
                )
            )

            activeReceiveTransport = pending.wifiDirectTransport
            pendingTransfer = null
            _state.update { it.copy(importPreview = null) }
        }
    }

    fun dismissTransferImport() {
        pendingTransfer?.wifiDirectTransport?.leaveGroup()
        pendingTransfer = null
        _state.update { it.copy(importPreview = null, transfer = null) }
    }

    fun cancelTransfer() {
        context.startService(TransferService.cancelIntent(context))
        pendingTransfer?.wifiDirectTransport?.leaveGroup()
        pendingTransfer = null
    }

    /** Sender: starts advertising [playlistId] for another device to connect and pull songs from. */
    fun startTransferAdvertise(playlistId: Long) {
        ContextCompat.startForegroundService(context, TransferService.advertiseIntent(context, playlistId))
    }

    /** Clears a terminal `Done`/`Failed` transfer state so the sender's export screen goes back to
     * showing the plain playlist QR - `TransferService.state` isn't touched, so this only affects
     * the local UI until the next real transfer starts. */
    fun dismissTransferState() {
        _state.update { it.copy(transfer = null) }
    }

    /**
     * Clears a *leftover* terminal (`Done`/`Failed`) transfer state from a previous session, in
     * both the local mirror and the shared [TransferService.state] flow itself - unlike
     * [dismissTransferState], which only overrides the local copy. Without this, a finished
     * session's outcome sits in that process-wide flow forever (nothing else resets it until the
     * *next* transfer starts) and bleeds into whichever unrelated screen reads `state.value.transfer`
     * next - e.g. opening the QR/share screen for a playlist you just received shows a stray "Sent"
     * from that receive, or scanning a brand new playlist shows the previous one's "Received N songs".
     *
     * Call this right as a *new* session-adjacent screen/action starts (entering the export screen,
     * decoding a fresh QR) - it no-ops while a transfer is genuinely in progress, so it never
     * interrupts one that's actually still running.
     */
    fun clearFinishedTransferState() {
        val current = TransferService.state.value
        if (current is TransferState.Done || current is TransferState.Failed) {
            TransferService.state.value = TransferState.Idle
        }
        _state.update { it.copy(transfer = null) }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun uniquePlaylistName(base: String): String {
        val existing = _state.value.playlists.map { it.name }.toHashSet()
        if (base !in existing) return base
        var n = 2
        while ("$base ($n)" in existing) n++
        return "$base ($n)"
    }

    private fun getRecentPlaylistSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            playlistSongRepository.getRecentPlaylistSongs().collectLatest { songs ->
                _state.update {
                    it.copy(
                        recentPlaylistSongs = songs.map { it.toSongUi() }
                    )
                }
            }
        }
    }

    fun updateIsSelectedSongFromPlaylist(
        value: Boolean,
        songs: List<SongUi> = emptyList(),
        playlistId: Long = -1
    ) {
        isSelectedSongFromPlaylist.update { value }
        currentPlayedPlaylistSong.update { songs }
        currentPlayedPlaylistId.update { playlistId }
        viewModelScope.launch { dataSaver.suspendSave(IsSelectedFromPlaylist_KEY, value) }
        PlayerSessionService.syncRecentPlaylist(songs, value)
    }

    fun isSelectedSongFromPlaylist(): Boolean = isSelectedSongFromPlaylist.value

    fun currentPlaylistId() = currentPlayedPlaylistId.value

    fun currentPlaylistSongs() = currentPlayedPlaylistSong.value

    fun updateCurrentPlaylist(song: List<SongUi>) {
        currentPlayedPlaylistSong.update { song }
    }

    private fun getFavoriteSongs(): List<SongUi> {
        return _state.value.playlistsWithSongs.find { it.playlist.id == 1L }!!.songs
    }

    fun updateFavorite(songUi: SongUi) {
        if (!isFavorite(songUi)) {
            viewModelScope.launch {
                playlistSongRepository.addPlaylistSongRef(
                    PlaylistSong(1, songUi.id)
                )
                controllerManager.getController()?.sendCustomCommand(
                    NotificationPlayerCustomCommand.UNFAVORITE.commandButton.sessionCommand!!,
                    Bundle.EMPTY
                )
            }
        } else {
            viewModelScope.launch {
                playlistSongRepository.deletePlaylistSongRef(
                    PlaylistSong(1, songUi.id)
                )

                controllerManager.getController()?.sendCustomCommand(
                    NotificationPlayerCustomCommand.FAVORITE.commandButton.sessionCommand!!,
                    Bundle.EMPTY
                )
            }
        }
    }

    fun isFavorite(songUi: SongUi): Boolean {
        return getFavoriteSongs().contains(songUi)
    }

    private suspend fun <T> SavedStateHandle.updateSuspended(
        key: String,
        function: suspend (T?) -> T?
    ) {

        this[key] = function(this.get<T>(key))
    }

    private fun <T> SavedStateHandle.update(key: String, function: (T?) -> T?) {

        this[key] = function(this.get<T>(key))
    }


    internal fun calculateProgressValue(currentProgress: Long) {


        _state.update {
            val progress = if (currentProgress > 0 && it.playedSong != null) {
                ((currentProgress.toFloat() / it.playedSong.displayableDuration.durationMillis.toFloat()) * 100f)

            } else {
                0f
            }
            it.copy(
                sliderProgress = progress,
                passedTimeDuration = fromMillis(currentProgress),
                currentPosition = currentProgress
            )
        }
    }

    private suspend fun getSavedLastPlayedPlaylistSongs() = coroutineScope {
        if (isSelectedSongFromPlaylist.value) {
            val string = async { dataSaver.suspendGet(RecentSongsIds_KEY, "") }

            val songIds: List<Long> = try {
                Json.decodeFromString(string.await())
            } catch (ex: SerializationException) {
                emptyList()
            } catch (ex: Exception) {
                emptyList()
            }

            // preserves the saved order; songs deleted since saving are dropped via mapNotNull
            val recentSongs = songIds.mapNotNull { songsRepository.getSong(it) }.map { it.toSongUi() }
            Log.e("ks", "my current :$recentSongs")
            currentPlayedPlaylistSong.update { recentSongs }
        }
    }

    suspend fun saveRecentPlaylistSongs() {
        val stringList = Json.encodeToString(currentPlayedPlaylistSong.value.map { it.id })
        dataSaver.suspendSave(RecentSongsIds_KEY, stringList)
    }


    public override fun onCleared() {

        songsRepository.stopObservingSongChanges()

        super.onCleared()

    }

    fun initController() {
        controllerManager.initialize()
    }

    suspend fun controllerAndLastPlayedSongSetup(songs: List<SongUi>) {
        Log.e("ks", "enter viewModel controllerAndLastPlayedSongSetup block")
        controllerManager.controllerAndLastPlayedSongSetup(songs)
    }

    fun getControllerManager(): MediaControllerManager = controllerManager

    fun getController(): MediaController? = controllerManager.getController()

    fun getIsNewCreation() = controllerManager.getIsNewCreation()

    fun setIsNewCreation(value: Boolean) = controllerManager.setIsNewCreation(value)

    private fun deleteSelectedSongsFromStorage(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update {
                Log.e("ks", "the ids are : ${it.selectedSongs.map { it.id }}")

                try {
                    songsRepository.deleteSongsFromLocal(uris)
                    _events.send(UiEvents.Message("Deleted Successfully"))
                    it.copy(selectedSongs = emptyList(), selectModeEnabled = false)
                } catch (ex: SecurityException) {
                    _events.send(UiEvents.IntentSender(ex, uris))
                    it
                } catch (ex: Exception) {
                    _events.send(UiEvents.Message("Deletion Failed"))
                    it
                }


            }
        }
    }

    private fun deleteSingleSongFromStorage(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                songsRepository.deleteSongsFromLocal(uris)
                _events.send(UiEvents.Message("Deleted Successfully"))
            } catch (ex: SecurityException) {
                _events.send(UiEvents.IntentSender(ex, uris))
            } catch (ex: Exception) {
                _events.send(UiEvents.Message("Deletion Failed"))

            }
        }
    }

    fun deletePlaylist(playlist: Playlist){
        viewModelScope.launch(Dispatchers.IO){
            playlistRepository.deletePlaylist(playlist)
        }
    }

    /** These just forward to the service via a custom session command - the countdown and the
    actual pause() call happen there (see [PlayerSessionService]), not here, so the timer keeps
    working while the app is backgrounded and this ViewModel/Activity is gone. **/
    private fun startSleepTimer(durationMillis: Long) {
        viewModelScope.launch {
            controllerManager.getController()?.sendCustomCommand(
                SessionCommand(CUSTOM_COMMAND_START_SLEEP_TIMER_ACTION, Bundle()),
                Bundle().apply { putLong(SLEEP_TIMER_DURATION_ARG, durationMillis) }
            )
        }
    }

    private fun startSleepTimerEndOfTrack() {
        viewModelScope.launch {
            controllerManager.getController()?.sendCustomCommand(
                SessionCommand(CUSTOM_COMMAND_START_SLEEP_TIMER_END_OF_TRACK_ACTION, Bundle()),
                Bundle.EMPTY
            )
        }
    }

    private fun cancelSleepTimer() {
        viewModelScope.launch {
            controllerManager.getController()?.sendCustomCommand(
                SessionCommand(CUSTOM_COMMAND_CANCEL_SLEEP_TIMER_ACTION, Bundle()),
                Bundle.EMPTY
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun onAction(action: UiAction) {
        val controller = controllerManager.getController() ?: return
        viewModelScope.launch {
            when (action) {
                is UiAction.ChangePlayType -> {

                    controller
                        .onChangPlayType(action.playType, ::updatePlayType)

                }

                is UiAction.ChangeToOtherSong -> {

                    when (action.index) {
                        controller.currentMediaItemIndex -> {
                            controller.playOrPause(
                                ::calculateProgressValue,
                                ::updateIsPlaying
                            )
                        }

                        else -> {
                            controller.seekToDefaultPosition(action.index)
                            updateIsPlaying(true)
                            controller.playWhenReady = true
                            controller
                                .startProgressUpdate(::calculateProgressValue)
                        }
                    }


                }

                UiAction.OnDownArrowClicked -> TODO()
                UiAction.OnMoreActionClicked -> TODO()
                UiAction.PlayPause -> controller.playOrPause(
                    ::calculateProgressValue,
                    ::updateIsPlaying
                )

                UiAction.SeekBackward -> controller.seekBack()
                UiAction.SeekForward -> controller.seekForward()
                is UiAction.SeekTo -> {

                    val seekPosition =
                        ((getPlayedSong()!!.displayableDuration.durationMillis * action.position / 100f)).toLong()

                    controller.seekTo(seekPosition)
                }

                UiAction.SeekToNext -> {
                    if (controller.repeatMode == Player.REPEAT_MODE_ONE && controller.currentMediaItemIndex == controller.mediaItemCount - 1) {
                        controller.seekTo(0, 0L)
                    } else {
                        controller.seekToNextMediaItem()

                    }
                }

                UiAction.SeekToPrevious -> {
                    if (controller.repeatMode == Player.REPEAT_MODE_ONE && controller.currentMediaItemIndex == 0) {
                        controller
                            .seekTo(controller.mediaItemCount - 1, 0L)
                    } else {
                        controller.seekToPreviousMediaItem()

                    }
                }

                is UiAction.UpdateProgress -> {
                    updateProgress(action.newProgress)
                }

                is UiAction.OnFavoriteClicked -> {
                    updateFavorite(action.songUi)
                }

                is UiAction.DeletionConfirmClicked -> {
                    when (action.deletionType) {
                        is DeletionType.StorageDeletion -> {
                            if (action.deletionType.songUi == null) deleteSelectedSongsFromStorage(
                                _state.value.selectedSongs.map { it.dataUri.toUri() }
                            )
                            else deleteSingleSongFromStorage(listOf(action.deletionType.songUi.dataUri.toUri()))
                        }

                        is DeletionType.PlaylistDeletion -> {
                            val playlistId = action.deletionType.playlistId
                            if (action.deletionType.songUi == null){
                                withContext(Dispatchers.IO){
                                    playlistSongRepository.deletePlaylistSongRefs(
                                        _state.value.selectedSongs.map {
                                            PlaylistSong(playlistId,it.id)
                                        }
                                    )
                                }

                            }else{
                                withContext(Dispatchers.IO){
                                    playlistSongRepository.deletePlaylistSongRef(
                                        PlaylistSong(playlistId,action.deletionType.songUi.id)
                                    )
                                }
                            }
                        }
                    }

                    onCancelAllSelectedSongs()
                }

                is UiAction.StartSleepTimer -> startSleepTimer(action.durationMillis)
                UiAction.StartSleepTimerEndOfTrack -> startSleepTimerEndOfTrack()
                UiAction.CancelSleepTimer -> cancelSleepTimer()
            }
        }
    }

}

