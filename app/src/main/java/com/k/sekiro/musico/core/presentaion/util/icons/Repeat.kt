package com.k.sekiro.musico.core.presentaion.util.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MusicIcons.Repeat: ImageVector
    get() {
        if (_Loop != null) {
            return _Loop!!
        }
        _Loop = ImageVector.Builder(
            name = "Loop",
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
                moveTo(3.35355f, 1.85355f)
                curveTo(3.5488f, 1.6583f, 3.5488f, 1.3417f, 3.3535f, 1.1464f)
                curveTo(3.1583f, 0.9512f, 2.8417f, 0.9512f, 2.6465f, 1.1464f)
                lineTo(0.646447f, 3.14645f)
                curveTo(0.4512f, 3.3417f, 0.4512f, 3.6583f, 0.6464f, 3.8535f)
                lineTo(2.64645f, 5.85355f)
                curveTo(2.8417f, 6.0488f, 3.1583f, 6.0488f, 3.3535f, 5.8536f)
                curveTo(3.5488f, 5.6583f, 3.5488f, 5.3417f, 3.3535f, 5.1464f)
                lineTo(2.20711f, 4f)
                horizontalLineTo(9.5f)
                curveTo(11.433f, 4f, 13f, 5.567f, 13f, 7.5f)
                curveTo(13f, 7.7761f, 13.2239f, 8f, 13.5f, 8f)
                curveTo(13.7761f, 8f, 14f, 7.7761f, 14f, 7.5f)
                curveTo(14f, 5.0147f, 11.9853f, 3f, 9.5f, 3f)
                horizontalLineTo(2.20711f)
                lineTo(3.35355f, 1.85355f)
                close()
                moveTo(2f, 7.5f)
                curveTo(2f, 7.2239f, 1.7761f, 7f, 1.5f, 7f)
                curveTo(1.2239f, 7f, 1f, 7.2239f, 1f, 7.5f)
                curveTo(1f, 9.9853f, 3.0147f, 12f, 5.5f, 12f)
                horizontalLineTo(12.7929f)
                lineTo(11.6464f, 13.1464f)
                curveTo(11.4512f, 13.3417f, 11.4512f, 13.6583f, 11.6464f, 13.8536f)
                curveTo(11.8417f, 14.0488f, 12.1583f, 14.0488f, 12.3536f, 13.8536f)
                lineTo(14.3536f, 11.8536f)
                curveTo(14.5488f, 11.6583f, 14.5488f, 11.3417f, 14.3536f, 11.1464f)
                lineTo(12.3536f, 9.14645f)
                curveTo(12.1583f, 8.9512f, 11.8417f, 8.9512f, 11.6464f, 9.1464f)
                curveTo(11.4512f, 9.3417f, 11.4512f, 9.6583f, 11.6464f, 9.8536f)
                lineTo(12.7929f, 11f)
                horizontalLineTo(5.5f)
                curveTo(3.567f, 11f, 2f, 9.433f, 2f, 7.5f)
                close()
            }
        }.build()
        return _Loop!!
    }

private var _Loop: ImageVector? = null
