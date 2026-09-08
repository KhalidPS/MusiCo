package com.k.sekiro.musico.playmusic.presenation.played_song.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
@Composable
fun InfoDialog(
    song: SongUi,
    modifier: Modifier = Modifier,
    onDismissRequest:() -> Unit,
    onCloseClicked:() -> Unit
) {

    val instant = Instant.fromEpochSeconds(song.addedDate)
    val dataTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val year = dataTime.year
    val day = dataTime.day
    val month = dataTime.month.number

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            // The M3 dialog container token. This used to be `onSurface` - the color meant for
            // content *on* a surface, not for the surface itself - and `contentColorFor` has no
            // mapping for it, so it fell back to LocalContentColor, which is also `onSurface`.
            // That painted the text the same color as the container in *both* themes: near-black
            // on near-black in light, near-white on near-white in dark. Using a real container
            // token lets Surface derive `onSurface` as the content color, so the text follows the
            // theme without any of the Texts below needing an explicit color.
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(.97f)
        ) {
            Column (
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ){
                Text(titleValueStyle(
                    title = "Name",
                    value = song.name
                ))
                Text(
                    titleValueStyle(
                        title = "Path",
                        value = song.path
                    )
                )

                Text(
                    titleValueStyle(
                        title = "Album",
                        value = song.album
                    )
                )

                Text(
                    titleValueStyle(
                        title = "Artist",
                        value = song.artist
                    )
                )

                Text(
                    titleValueStyle(
                        title = "Added Date",
                        value = "$day/$month/$year"
                    )
                )


                Spacer(Modifier.height(8.dp))

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = onCloseClicked
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}


private fun titleValueStyle(
    title: String,
    value: String
): AnnotatedString = buildAnnotatedString {
    withStyle(style = SpanStyle(
        fontWeight = FontWeight.Bold
    )){
        append("$title: ")
    }

    append(value)


}

@Preview
@Composable
private fun InfoDialogPrev() {
    InfoDialog(onDismissRequest = {}, onCloseClicked = {}, song = mockSongs[0].toSongUi())
}