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

@Composable
fun ImportPreviewDialog(
    preview: PlaylistImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
                    enabled = preview.matchedCount > 0,
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
