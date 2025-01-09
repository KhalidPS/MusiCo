package com.k.sekiro.musico.playmusic.presenation.played_song.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun DrawScope.drawImageOuterLine(
    line1X:() -> Float,
    line2Y:() -> Float,
    line3X:() -> Float,
    line4Y:() -> Float,
    lineStroke: Float,
    outlineColor: Color
){

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                outlineColor,
                Color.Transparent
            ),
        ),
        start = Offset(0f, 0f),
        end = Offset(line1X(), 0f),
        strokeWidth = lineStroke,
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                outlineColor,
                Color.Transparent
            )
        ),
        start = Offset(size.width, 0f),
        end = Offset(size.width, line2Y()),
        strokeWidth = lineStroke
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                outlineColor,
                Color.Transparent
            )
        ),
        start = Offset(size.width, size.height),
        end = Offset(line3X(), size.height),
        strokeWidth = lineStroke
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                outlineColor,
                Color.Transparent
            )
        ),
        start = Offset(0f, size.height),
        end = Offset(0f, line4Y()),
        strokeWidth = lineStroke
    )

}