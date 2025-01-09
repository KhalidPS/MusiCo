package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.k.sekiro.musico.R
import com.k.sekiro.musico.ui.theme.PlayListColor

@Composable
fun PlayListBox(
    modifier: Modifier = Modifier,
    boxColor: Color,
    @DrawableRes latestSongImagePerPlayList: Int,
    playListIcon: ImageVector,
    playListIconTint: Color = LocalContentColor.current,
    playListName: String
) {

    Box(
        modifier
            .width(120.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))

    ){
        Image(
            painter = painterResource(latestSongImagePerPlayList),
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
               text =  playListName,
                fontWeight = FontWeight.Bold
            )
        }
    }

}


@Preview
@Composable
private fun PlayListBoxPrev() {
    PlayListBox(
        boxColor = Color.Cyan,
        latestSongImagePerPlayList = R.drawable.funk,
        playListIcon = Icons.Default.Favorite,
        playListName = "Favorite",
        playListIconTint = Color.Red
        )
}