package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive dimension tokens, swapped as a whole per [DeviceConfiguration] at the theme root
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
    val player: PlayerDimens = PlayerDimens(),
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
    /** Width of Home's quick-access shelf cards (Favorite/Playlists/Recent/Artists/Albums). */
    val shelfCardWidth: Dp = 130.dp,
)

data class BrowseDimens(
    /** Minimum tile width for the artist/album/playlist grids; the grid fits as many columns as this allows. */
    val tileMinWidth: Dp = 160.dp,
    /** Expanded height of the collapsing cover header on the browse/playlist detail screens. */
    val headerHeight: Dp = 250.dp,
    /**
     * Expanded header height used instead of [headerHeight] when the window is height-compact
     * (a phone in landscape) - independent of the width bucket, since a portrait-sized header
     * would otherwise eat most of a short viewport before any scrolling happens.
     */
    val compactHeightHeaderHeight: Dp = 160.dp,
)

/** Responsive tokens for the now-playing (`PlayedSongScreen`) pager/controls. */
data class PlayerDimens(
    /** Cover art size in the stacked (portrait) layout. */
    val coverWidth: Dp = 270.dp,
    val coverHeight: Dp = 350.dp,
    /** `HorizontalPager` page width; wider than [coverWidth] so neighboring covers peek in. */
    val pagerPageWidth: Dp = 300.dp,
    val pagerPeekPadding: Dp = 60.dp,
    val topIconSize: Dp = 40.dp,
    val controlIconSize: Dp = 30.dp,
    val playIconSize: Dp = 70.dp,
    /**
     * Cover/pager size used instead of [coverWidth]/[coverHeight]/[pagerPageWidth] when the
     * window is height-compact (a phone in landscape) - fixed regardless of the width bucket,
     * sized to fit a short viewport side-by-side with the track info instead of stacked above it.
     */
    val compactHeightCoverWidth: Dp = 170.dp,
    val compactHeightCoverHeight: Dp = 220.dp,
    val compactHeightPagerPageWidth: Dp = 190.dp,
    val compactHeightPagerPeekPadding: Dp = 20.dp,
)

val CompactDimens = AppDimens()

val MediumDimens = AppDimens(
    common = CommonDimens(
        contentMaxWidth = 720.dp,
        gridSpacing = 16.dp,
        shelfCardWidth = 150.dp,
    ),
    browse = BrowseDimens(
        tileMinWidth = 190.dp,
        headerHeight = 300.dp,
    ),
    player = PlayerDimens(
        coverWidth = 300.dp,
        coverHeight = 390.dp,
        pagerPageWidth = 330.dp,
    ),
)

val ExpandedDimens = AppDimens(
    common = CommonDimens(
        contentMaxWidth = 900.dp,
        gridSpacing = 20.dp,
        shelfCardWidth = 170.dp,
    ),
    browse = BrowseDimens(
        tileMinWidth = 210.dp,
        headerHeight = 340.dp,
    ),
    player = PlayerDimens(
        coverWidth = 320.dp,
        coverHeight = 410.dp,
        pagerPageWidth = 350.dp,
    ),
)

/**
 * Maps [DeviceConfiguration]'s 5 device/orientation buckets onto the 3 width-driven token sets
 * above. [DeviceConfiguration.MOBILE_LANDSCAPE] shares [ExpandedDimens] with the tablet/desktop
 * buckets rather than [CompactDimens] - the tokens that actually vary by *width* here
 * (`contentMaxWidth`, grid/tile sizing) should scale with the extra width a landscape phone has
 * over a portrait one; the tokens that are landscape-*specific* (the `compactHeight*` fields, the
 * shelf card's flattened aspect ratio in `SongsList`) are separate, orientation-gated values read
 * directly by each screen, not selected by this width bucket at all.
 */
fun dimensFor(config: DeviceConfiguration): AppDimens = when (config) {
    DeviceConfiguration.MOBILE_PORTRAIT -> CompactDimens
    DeviceConfiguration.MOBILE_LANDSCAPE -> ExpandedDimens
    DeviceConfiguration.TABLET_PORTRAIT -> MediumDimens
    DeviceConfiguration.TABLET_LANDSCAPE -> ExpandedDimens
    DeviceConfiguration.DESKTOP -> ExpandedDimens
}
