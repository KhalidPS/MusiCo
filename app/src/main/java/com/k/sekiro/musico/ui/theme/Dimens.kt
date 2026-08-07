package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.Dp


data class HomeDimens(
    val circularProgressXOffsetMultiplay: Float = 1.9f
)


data class ScreenDimens(
    val homeDimens: HomeDimens = HomeDimens(),
)


val compactDimens = ScreenDimens()
val compactSmallDimens = ScreenDimens()
val mediumDimens = ScreenDimens()
val expandedDimens = ScreenDimens()


fun WindowSize.getDimens(onDimensReady: (ScreenDimens) -> Unit) {
    when (this) {
        WindowSize.Compact -> onDimensReady(compactDimens)
        WindowSize.SmallCompact -> onDimensReady(compactSmallDimens)
        WindowSize.Medium -> onDimensReady(mediumDimens)
        WindowSize.Expanded -> onDimensReady(expandedDimens)
    }
}