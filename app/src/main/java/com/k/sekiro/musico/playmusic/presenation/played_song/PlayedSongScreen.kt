package com.k.sekiro.musico.playmusic.presenation.played_song

import android.os.Build
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.presenation.util.Constants
import com.k.sekiro.musico.playmusic.presenation.util.applyIf
import com.k.sekiro.musico.playmusic.presenation.util.applyIfComposable
import com.k.sekiro.musico.playmusic.presenation.util.getColorFromCover
import com.k.sekiro.musico.playmusic.presenation.util.toPx
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.component.InfoDialog
import com.k.sekiro.musico.playmusic.presenation.played_song.component.PassedTimeText
import com.k.sekiro.musico.playmusic.presenation.played_song.component.SongSlider
import com.k.sekiro.musico.playmusic.presenation.played_song.component.drawImageOuterLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlayedSongScreen(
    modifier: Modifier = Modifier,
    lurCache: LruCache<String, Palette>,
    //state: UiState,
    songs: List<SongUi>,
    favoriteSongs: List<SongUi>,
    playedSong: SongUi?,
    sliderProgress: () -> Float,
    passedTimeDuration: () -> String,
    playType: PlayType,
    isPlaying: Boolean,
    index: Int = 0,
    launchedFromBottomBar: Boolean = false,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAction: (UiAction) -> Unit,
    onDownArrowClicked: () -> Unit = {},
    onSettledPageChanged: (Long) -> Unit = {}
) {


    val context = LocalContext.current
    val resources = LocalResources.current
    val density = LocalDensity.current.density
    val pagerState = rememberPagerState(
        pageCount = { songs.size },
        initialPage = index
    )
    // var indexState = index
    val scope = rememberCoroutineScope()
    val isFavorite by remember(favoriteSongs){ derivedStateOf { favoriteSongs.contains(songs[pagerState.settledPage]) } }


    var spotColor by remember { mutableStateOf(Color.Cyan) }

    var isShowDialog by remember { mutableStateOf(false) }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // The mini player bottom bar and the song list card each have their own shared-element key
    // namespace (they can be mounted at the same time), so pick the one that matches whichever
    // element the user actually navigated from.
    val imageKey = if (launchedFromBottomBar) Constants.IMAGE_KEY else Constants.LIST_IMAGE_KEY
    val titleKey = if (launchedFromBottomBar) Constants.TITLE_KEY else Constants.LIST_TITLE_KEY
    val artistKey = if (launchedFromBottomBar) Constants.ARTIST_KEY else Constants.LIST_ARTIST_KEY

    if (isShowDialog){
        InfoDialog(
            song = songs[pagerState.settledPage],
            onDismissRequest = { isShowDialog = false },
            onCloseClicked = { isShowDialog = false },
        )
    }

    val imgWidthPx = 270.dp.toPx(density = density)
    val imgHeightPx = 350.dp.toPx(density = density)

    val line1X = remember { Animatable(initialValue = 0f) }
    val line2Y = remember { Animatable(initialValue = 0f) }
    val line3X = remember { Animatable(imgWidthPx) }
    val line4Y = remember { Animatable(imgHeightPx) }
    var outlineColor by remember { mutableStateOf(Color.Cyan) }
    val outerLineStroke =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 30f else 20f

    /*    val coilPainter = rememberAsyncImagePainter(songs[pagerState.currentPage].cover)
        val painterState = coilPainter.state.collectAsState()

        val painter = when (painterState.value) {
            is AsyncImagePainter.State.Empty -> {
                painterResource(R.drawable.logo_musico3)
            }

            is AsyncImagePainter.State.Error -> {
                painterResource(R.drawable.logo_musico3)
            }

            else -> {
                coilPainter
            }
        }*/

    LaunchedEffect(Unit) {
        Log.e("ks", "index state 1: $index")

        if (songs[index] != playedSong) {
            launch {
                onAction(UiAction.PlayPause)

            }
        }

    }


    LaunchedEffect(playedSong) {//this code to sync the pager with notification when seek to
        // other song from notification
        delay(500)
        /** this delay to prevent the pager random choose for songs with lag,
        without this delay if you choose song from list thr lag will start in pager so
        we make the check  if the current song match the settled one after 200 millis second this will
        ensure that the selected song is settled then we can check for current playing song that match the
        pager if we change song from notification**/
        if (playedSong != null && playedSong != songs[pagerState.settledPage]) {
            pagerState.animateScrollToPage(songs.indexOf(playedSong))
        }
        Log.e("ks", "the played one : $playedSong")
        Log.e("ks", "the index one :${songs.indexOf(playedSong)}")
        Log.e("ks", "the settled one ${songs[pagerState.settledPage]}")
    }


    LaunchedEffect(pagerState) {

        Log.e("ks", "index state 3: $index")

        snapshotFlow { pagerState.settledPage }.collect {

            // if (songs[indexState] != state.playedSong){
            onAction(UiAction.ChangeToOtherSong(it))
            //onStart()
            onSettledPageChanged(songs[it].id)
            onAction(UiAction.PlayPause)
            //}
           // isFavorite = isFavorite(songs[it])

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


            val song = songs[it]
            outlineColor = getColorFromCover(
                lurCache = lurCache,
                context = context,
                cover = song.cover,
                path = song.path
            )
            spotColor = outlineColor

        }

    }



    Box(
        /** Main container for all composables**/
        modifier = modifier.fillMaxSize()
    ) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            /** background for screen if the system is less than 12 then make the background
             * the image itself with alpha for it else the image with blur*/
            AsyncImage(
                model = songs[pagerState.currentPage].cover,
                error = painterResource(R.drawable.logo_musico3),
                placeholder = painterResource(R.drawable.logo_musico3),
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

            AsyncImage(
                model = songs[pagerState.currentPage].cover,
                //bitmap = songs[pagerState.currentPage].cover.asImageBitmap(),
                error = painterResource(R.drawable.logo_musico3),
                placeholder = painterResource(R.drawable.logo_musico3),
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
                    onClick = onDownArrowClicked
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isShowDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Info,
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


                    //  val songCover = songs[it].cover?:convertResToBitmap(context,R.drawable.logo_musico3)


                    AsyncImage(
                        model = songs[it].cover,
                        //bitmap = songs[it].cover.asImageBitmap(),
                        error = painterResource(R.drawable.logo_musico3),
                        placeholder = painterResource(R.drawable.logo_musico3),
                        contentDescription = null,
                        modifier = Modifier
                            // Only the settled page should participate in the shared-element
                            // transition - the pager also composes the peeking prev/next pages
                            // (contentPadding makes them partially visible), and giving every
                            // composed page a sharedBounds tied to the same
                            // animatedVisibilityScope pulled all of them into the list->player
                            // transition at once, which is what caused the lag.
                            .applyIfComposable(
                                condition = pagerState.settledPage == it
                            ) {
                                sharedBounds(
                                    sharedContentState = rememberSharedContentState("${imageKey}_${songs[it].path}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = ResizeMode.RemeasureToBounds,
                                )
                            }
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

                Spacer(Modifier.weight(1f))

                Text(
                    songs[pagerState.currentPage].title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("${titleKey}_${songs[pagerState.currentPage].path}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
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
                    songs[pagerState.currentPage].artist,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("${artistKey}_${songs[pagerState.currentPage].path}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                        ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis

                )



                SongSlider(
                    sliderProgress = sliderProgress,
                    outlineColor = outlineColor,
                    onAction = onAction
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    PassedTimeText(
                        passedTime = passedTimeDuration,
                        color = Color.White.copy(alpha = .7f)
                    )

                    Text(
                        text = songs[pagerState.currentPage].displayableDuration.formatted,
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
                            when (playType) {
                                PlayType.Shuffle -> onAction(
                                    UiAction.ChangePlayType(
                                        PlayType.RepeatAll
                                    )
                                )

                                PlayType.RepeatOne -> onAction(
                                    UiAction.ChangePlayType(
                                        PlayType.Shuffle
                                    )
                                )

                                PlayType.RepeatAll -> onAction(
                                    UiAction.ChangePlayType(
                                        PlayType.RepeatOne
                                    )
                                )
                            }
                        },

                        ) {
                        Icon(
                            imageVector = when (playType) {
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
                        onClick = {
                            onAction(UiAction.SeekToPrevious)
                            scope.launch {
                                if (pagerState.settledPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.settledPage - 1)

                                }
                            }
                        },

                        ) {
                        Icon(
                            imageVector = if (isRtl) {
                                Icons.Default.SkipNext
                            } else {
                                Icons.Default.SkipPrevious
                            },
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { /**/ },
                        modifier = Modifier
                            .size(70.dp)

                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clickable(
                                    enabled = true,
                                    onClick = {
                                        onAction(UiAction.PlayPause)
                                    },
                                ),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            onAction(UiAction.SeekToNext)
                            scope.launch {
                                if (pagerState.settledPage < songs.lastIndex)
                                    pagerState.animateScrollToPage(pagerState.settledPage + 1)
                            }

                        },

                        ) {
                        Icon(
                            imageVector = if (isRtl) {
                                Icons.Default.SkipPrevious
                            } else {
                                Icons.Default.SkipNext
                            },
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = {
                            onAction(UiAction.OnFavoriteClicked(songs[pagerState.settledPage]))
                        },

                        ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = if (isFavorite) Color.Red else Color.White.copy(
                                alpha = .5f
                            )
                        )
                    }

                }
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun PlayedSongScreenPrev() {
    SharedTransitionLayout {
        AnimatedVisibility(true) {
            PlayedSongScreen(
                lurCache = LruCache(4),
                onAction = { },
                animatedVisibilityScope = this,
                passedTimeDuration = { "" },
                songs = mockSongs.map { it.toSongUi() },
                favoriteSongs = mockSongs.map { it.toSongUi() },
                playedSong = mockSongs[0].toSongUi(),
                playType = PlayType.RepeatAll,
                isPlaying = true,
                sliderProgress = { 0f }
            )
        }
    }

}