package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.mockPlaylists
import com.k.sekiro.musico.ui.theme.PlayListColor

@Composable
fun PlayListBox(
    modifier: Modifier = Modifier,
    boxColor: Color,
    playListIcon: ImageVector,
    playListIconTint: Color = LocalContentColor.current,
    onClick:(Long) -> Unit = {},
    playlist: PlaylistWithSongsUi,
    playlistName: String = ""
) {

    Box(
        modifier
            .width(120.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(true, onClick = { onClick(playlist.playlist.id) })

    ){

        AsyncImage(
            model = if (playlist.songs.isNotEmpty()) playlist.songs[0].cover else "",
            error = painterResource(R.drawable.logo_2),
            contentScale = ContentScale.Crop,
            contentDescription = "playlist recent song's image",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = .7f
                }
                .background(boxColor)
        )

        Column (
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        ){
            Icon(
                imageVector = playListIcon,
                contentDescription = "playlist icon",
                tint = playListIconTint
            )

            Text(
               text = playlistName.ifEmpty { playlist.playlist.name },
                fontWeight = FontWeight.Bold,
            )
        }
    }

}


@Preview
@Composable
private fun PlayListBoxPrev() {
    PlayListBox(
        boxColor = Color.Cyan,
        playListIcon = Icons.Default.Favorite,
        playListIconTint = Color.Red,
        playlist = mockPlaylists[0]
        )
}