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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.k.sekiro.musico.ui.theme.FormFactorPreviews
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import com.k.sekiro.musico.ui.theme.appDimens
import com.k.sekiro.musico.ui.theme.deviceConfiguration
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

    val playerDimens = appDimens.player
    // A phone in landscape (either size bucket) is the signal to switch from the stacked
    // cover-above-controls layout to a side-by-side one, since a portrait-sized cover would
    // otherwise crowd out the controls on a short window.
    val isCompactHeight = deviceConfiguration.isMobileLandscape

    // Plain token reads - the landscape sets *are* the landscape values, so there is nothing to
    // choose between here any more.
    val coverFullWidth = playerDimens.coverWidth
    val coverFullHeight = playerDimens.coverHeight
    // The page is the cover's width: the peek comes from the pager's content padding, so a
    // wider page would only add dead space between covers.
    val pagerPageWidth = coverFullWidth
    // Size a peeking (non-settled) page shrinks to while scrolling past it. Derived from the
    // cover rather than a flat number, so it keeps the artwork's aspect ratio at every size.
    val peekCoverWidth = coverFullWidth * playerDimens.peekCoverScale
    val peekCoverHeight = coverFullHeight * playerDimens.peekCoverScale
    val titleFontSize = playerDimens.titleFontSize
    val artistFontSize = playerDimens.artistFontSize

    val imgWidthPx = coverFullWidth.toPx(density = density)
    val imgHeightPx = coverFullHeight.toPx(density = density)

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


        // Shared pieces of the screen, factored out so the portrait (stacked) and landscape
        // (side-by-side) layouts below can arrange them differently without duplicating the
        // pager/shared-element/controls logic. They're plain local lambdas - not extracted to
        // file scope - specifically so they keep capturing this composable's SharedTransitionScope
        // receiver and local state (pagerState, the outline Animatables, spotColor, etc).

        // Natural (wrap-content) height, not weighted - it's placed inside a much shorter column
        // in the landscape branch (just the cover-pager side) than in the portrait branch (the
        // full screen), so a fixed weight fraction would size it very differently in each: too
        // short in landscape to fit the icons without compressing them. The cover/controls
        // content below it takes the actual remaining space via its own weight(1f) in both
        // branches instead.
        val topBar: @Composable () -> Unit = {
            Row(
                /** This row is for top icons on screen like (arrow down icon)**/
                modifier = Modifier
                    .fillMaxWidth()
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
                        modifier = Modifier.size(playerDimens.topIconSize),
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
                        modifier = Modifier.size(playerDimens.controlIconSize),
                        tint = Color.White
                    )
                }
            }
        }

        val coverPager: @Composable () -> Unit = {
            // Bounded to page-width-plus-peek and centered, rather than a bare fillMaxWidth
            // pager - HorizontalPager positions its settled page starting right after the
            // leading contentPadding, not centered in its own bounds, so on a tablet/desktop-wide
            // container a fillMaxWidth pager left the cover hugging the left edge with a dead
            // zone on the right. Bounding it also keeps it from overflowing a window narrower
            // than pageWidth + 2*peek.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Use the full width available and centre the settled page in it, so the
                // neighbouring covers run off the screen edge rather than stopping short of it.
                // pagerMaxPeek only bites on a window wide enough that this would start showing
                // several covers at once; on a phone it never does.
                val viewportWidth = minOf(maxWidth, pagerPageWidth + playerDimens.pagerMaxPeek * 2)
                val peekPadding = ((viewportWidth - pagerPageWidth) / 2).coerceAtLeast(0.dp)

                HorizontalPager(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(viewportWidth)
                        .height(coverFullHeight),
                    state = pagerState,
                    pageSize = PageSize.Fixed(pagerPageWidth),
                    contentPadding = PaddingValues(horizontal = peekPadding),
                ) { page ->

                val pageOffset = pagerState.getOffsetDistanceInPages(page).absoluteValue
                // or you can use pagerState.currentPageOffsetFraction instead of getOffsetDis.....

                // Non-settled (peeking) pages shrink toward peekCoverScale as they scroll away,
                // keeping the cover's aspect ratio the whole way.
                val high = lerp(coverFullHeight.value, peekCoverHeight.value, pageOffset)
                val width = lerp(coverFullWidth.value, peekCoverWidth.value, pageOffset)

                // HorizontalPager start-aligns page content, which left the settled cover
                // (pageWidth - coverWidth)/2 off-centre and made the leading peek show empty
                // page while the trailing one showed cover. Centring makes both symmetric.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                AsyncImage(
                    model = songs[page].cover,
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
                            condition = pagerState.settledPage == page
                        ) {
                            sharedBounds(
                                sharedContentState = rememberSharedContentState("${imageKey}_${songs[page].path}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = ResizeMode.RemeasureToBounds,
                            )
                        }
                        .applyIf(
                            condition = pagerState.currentPage == page && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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
                            condition = pagerState.settledPage == page,
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
                        ),
                    contentScale = ContentScale.Crop
                )
                }
                }
            }
        }

        val titleText: @Composable () -> Unit = {
            Text(
                songs[pagerState.currentPage].title,
                fontSize = titleFontSize,
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
        }

        val artistText: @Composable () -> Unit = {
            Text(
                songs[pagerState.currentPage].artist,
                fontSize = artistFontSize,
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
        }

        val slider: @Composable () -> Unit = {
            SongSlider(
                sliderProgress = sliderProgress,
                outlineColor = outlineColor,
                onAction = onAction
            )
        }

        val timeRow: @Composable () -> Unit = {
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
        }

        val controlsRow: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.size(playerDimens.controlIconSize),
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
                        modifier = Modifier.size(playerDimens.controlIconSize),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { /**/ },
                    modifier = Modifier
                        .size(playerDimens.playIconSize)

                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(playerDimens.playIconSize)
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
                        modifier = Modifier.size(playerDimens.controlIconSize),
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
                        modifier = Modifier.size(playerDimens.controlIconSize),
                        tint = if (isFavorite) Color.Red else Color.White.copy(
                            alpha = .5f
                        )
                    )
                }

            }
        }

        if (isCompactHeight) {
            // Short window (phone landscape): cover and controls side by side instead of
            // stacked, so the controls aren't squeezed under a portrait-sized cover.
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    topBar()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        coverPager()
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    titleText()
                    artistText()
                    slider()
                    timeRow()
                    Spacer(Modifier.height(8.dp))
                    controlsRow()
                }
            }
        } else {
            Column {
                topBar()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    coverPager()

                    Spacer(Modifier.weight(1f))

                    titleText()
                    artistText()
                    slider()
                    timeRow()

                    Box(Modifier.weight(2f)) {
                        controlsRow()
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@FormFactorPreviews
@Composable
private fun PlayedSongScreenPrev() {
    MusiCoTheme {
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

}