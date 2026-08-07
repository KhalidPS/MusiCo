package com.k.sekiro.musico.playmusic.presenation.showcase_playlists.components

import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.mockPlaylists
import com.k.sekiro.musico.playmusic.presenation.util.getColorFromCover


@Composable
fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: PlaylistWithSongsUi = mockPlaylists[0],
    lruCache: LruCache<String, Palette> = LruCache(200),
    onClick:(Long) -> Unit = {}
) {

    var backgroundColor by remember { mutableStateOf(Color.White) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        backgroundColor = getColorFromCover(
            lurCache = lruCache,
            context = context,
            cover = if (playlist.songs.isEmpty()) "" else playlist.songs[0].cover!!,
            path = if (playlist.songs.isEmpty()) "" else playlist.songs[0].path
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.5f),
                        Color.Transparent,
                        backgroundColor.copy(alpha = 0.1f)
                    ),
                )
            )
            .clickable(
                onClick = { onClick(playlist.playlist.id) }
            )
            .padding(20.dp),

    ) {
        AsyncImage(
            model = if (playlist.songs.isNotEmpty()) playlist.songs[0].cover else "",
            error = painterResource(R.drawable.logo_2),
            contentDescription = "playlist cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(
            Modifier
                .height(12.dp)
        )

        Text(
            text = playlist.playlist.name,
            fontStyle = FontStyle.Italic,
            style = TextStyle(
                fontSize = if (playlist.playlist.name.length >= 10) 20.sp else 30.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        )

        Text(
            text = "${playlist.songs.size} ${if(playlist.songs.size > 1) "songs" else "song"}",
            fontSize = 14.sp,
        )
    }

}


@Preview
@Composable
private fun PlaylistItemPrev() {
    PlaylistItem()
}