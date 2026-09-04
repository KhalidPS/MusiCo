package com.k.sekiro.musico.playmusic.presenation.exchange.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.k.sekiro.musico.playmusic.presenation.exchange.PlaylistImportPreview
import com.k.sekiro.musico.playmusic.presenation.exchange.TransferState

private fun formatMb(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000.0)

@Composable
fun ImportPreviewDialog(
    preview: PlaylistImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    transferState: TransferState? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
        ) {
            Text("Import playlist", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "“${preview.name}”",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${preview.matchedCount} of ${preview.totalCount} songs found in your library",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            preview.transferOffer?.let { offer ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "+ ${offer.newTrackCount} new song${if (offer.newTrackCount == 1) "" else "s"}" +
                        " (${formatMb(offer.totalBytes)}) can be sent from the other device",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (transferState != null && transferState !is TransferState.Idle) {
                Spacer(Modifier.height(12.dp))
                when (transferState) {
                    // Advertising is sender-only - this dialog only ever shows on the receiver.
                    is TransferState.Advertising -> {}
                    is TransferState.Connecting -> Text("Connecting…", fontSize = 13.sp)
                    is TransferState.Downloading -> {
                        val percent = if (transferState.bytesTotal > 0) {
                            ((transferState.bytesDone * 100) / transferState.bytesTotal).toInt()
                        } else 0
                        val positionSuffix = if (transferState.count > 0) {
                            " (${transferState.index + 1}/${transferState.count})"
                        } else ""
                        Text(
                            "Receiving ${transferState.title}$positionSuffix — $percent%",
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        val fraction = if (transferState.bytesTotal > 0) {
                            (transferState.bytesDone.toFloat() / transferState.bytesTotal).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is TransferState.Verifying -> Text("Verifying…", fontSize = 13.sp)
                    is TransferState.Done -> Text(
                        "Received ${transferState.added} song${if (transferState.added == 1) "" else "s"}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is TransferState.Failed -> Text(
                        transferState.reason,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (preview.unmatchedTitles.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Not found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    preview.unmatchedTitles.forEach { title ->
                        Text(
                            text = "• $title",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    // A transfer offer can fetch every unmatched song from the other device, so
                    // it only needs *some* song in the playlist to exist (matched locally or
                    // offered over the wire) - unlike a plain (no-transfer) import, which can only
                    // ever add songs already matched in this device's own library.
                    enabled = if (preview.transferOffer != null) {
                        preview.totalCount > 0
                    } else {
                        preview.matchedCount > 0
                    },
                ) { Text("Import") }
            }
        }
    }
}

@Preview
@Composable
private fun ImportPreviewDialogPreview() {
    ImportPreviewDialog(
        preview = PlaylistImportPreview(
            name = "Road trip",
            matchedCount = 8,
            totalCount = 11,
            unmatchedTitles = listOf("Some Rare Track", "Another Missing One", "Third"),
        ),
        onConfirm = {},
        onDismiss = {},
    )
}
