package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.k.sekiro.musico.R
import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.k.sekiro.musico.core.presentaion.util.Constants

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SongCD(
    cover: Uri,
    path: String = "",
    radius: Dp = 60.dp,
    isPlaying: Boolean = true,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val painter = rememberAsyncImagePainter(cover)
    val painterState = painter.state.collectAsState()
    //var clicked by remember { mutableStateOf(false) }



/*    val baseColor = Color(0xFF9A9A9A)
    val highlightAlpha = .5f
    val highlightColor = lerp(baseColor, Color.White,.5f)
        .copy(alpha = highlightAlpha)
    val highlightColors: List<Color> = buildList {
        add(highlightColor)
        repeat(3){
            add(highlightColor.copy(alpha = 0f))
            if (it < 2) add(highlightColor)
        }
        add(highlightColor)
    }*/

/*    val infiniteTransition = rememberInfiniteTransition()

    val rotateValue = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                5000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart,
        )
    )*/

    var lastAnimValue by remember { mutableStateOf(0f) }
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



    Box(
        modifier = Modifier
            .size(radius)
            .clip(CircleShape)
            .rotate(rotateAnim.value)
        ,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(radius - 8.dp)
                .clip(CircleShape)
                .background(
                    Color.Black
                )
                .border(
                    border = BorderStroke(
                        3.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,
                            )
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(radius - 13.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(
                    border = BorderStroke(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,
                            )
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(radius - 18.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,
                                Color.White,
                                Color.Transparent,
                                Color.Transparent,

                                )
                        )
                    ),
                    shape = CircleShape
                )
        )


        Image(
            painter = when (painterState.value) {
                is AsyncImagePainter.State.Empty -> {
                    painterResource(R.drawable.logo_2)
                }

                is AsyncImagePainter.State.Error -> {
                    painterResource(R.drawable.logo_2)
                }

                else -> {
                    painter
                }
            },
            contentDescription = "song cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState("${Constants.IMAGE_KEY}_$path"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = ResizeMode.ScaleToBounds(),
                )
                .size(40.dp)
                .clip(CircleShape)
/*                .drawWithContent {

                    drawContent()

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = ringColors,
                            tileMode = TileMode.Repeated,
                        ),
                        blendMode = BlendMode.Overlay
                    )

                    drawRect(
                        brush = Brush.sweepGradient(
                            colors = highlightColors
                        )
                    )

                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = highlightColors
                        ),
                        radius = size.width * size.height
                    )

                }*/
                .clickable(
                    onClick = {/*clicked = !clicked*/}
                )
        )


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