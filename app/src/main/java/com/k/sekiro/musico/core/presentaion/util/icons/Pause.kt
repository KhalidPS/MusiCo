package com.k.sekiro.musico.core.presentaion.util.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MusicIcons.Pause: ImageVector
    get() {
        if (_Pause != null) {
            return _Pause!!
        }
        _Pause = ImageVector.Builder(
            name = "Pause",
            defaultWidth = 70.dp,
            defaultHeight = 70.dp,
            viewportWidth = 70f,
            viewportHeight = 70f
        ).apply {

            val scaleFactor = 70f/15f //15f older viewport
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(6.04995f*scaleFactor, 2.74998f)
                curveTo(6.0499f*scaleFactor, 2.4462f*scaleFactor, 5.8037f*scaleFactor, 2.2f*scaleFactor, 5.5f*scaleFactor, 2.2f*scaleFactor)
                curveTo(5.1962f*scaleFactor, 2.2f*scaleFactor, 4.95f*scaleFactor, 2.4462f*scaleFactor, 4.95f*scaleFactor, 2.75f*scaleFactor)
                verticalLineTo(12.25f*scaleFactor)
                curveTo(4.95f*scaleFactor, 12.5537f*scaleFactor, 5.1962f*scaleFactor, 12.8f*scaleFactor, 5.5f*scaleFactor, 12.8f*scaleFactor)
                curveTo(5.8037f*scaleFactor, 12.8f*scaleFactor, 6.0499f*scaleFactor, 12.5537f*scaleFactor, 6.0499f*scaleFactor, 12.25f*scaleFactor)
                verticalLineTo(2.74998f*scaleFactor)
                close()
                moveTo(10.05f*scaleFactor, 2.74998f*scaleFactor)
                curveTo(10.05f*scaleFactor, 2.4462f*scaleFactor, 9.8037f*scaleFactor, 2.2f*scaleFactor, 9.5f*scaleFactor, 2.2f*scaleFactor)
                curveTo(9.1962f*scaleFactor, 2.2f*scaleFactor, 8.9499f*scaleFactor, 2.4462f*scaleFactor, 8.9499f*scaleFactor, 2.75f*scaleFactor)
                verticalLineTo(12.25f*scaleFactor)
                curveTo(8.9499f*scaleFactor, 12.5537f*scaleFactor, 9.1962f*scaleFactor, 12.8f*scaleFactor, 9.5f*scaleFactor, 12.8f*scaleFactor)
                curveTo(9.8037f*scaleFactor, 12.8f*scaleFactor, 10.05f*scaleFactor, 12.5537f*scaleFactor, 10.05f*scaleFactor, 12.25f*scaleFactor)
                verticalLineTo(2.74998f*scaleFactor)
                close()
            }
        }.build()
        return _Pause!!
    }

private var _Pause: ImageVector? = null
