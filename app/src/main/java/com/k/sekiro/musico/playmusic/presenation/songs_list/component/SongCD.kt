package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.k.sekiro.musico.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.trace
import coil3.compose.AsyncImage
import com.k.sekiro.musico.playmusic.presenation.util.Constants
import com.k.sekiro.musico.playmusic.presenation.util.applyIfComposable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SongCD(
    cover: Uri,
    path: String = "",
    radius: Dp = 60.dp,
    isPlaying: Boolean = true,
    animatedVisibilityScope: AnimatedVisibilityScope,
    bottomClicked: Boolean = false
) {




    var lastAnimValue by rememberSaveable { mutableStateOf(0f) }
    val rotateAnim = remember(isPlaying) { Animatable(lastAnimValue) }


    LaunchedEffect(isPlaying) {
        if (isPlaying){

                rotateAnim.animateTo(
                    360f + lastAnimValue,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 5000,
                            easing = LinearEasing,
                        ),
                        repeatMode = RepeatMode.Restart
                    )
                ){
                    lastAnimValue = value
                }
        }
    }

/*    LaunchedEffect(isPlaying) {
        if (isPlaying){
            rotateAnim.snapTo(lastAnimValue)
            rotateAnim.animateTo(
                360f + lastAnimValue,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 5000,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )

        }else{
            rotateAnim.stop()
            lastAnimValue = rotateAnim.value % 360
        }
    }*/



    Box(
        modifier = Modifier
            .size(radius)
            .clip(CircleShape)
            .graphicsLayer{
                rotationZ = rotateAnim.value
            }
        ,
        contentAlignment = Alignment.Center
    ) {
        CircularRing(size = radius - 8.dp)

        CircularRing(size = radius - 13.dp)

        CircularRing(size = radius - 18.dp)


        trace("CD Image"){
            AsyncImage(
                model = cover,
                error = painterResource(R.drawable.logo_musico3),
                placeholder = painterResource(R.drawable.logo_musico3),
                contentDescription = "song cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    // Same reasoning as PlayedSongBottomBar's Title/Artist: only an actual
                    // bottom-bar-originated PlayedSong transition should pull this into the
                    // shared-element transition - otherwise it's an untracked/orphaned
                    // participant (list-row navigation matches on LIST_IMAGE_KEY instead).
                    .applyIfComposable(condition = bottomClicked) {
                        sharedBounds(
                            sharedContentState = rememberSharedContentState("${Constants.IMAGE_KEY}_$path"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = ResizeMode.scaleToBounds(),
                        )
                    }
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }



    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun SongCDPrev() {
    SharedTransitionScope {
        AnimatedVisibility(true) {
            SongCD(Uri.parse(""), animatedVisibilityScope = this)

        }
    }
}