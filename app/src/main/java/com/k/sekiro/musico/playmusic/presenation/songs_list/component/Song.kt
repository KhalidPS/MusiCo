package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi

@Composable
fun Song(
    modifier: Modifier = Modifier,
    song: SongUi,
    onClick:(SongUi) -> Unit = {},
    onLongClicked:(SongUi) -> Unit = {},
    onShareClick:() -> Unit = {},
    onMoreActionClick: () -> Unit = {},
    selectModeEnabled: Boolean = false,
    selectedSongs: List<SongUi> = emptyList()
) {

    val artist =
        if (song.artist.isBlank() || song.artist.isEmpty()) "Unknown artist" else song.artist
    val album = if (song.album.isBlank() || song.album.isEmpty()) "Unknown album" else song.album

    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = {
                    onClick(song)
                },
                onLongClick =   {
                    Log.e("ks","long clicked")
                    Log.e("ks","in long enabled $selectModeEnabled")
                    if (!selectModeEnabled){
                        Log.e("ks","inside if")
                        onLongClicked(song)
                    }
                }
            )
            .background(if (selectedSongs.contains(song)) Color.Gray else Color.Unspecified)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
       // val song = song.toSongUi(context.contentResolver,context.resources)

        trace("song Image"){
            AsyncImage(
                model = song.cover,
                error = painterResource(R.drawable.logo_2),
                placeholder = painterResource(R.drawable.logo_2),

                // bitmap = songCover.asImageBitmap(),
                contentDescription = "song album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .size(70.dp)

            )
        }


        Column(
            modifier = modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.name,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 165.dp)
                    .padding(bottom = 8.dp),
                maxLines = 1,

            )
            Text(
                text = "$artist | $album",
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier
                    .widthIn(max = 166.dp)

            )
        }


        IconButton(
            onClick = {}
        ) {
            Icon(
                imageVector = Icons.TwoTone.Share,
                contentDescription = "sharing song icon"
            )
        }


        Spacer(Modifier.width(16.dp))

        IconButton(
            onClick = {}
        ) {
            Icon(
                imageVector = Icons.TwoTone.MoreVert,
                contentDescription = "more action to do for song icon"
            )
        }

    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun SongPrev() {
            Song(
                song = mockSongs[1].toSongUi()
            )
}