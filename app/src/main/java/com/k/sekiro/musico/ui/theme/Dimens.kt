package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive dimension tokens, swapped as a whole per [WindowWidthSize] at the theme root
 * (see [MusiCoTheme]) and read through `LocalAppDimens` / `appDimens`.
 *
 * Every field defaults to its Compact (phone-portrait) value, so [CompactDimens] is just
 * `AppDimens()` and the wider buckets override only what actually changes. Things that are
 * design-system constants rather than responsive ones - corner radii, typography, color -
 * deliberately do NOT live here.
 */
data class AppDimens(
    val common: CommonDimens = CommonDimens(),
    val browse: BrowseDimens = BrowseDimens(),
)

data class CommonDimens(
    /**
     * Upper bound on the width of a screen's primary content column. On a phone this is
     * effectively unbounded; on wider windows it keeps text/rows from stretching into
     * unreadable full-bleed lines and centers the column instead.
     */
    val contentMaxWidth: Dp = 10_000.dp,
    /** Gap between items in the showcase grids. */
    val gridSpacing: Dp = 12.dp,
)

data class BrowseDimens(
    /** Minimum tile width for the artist/album/playlist grids; the grid fits as many columns as this allows. */
    val tileMinWidth: Dp = 160.dp,
    /** Expanded height of the collapsing cover header on the browse/playlist detail screens. */
    val headerHeight: Dp = 250.dp,
)

val CompactDimens = AppDimens()

val MediumDimens = AppDimens(
    common = CommonDimens(
        contentMaxWidth = 720.dp,
        gridSpacing = 16.dp,
    ),
    browse = BrowseDimens(
        tileMinWidth = 190.dp,
        headerHeight = 300.dp,
    ),
)

val ExpandedDimens = AppDimens(
    common = CommonDimens(
        contentMaxWidth = 900.dp,
        gridSpacing = 20.dp,
    ),
    browse = BrowseDimens(
        tileMinWidth = 210.dp,
        headerHeight = 340.dp,
    ),
)

fun dimensFor(size: WindowWidthSize): AppDimens = when (size) {
    WindowWidthSize.Compact -> CompactDimens
    WindowWidthSize.Medium -> MediumDimens
    WindowWidthSize.Expanded -> ExpandedDimens
}
