package com.k.sekiro.musico.ui.theme

/**
 * Coarse width bucket for the current window, using the Material 3 width breakpoints
 * (600dp / 840dp). Drives [AppDimens] selection in [MusiCoTheme].
 *
 * We bucket a plain width-in-dp here rather than pulling in `material3-window-size-class`
 * or `material3-adaptive` - the app only needs three buckets and one computation at the
 * theme root, and the width is already available from the composition.
 */
enum class WindowWidthSize {
    /** Phone portrait and small phones (< 600dp). */
    Compact,

    /** Large phone landscape, small tablets, unfolded foldables (600dp - 839dp). */
    Medium,

    /** Tablets, desktop, TV (>= 840dp). */
    Expanded,
}

fun windowWidthSizeOf(widthDp: Int): WindowWidthSize = when {
    widthDp < 600 -> WindowWidthSize.Compact
    widthDp < 840 -> WindowWidthSize.Medium
    else -> WindowWidthSize.Expanded
}
