package com.k.sekiro.musico.core.presentaion.util.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MusicIcons.TrackPrevious: ImageVector
    get() {
        if (_TrackPrevious != null) {
            return _TrackPrevious!!
        }
        _TrackPrevious = ImageVector.Builder(
            name = "TrackPrevious",
            defaultWidth = 15.dp,
            defaultHeight = 15.dp,
            viewportWidth = 15f,
            viewportHeight = 15f
        ).apply {
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
                moveTo(1.94976f, 2.74989f)
                curveTo(1.9498f, 2.4461f, 2.196f, 2.1999f, 2.4998f, 2.1999f)
                curveTo(2.8035f, 2.1999f, 3.0498f, 2.4461f, 3.0498f, 2.7499f)
                verticalLineTo(7.2825f)
                curveTo(3.0954f, 7.188f, 3.1705f, 7.1085f, 3.2666f, 7.0578f)
                lineTo(12.2666f, 2.30776f)
                curveTo(12.4216f, 2.226f, 12.6081f, 2.2313f, 12.7582f, 2.3218f)
                curveTo(12.9083f, 2.4122f, 13f, 2.5747f, 13f, 2.75f)
                verticalLineTo(12.25f)
                curveTo(13f, 12.4252f, 12.9083f, 12.5877f, 12.7582f, 12.6781f)
                curveTo(12.6081f, 12.7686f, 12.4216f, 12.7739f, 12.2666f, 12.6921f)
                lineTo(3.26662f, 7.94214f)
                curveTo(3.1705f, 7.8914f, 3.0954f, 7.8119f, 3.0498f, 7.7174f)
                verticalLineTo(12.2499f)
                curveTo(3.0498f, 12.5536f, 2.8035f, 12.7999f, 2.4998f, 12.7999f)
                curveTo(2.196f, 12.7999f, 1.9498f, 12.5536f, 1.9498f, 12.2499f)
                verticalLineTo(2.74989f)
                close()
                moveTo(4.57122f, 7.49995f)
                lineTo(12f, 11.4207f)
                verticalLineTo(3.5792f)
                lineTo(4.57122f, 7.49995f)
                close()
            }
        }.build()
        return _TrackPrevious!!
    }

private var _TrackPrevious: ImageVector? = null
