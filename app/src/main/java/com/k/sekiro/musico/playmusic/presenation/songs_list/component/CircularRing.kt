package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircularRing(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
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
}