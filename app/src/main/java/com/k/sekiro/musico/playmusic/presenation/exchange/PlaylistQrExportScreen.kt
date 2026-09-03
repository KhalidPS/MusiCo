package com.k.sekiro.musico.playmusic.presenation.exchange

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.sekiro.musico.playmusic.domain.exchange.PlaylistExport
import com.k.sekiro.musico.playmusic.domain.exchange.SongFingerprint
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface QrRender {
    object Loading : QrRender
    data class Ready(val bitmap: Bitmap) : QrRender
    object TooLarge : QrRender
}

@Composable
fun PlaylistQrExportScreen(
    playlistWithSongs: PlaylistWithSongsUi,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val export = remember(playlistWithSongs) {
        PlaylistExport(
            name = playlistWithSongs.playlist.name,
            songs = playlistWithSongs.songs.map { s ->
                SongFingerprint(
                    title = s.title.ifBlank { s.name },
                    artist = s.artist,
                    album = s.album,
                    durationMs = s.displayableDuration.durationMillis,
                )
            },
        )
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(PlaylistQrCodec.encodeJson(export).toByteArray(Charsets.UTF_8))
            }
        }.onSuccess {
            Toast.makeText(context, "Playlist saved", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Couldn't save file", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBackIos, contentDescription = "back")
            }
            Text("Share playlist", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val qrSizeDp = if (maxWidth < 340.dp) maxWidth else 320.dp
            val qrSizePx = with(LocalDensity.current) { qrSizeDp.roundToPx() }

            val render by produceState<QrRender>(QrRender.Loading, export, qrSizePx) {
                value = QrRender.Loading
                value = withContext(Dispatchers.Default) {
                    try {
                        QrRender.Ready(
                            PlaylistQrCodec.generateQrBitmap(
                                PlaylistQrCodec.encode(export),
                                qrSizePx,
                            )
                        )
                    } catch (_: QrTooLargeException) {
                        QrRender.TooLarge
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = playlistWithSongs.playlist.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${export.songs.size} songs",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when (val r = render) {
                    QrRender.Loading -> CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 40.dp)
                            .size(48.dp)
                    )

                    is QrRender.Ready -> {
                        Image(
                            bitmap = r.bitmap.asImageBitmap(),
                            contentDescription = "Playlist QR code",
                            modifier = Modifier
                                .size(qrSizeDp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                        )
                        Text(
                            text = "Scan this from the other device's playlists screen.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    QrRender.TooLarge -> {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "This playlist is too big for a QR code. " +
                                "Save it as a file and share that instead.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { saveFileLauncher.launch(suggestedFileName(export.name)) },
                ) {
                    Text("Save as file")
                }
            }
        }
    }
}

private fun suggestedFileName(playlistName: String): String {
    val safe = playlistName.trim().ifBlank { "playlist" }
        .replace(Regex("[^A-Za-z0-9 _-]"), "")
        .replace(Regex("\\s+"), "_")
        .take(40)
    return "$safe.musicoplaylist.json"
}
