package com.k.sekiro.musico.playmusic.presenation.played_song

import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.twotone.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.placeholder
import com.k.sekiro.musico.R
import com.k.sekiro.musico.core.presentaion.util.applyIf
import com.k.sekiro.musico.core.presentaion.util.convertResToBitmap
import com.k.sekiro.musico.core.presentaion.util.getColorFromCover
import com.k.sekiro.musico.core.presentaion.util.icons.MusicIcons
import com.k.sekiro.musico.core.presentaion.util.icons.Pause
import com.k.sekiro.musico.core.presentaion.util.icons.Repeat
import com.k.sekiro.musico.core.presentaion.util.icons.TrackNext
import com.k.sekiro.musico.core.presentaion.util.icons.TrackPrevious
import com.k.sekiro.musico.core.presentaion.util.toPx
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.convertUriToBitmap
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.component.drawImageOuterLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@Composable
fun PlayedSongScreen(
    modifier: Modifier = Modifier,
    lurCache: LruCache<String, Palette>,
    state: PlayedSongState,
    onAction:(PlayedSongAction) -> Unit
) {


    val context = LocalContext.current
    val density = LocalDensity.current.density
    val pagerState = rememberPagerState(pageCount = { state.songs.size })
    val scope = rememberCoroutineScope()




    var spotColor by remember { mutableStateOf(Color.Cyan) }

    /*    val infiniteTransition = rememberInfiniteTransition(label = "")
        val colorAnimation = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )
        */


    val imgWidthPx = 270.dp.toPx(density = density)
    val imgHeightPx = 350.dp.toPx(density = density)

    val line1X = remember { Animatable(initialValue = 0f) }
    val line2Y = remember { Animatable(initialValue = 0f) }
    val line3X = remember { Animatable(imgWidthPx) }
    val line4Y = remember { Animatable(imgHeightPx) }
    var outlineColor by remember { mutableStateOf(Color.Cyan) }
    val outerLineStroke =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 30f else 20f

    val coilPainter = rememberAsyncImagePainter(state.songs[pagerState.currentPage].cover)
    val painterState = coilPainter.state.collectAsState()

    val painter = when(painterState.value){
        is  AsyncImagePainter.State.Empty -> {
            painterResource(R.drawable.logo_2)
        }
        is AsyncImagePainter.State.Error -> {
            painterResource(R.drawable.logo_2)
        }
        else -> {coilPainter}
    }


    LaunchedEffect(state.playedSong) {
        if (state.playedSong != null && state.playedSong != state.songs[pagerState.settledPage]){
            pagerState.animateScrollToPage(state.songs.indexOf(state.playedSong))
        }
    }


    LaunchedEffect(pagerState) {


        snapshotFlow { pagerState.settledPage }.collect {

            onAction(PlayedSongAction.ChangeToOtherSong(it))
            //onStart()
            onAction(PlayedSongAction.PlayPause)

            val job1 = launch { line1X.snapTo(0f) }
            val job2 = launch { line2Y.snapTo(0f) }
            val job3 = launch { line3X.snapTo(imgWidthPx) }
            val job4 = launch { line4Y.snapTo(imgHeightPx) }
            job1.join()
            job2.join()
            job3.join()
            job4.join()

            launch {
                line1X.animateTo(
                    imgWidthPx,
                    animationSpec = tween(
                        durationMillis = 200
                    )
                )
                line2Y.animateTo(
                    imgHeightPx,
                    animationSpec = tween(
                        durationMillis = 200,
                    )
                )
                line3X.animateTo(
                    0f,
                    animationSpec = tween(
                        durationMillis = 200
                    )
                )
                line4Y.animateTo(
                    0f,
                    animationSpec = tween(
                        durationMillis = 200
                    )
                )
            }

            val song = state.songs[it]

/*            outlineColor = getColorFromCover(
                lurCache = lurCache,
                context = context,
                song = song
            )

            spotColor = outlineColor*/

                        launch(Dispatchers.Default) {

                            val song = state.songs[it]
                            val songCover = convertUriToBitmap(song.cover,context.contentResolver,context.resources)

                            val palette = if (lurCache[song.path] != null) {
                                Log.e("ks", "$it :${lurCache[song.path]}")
                                lurCache[song.path]!!
                            } else {
                                Palette.from(songCover).generate().apply {
                                    lurCache.put(song.path, this)
                                }
                            }


                            withContext(Dispatchers.Main.immediate) {
                                outlineColor =
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
                                spotColor = outlineColor

                            }


                        }
            /*.join()
            snapshotFlow { colorAnimation.value }.collect { value ->
                spotColor = lerp(outlineColor, Color.White, value)
            }*/

        }

    }



    Box(
        /** Main container for all composables**/
        modifier = modifier.fillMaxSize()
    ) {





        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            /** background for screen if the system is less than 12 then make the background
             * the image itself with alpha for it else the image with blur*/
            Image(
                painter = painter,
                //bitmap = songs[pagerState.currentPage].cover.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = .3f
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = Color.Black,
                            size = Size(size.width, size.height),
                            alpha = .7f, //.7f or .5f
                        )

                    },
                contentScale = ContentScale.Crop
            )


        } else {

            Image(
                painter = painter,
                //bitmap = songs[pagerState.currentPage].cover.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .5f))
                    .blur(60.dp)
                    .padding(top = 16.dp),
                contentScale = ContentScale.Crop
            )
        }


        Column {
            Row(
                /** This row is for top icons on screen like (arrow down icon)**/
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(.1f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /** TODO() **/ }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {


                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    state = pagerState,
                    pageSize = PageSize.Fixed(300.dp),
                    contentPadding = PaddingValues(horizontal = 60.dp),
                ) {


                    val pageOffset = pagerState.getOffsetDistanceInPages(it).absoluteValue
                    // or you can use pagerState.currentPageOffsetFraction instead of getOffsetDis.....

                    /*                   val imgHeight by animateDpAsState(
                                           targetValue = if (pageOffset in (0.1f..2.0f)) 250.dp else 350.dp
                                       )

                                       val imgWidth by animateDpAsState(
                                           targetValue = if (pageOffset in (0.1f..2.0f)) 250.dp else 270.dp
                                       )*/

                    // or using lerp() instead of animateDpAsState as the code below

                    val high = lerp(350f, 250f, pageOffset)
                    val width = lerp(270f, 250f, pageOffset)


                    /*                    LaunchedEffect(Unit) {
                                            launch(Dispatchers.Default) {
                                                val palette =
                                                    Palette.Builder(convertResToBitmap(context, songs[it].cover))
                                                        .generate { palette ->
                                                            outlineColor =
                                                                if (palette != null && palette.vibrantSwatch != null) Color(
                                                                    palette.vibrantSwatch!!.rgb
                                                                ) else Color.Cyan
                                                        }

                                            }.join()
                                            snapshotFlow { colorAnimation.value }.collect { value ->
                                                spotColor = lerp(outlineColor, Color.White, value)
                                            }
                                        }*/



                  //  val songCover = songs[it].cover?:convertResToBitmap(context,R.drawable.logo_2)

                    val painter = rememberAsyncImagePainter(state.songs[it].cover)
                    val painterState = painter.state.collectAsState()


                    Image(
                        painter = when(painterState.value){
                            is  AsyncImagePainter.State.Empty -> {
                                painterResource(R.drawable.logo_2)
                            }
                            is AsyncImagePainter.State.Error -> {
                                painterResource(R.drawable.logo_2)
                            }
                            else -> {painter}
                        },
                        //bitmap = songs[it].cover.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .applyIf(
                                condition = pagerState.currentPage == it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                                modifier = {
                                    Modifier.shadow(
                                        elevation = 20.dp,
                                        ambientColor = spotColor,
                                        spotColor = spotColor,
                                    )
                                }
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .width(Dp(width))
                            .height(Dp(high))
                            .applyIf(
                                condition = pagerState.settledPage == it,
                                modifier = {
                                    Modifier.drawWithContent {
                                        drawContent()

                                        drawImageOuterLine(
                                            line1X = { line1X.value },
                                            line2Y = { line2Y.value },
                                            line3X = { line3X.value },
                                            line4Y = { line4Y.value },
                                            lineStroke = outerLineStroke,
                                            outlineColor = outlineColor
                                        )


                                    }
                                }
                            )
                            .graphicsLayer {
                                val scale = lerp(1f, 1.75f, pageOffset)
                                scaleX *= scale
                                scaleY *= scale
                            },
                        contentScale = ContentScale.Crop
                    )


                }


                /*   Image(
                       painter = painterResource(R.drawable.song_cover),
                       contentDescription = null,
                       modifier = Modifier
                           .clip(RoundedCornerShape(12.dp))
                           .size(
                               height = 300.dp,
                               width = 250.dp
                           ),
                       contentScale = ContentScale.Crop
                   )*/
                Spacer(Modifier.weight(1f))

                Text(
                    state.songs[pagerState.currentPage].title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1

                )

                Text(
                    state.songs[pagerState.currentPage].artist,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                        ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis

                )



                Slider(
                    value = state.sliderProgress,
                    onValueChange = {
                        onAction(PlayedSongAction.SeekTo(it))
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(text = state.passedTimeDuration, color = Color.White.copy(alpha = .7f))

                    Text(
                        text = state.songs[pagerState.currentPage].displayableDuration.formatted,
                        color = Color.White.copy(alpha = .7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                ) {

                    IconButton(
                        onClick = {
                            when(state.playType){
                                PlayType.Shuffle -> onAction(PlayedSongAction.ChangePlayType(PlayType.RepeatAll))
                                PlayType.RepeatOne -> onAction(PlayedSongAction.ChangePlayType(PlayType.Shuffle))
                                PlayType.RepeatAll -> onAction(PlayedSongAction.ChangePlayType(PlayType.RepeatOne))
                            }
                        },

                        ) {
                        Icon(
                            imageVector = when(state.playType){
                                PlayType.Shuffle -> Icons.Default.Shuffle
                                PlayType.RepeatOne -> Icons.Default.RepeatOne
                                PlayType.RepeatAll -> Icons.Default.Repeat
                            },
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { onAction(PlayedSongAction.SeekToPrevious) },

                        ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { /**/ },
                        modifier = Modifier
                            .size(70.dp)
                          /*  .background(
                                color = Color.Black,
                                shape = CircleShape
                            )*/

                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clickable(
                                    enabled = true,
                                    onClick = {
                                        onAction(PlayedSongAction.PlayPause)
                                    },
                                ),
                            tint = Color.White
                            )
                    }

                    IconButton(
                        onClick = { onAction(PlayedSongAction.SeekToNext) },

                        ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = { /**/ },

                        ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.Red
                        )
                    }

                }
            }
        }
    }
}


@Preview
@Composable
private fun PlayedSongScreenPrev() {
      PlayedSongScreen(
          lurCache = LruCache(4),
          state = PlayedSongState(),
          onAction = {},
      )
}