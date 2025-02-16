package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.util.Log
import androidx.collection.LruCache
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.core.presentaion.util.applyIf
import com.k.sekiro.musico.core.presentaion.util.getColorFromCover
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.convertUriToBitmap
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayedSongBottomBar(
    song: SongUi = mockSongs[0].toSongUi(),
    lurCache: LruCache<String, Palette> = LruCache(4 * 1024 * 1024)
) {

    val context = LocalContext.current
   // val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()

    var bottomBarColor by remember { mutableStateOf(Color.White) }

/*    val infiniteTransition = rememberInfiniteTransition("song name animation")
    val translationXValue by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = screenWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(5000),
            repeatMode = RepeatMode.Restart
        )

    )*/



    LaunchedEffect(true) {
        val songCover = withContext(Dispatchers.Default){convertUriToBitmap(song.cover,context.contentResolver,context.resources)}

        val palette = if (lurCache[song.path] != null) {
            lurCache[song.path]!!
        } else {
            Palette.from(songCover).generate().apply {
                lurCache.put(song.path, this)
            }
        }

        bottomBarColor =
            if (palette.vibrantSwatch != null) Color(
                palette.vibrantSwatch!!.rgb
            ) else if(palette.lightVibrantSwatch != null){
                Color(palette.lightVibrantSwatch!!.rgb)
            }else if (palette.darkVibrantSwatch != null){
                Color(palette.darkVibrantSwatch!!.rgb)
            }else if (palette.lightMutedSwatch != null){
                Color(palette.lightMutedSwatch!!.rgb)
            }else if (palette.mutedSwatch != null){
                Color(palette.mutedSwatch!!.rgb)
            }else if (palette.darkMutedSwatch != null){
                Color(palette.darkMutedSwatch!!.rgb)
            } else Color.Cyan


/*        bottomBarColor = getColorFromCover(
            lurCache = lurCache,
            context = context,
            song = song
        )*/

    }


    Box(
        contentAlignment = Alignment.CenterStart,
    ){
        Row (
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawRect(
                        color = bottomBarColor,
                        topLeft = Offset(12.dp.toPx(), 0f)
                    )

                    drawContent()


                }
                .padding(start = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ){

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
/*                    modifier = Modifier.applyIf(
                        song.name.length >= 30,
                        modifier = {
                            graphicsLayer {
                                translationX+= translationXValue
                            }
                        }
                    )*/
                )
                Text(
                    text = song.artist,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "play icon",
                    modifier = Modifier.weight(.5f)
                )



        }

        SongCD(song.cover)
    }


}

@Preview
@Composable
private fun PlayedSongBottomBarPrev() {
    PlayedSongBottomBar()
}