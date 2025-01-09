package com.k.sekiro.musico.playmusic.presenation.loading_screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs


@Preview
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            CustomAudioLoader()

            Text("Please Wait...")
        }


    }
}

@Preview
@Composable
fun CustomAudioLoader(
    width: Dp = 50.dp,
    height: Dp = 100.dp,
    barWidth: Dp = 10.dp,
    color: Color = Color.Cyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio loader animation")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio loader animation"
    )

    Canvas(modifier = Modifier.size(width = width, height = height)) {
        val barWidth = barWidth.toPx()
        val barSpacing = 10.dp.toPx()
        val halfBarHeight = size.height / 2f

        // Calculate bar height changes based on animation progress
        val middleBarHeightChange = halfBarHeight * (1 - 2 * abs(animationProgress - 0.57f))
        val sideBarHeightChange = halfBarHeight * abs(animationProgress - 0.5f)

        // Draw middle bar
        drawRoundRect(
            color = color,
            topLeft = Offset(barWidth + barSpacing, halfBarHeight - middleBarHeightChange),
            size = Size(barWidth, 2 * middleBarHeightChange),
            cornerRadius = CornerRadius(12.dp.toPx(),12.dp.toPx())
        )

        // Draw left bar
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, halfBarHeight - sideBarHeightChange),
            size = Size(barWidth, 2 * sideBarHeightChange),
            cornerRadius = CornerRadius(12.dp.toPx(),12.dp.toPx())
        )

        // Draw right bar
        drawRoundRect(
            color = color,
            topLeft = Offset(2 * barWidth + 2 * barSpacing, halfBarHeight - sideBarHeightChange),
            size = Size(barWidth, 2 * sideBarHeightChange),
            cornerRadius = CornerRadius(12.dp.toPx(),12.dp.toPx())
        )
    }
}

@Composable
fun AudioLoader(
    barsCount: Int = 3,
    maxHeight: Dp = 100.dp,
    animationState: Float = 50f
) {
    Canvas(
        modifier = Modifier.size(width = 200.dp, height = maxHeight)) {
        for (i in 0 until barsCount) {
            val barWidth = 10.dp.toPx() /*size.width / barsCount*/
            val barHeight = maxHeight * (animationState + Random.nextFloat() * 0.2f).coerceIn(0f, 1f)
            drawRoundRect(
                color = Color.Blue,
                topLeft = Offset(i * barWidth, size.height - barHeight.value),
                size = Size(barWidth, barHeight.value),
                cornerRadius = CornerRadius(12.dp.toPx(),12.dp.toPx())
            )
        }
    }
}


@Composable
fun AnimatedAudioLoader() {
    val infiniteTransition = rememberInfiniteTransition()
    val animationState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loader audio animation"
    )

    AudioLoader(animationState = animationState)
}