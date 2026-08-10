package com.k.sekiro.musico.playmusic.presenation.browse.components

import androidx.collection.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.presenation.util.getColorFromCover

/** Grid card for a browse group (artist/album) - visually matches PlaylistItem but is
decoupled from PlaylistWithSongsUi, since artists/albums are derived groupings rather than
persisted Playlist entities.**/
@Composable
fun BrowseGroupItem(
    modifier: Modifier = Modifier,
    title: String,
    songCount: Int,
    coverUrl: String,
    cacheKey: String,
    lruCache: LruCache<String, Palette>,
    onClick: () -> Unit
) {

    var backgroundColor by remember { mutableStateOf(Color.White) }
    val context = LocalContext.current

    LaunchedEffect(cacheKey, coverUrl) {
        backgroundColor = getColorFromCover(
            lurCache = lruCache,
            context = context,
            cover = coverUrl,
            path = cacheKey
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
            .clickable(onClick = onClick)
            .padding(20.dp),

        ) {
        AsyncImage(
            model = coverUrl,
            error = painterResource(R.drawable.logo_2),
            contentDescription = "$title cover",
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
            text = title,
            fontStyle = FontStyle.Italic,
            style = TextStyle(
                fontSize = if (title.length >= 10) 20.sp else 30.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        )

        Text(
            text = "$songCount ${if (songCount > 1) "songs" else "song"}",
            fontSize = 14.sp,
        )
    }

}


@Preview
@Composable
private fun BrowseGroupItemPrev() {
    BrowseGroupItem(
        title = "Ajnad Nasheed",
        songCount = 12,
        coverUrl = "",
        cacheKey = "",
        lruCache = LruCache(200),
        onClick = {}
    )
}
