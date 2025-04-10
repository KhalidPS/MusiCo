package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.util.Log
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import com.k.sekiro.musico.core.presentaion.util.Constants
import com.k.sekiro.musico.playmusic.presenation.model.toUri
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongAction

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlayedSongBottomBar(
    song: SongUi = mockSongs[0].toSongUi(),
    lurCache: LruCache<String, Palette> = LruCache(4 * 1024 * 1024),
    isPlaying: Boolean = false,
    progress: Float,
    currentPosition: Long,
    onPlayClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAction: (PlayedSongAction) -> Unit = {},
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

    //var oldAnimValue by remember { mutableStateOf(progress) }
    //val progressAnim  = remember (isPlaying){ Animatable(progress) }
    val progressAnim = key(currentPosition) {
        animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(
                //((song.displayableDuration.durationMillis - currentPosition)).toInt(),
                (song.displayableDuration.durationMillis * (progress / 100f)).toInt(),
                easing = LinearEasing
            )
        )
    }


    /*    Log.e(
            "ks",
            "tween duration for progress :${(song.displayableDuration.durationMillis * (progress / 100f)).toInt()}"
        )
        Log.e("ks", "progress in 360: ${progressAnim.value * 360f / 100f}")*/


    /*    LaunchedEffect(isPlaying,currentPosition) {
            Log.e("ks","the current position : $currentPosition")
            if (isPlaying){
                progressAnim.animateTo(
                    targetValue = progress,
                    animationSpec = tween(
                        //((song.displayableDuration.durationMillis - currentPosition)).toInt(),
                        (song.displayableDuration.durationMillis * (progress/100f)).toInt(),
                        easing = LinearEasing
                    )
                ){
                   // oldAnimValue = value
                }
            }


        }*/




    LaunchedEffect(song) {
        val songCover = withContext(Dispatchers.Default) {
            convertUriToBitmap(
                song.cover.toUri(),
                context.contentResolver,
                context.resources
            )
        }

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
            ) else if (palette.lightVibrantSwatch != null) {
                Color(palette.lightVibrantSwatch!!.rgb)
            } else if (palette.darkVibrantSwatch != null) {
                Color(palette.darkVibrantSwatch!!.rgb)
            } else if (palette.lightMutedSwatch != null) {
                Color(palette.lightMutedSwatch!!.rgb)
            } else if (palette.mutedSwatch != null) {
                Color(palette.mutedSwatch!!.rgb)
            } else if (palette.darkMutedSwatch != null) {
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
        modifier = Modifier.clickable(onClick = onClicked)
    ) {
        Row(
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
        ) {

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {


                Text(
                    text = song.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("${Constants.TITLE_KEY}_${song.path}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .applyIf(
                            song.name.length >= 30,
                            modifier = {
                                basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 0,
                                    repeatDelayMillis = 1000
                                )
                            }
                        )
                )
                Text(
                    text = song.artist,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("${Constants.ARTIST_KEY}_${song.path}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }


            /** Here the modifier code for box was for icon without box but I added the box to make
             * the Icon size smaller than the circular progress size as in mi app**/
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .weight(.5f)
                    .clickable(
                        onClick = onPlayClicked,
                        indication = null,
                        interactionSource = null

                    )
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = .3f),
                            style = Stroke(
                                width = 2.dp.toPx()
                            )
                        )
                        drawArc(
                            color = Color.White,
                            useCenter = false,
                            style = Stroke(
                                width = 2.dp.toPx()
                            ),
                            startAngle = -90f,
                            sweepAngle = progressAnim.value * 360f / 100f,
                            size = Size(size.minDimension, size.minDimension),
                            topLeft = Offset(size.minDimension * 1.8f, 0f)

                        )


                    }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "play icon",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White

                )
            }


        }

        SongCD(
            song.cover.toUri(),
            isPlaying = isPlaying,
            animatedVisibilityScope = animatedVisibilityScope,
            path = song.path
        )
    }


}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun PlayedSongBottomBarPrev() {
    SharedTransitionScope {
        AnimatedVisibility(true) {
            PlayedSongBottomBar(
                progress = 180f,
                currentPosition = 0L,
                animatedVisibilityScope = this
            )
        }
    }

}