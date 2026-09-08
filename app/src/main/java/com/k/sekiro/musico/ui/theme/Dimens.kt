package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    /**
     * Width-to-height ratio of those cards. Flattened on a landscape phone: the shelf row's
     * height is derived from this, and cutting the height (rather than shrinking the width,
     * which cuts both at once and squeezes the title into the badge icon) is what leaves the
     * song list below it usable on a short window.
     */
    val shelfCardAspectRatio: Float = 0.85f,
    /** Gap above Home's shelf row - tightened on a short window, where vertical space is scarce. */
    val shelfRowSpacing: Dp = 16.dp,
)

data class BrowseDimens(
    /** Minimum tile width for the artist/album/playlist grids; the grid fits as many columns as this allows. */
    val tileMinWidth: Dp = 160.dp,
    /**
     * Expanded height of the collapsing cover header on the browse/playlist detail screens.
     * The landscape sets shrink this: a portrait-sized header would otherwise eat most of a
     * short viewport before any scrolling happens.
     */
    val headerHeight: Dp = 250.dp,
)

/** Responsive tokens for the now-playing (`PlayedSongScreen`) pager/controls. */
data class PlayerDimens(
    /** Cover art size in the stacked (portrait) layout. */
    val coverWidth: Dp = 270.dp,
    val coverHeight: Dp = 350.dp,
    /**
     * *Upper bound* on the peek either side of the settled cover - not a target.
     *
     * The pager spans the full width it is given and centres the settled page in it, so on a
     * phone the neighbouring covers run right off the screen edge. This only binds on a window
     * wide enough that a full-width pager would start showing several covers at once (a tablet or
     * desktop), where it keeps the row down to the settled cover plus one neighbour each side.
     * It is set well above what any phone has spare, so phones are always edge-to-edge.
     *
     * There is deliberately no `pagerPageWidth` token. A page used to be made wider than the
     * cover so neighbours would show; the content padding does that, so a wider page only added
     * dead space between covers. The page is the cover's width, which leaves the gap between
     * adjacent covers at `coverWidth * (1 - peekCoverScale) / 2`.
     */
    val pagerMaxPeek: Dp = 100.dp,
    /**
     * Size of a neighbouring cover relative to the settled one, as a fraction. Applied to the
     * cover's own width *and* height, so the peeking cover keeps the artwork's aspect ratio
     * instead of being squared off, and scales with whatever cover size the bucket uses.
     */
    val peekCoverScale: Float = 0.85f,
    val topIconSize: Dp = 40.dp,
    val controlIconSize: Dp = 30.dp,
    val playIconSize: Dp = 70.dp,
    /**
     * Now-playing title/artist sizes. These are the one deliberate exception to the "no
     * typography in here" rule above: they aren't a type scale, they're a consequence of the
     * layout - in landscape the title shares a narrow half-column with the controls instead of
     * spanning the window, so the portrait size truncates. The alternative is a
     * `when (deviceConfiguration)` inside the screen, which is worse.
     */
    val titleFontSize: TextUnit = 26.sp,
    val artistFontSize: TextUnit = 20.sp,
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
    ),
)

/**
 * The small-phone portrait set: [CompactDimens] with a smaller now-playing cover.
 *
 * These values replace a runtime formula that used to cap the cover against the live window
 * height (`min(coverHeight, screenHeightDp * 0.5)`, then scaled width and pager to match); they
 * are what that formula produced on the `Small_Phone` AVD, rounded. A fixed 350dp cover left
 * roughly 18dp for the flexible spacer and the whole controls row to share, pushing the
 * play/pause/repeat/favorite icons against the bottom edge.
 *
 * Note this is deliberately less adaptive than the formula *within* the bucket - a 533dp-tall
 * device gets 290dp (54% of its height) where the formula gave 50%. That trade is the point:
 * a token that can be found, compared and tuned beats a magic fraction inlined in a screen.
 */
val SmallCompactDimens = AppDimens(
    player = PlayerDimens(
        coverWidth = 225.dp,
        coverHeight = 290.dp,
    ),
)

/**
 * Phone in landscape.
 *
 * Shares [ExpandedDimens]' width-driven values (a landscape phone really is that wide) but owns
 * every height-driven one, which is why it is a set rather than a width bucket reused: the cover
 * and header shrink to fit a short viewport beside the track info instead of stacked above it,
 * and the shelf card is flattened rather than merely narrowed (see
 * [CommonDimens.shelfCardAspectRatio]).
 */
val MobileLandscapeDimens = ExpandedDimens.copy(
    common = ExpandedDimens.common.copy(
        shelfCardWidth = 130.dp,
        shelfCardAspectRatio = 1.15f,
        shelfRowSpacing = 8.dp,
    ),
    browse = ExpandedDimens.browse.copy(
        headerHeight = 160.dp,
    ),
    player = ExpandedDimens.player.copy(
        coverWidth = 170.dp,
        coverHeight = 220.dp,
        // The cover already shares the window with the whole controls column here, so the
        // neighbour has to give up more than it does in portrait to stay a peek.
        peekCoverScale = 0.8f,
        titleFontSize = 20.sp,
        artistFontSize = 16.sp,
    ),
)

/**
 * Small phone in landscape - [MobileLandscapeDimens] with a shelf row shrunk harder still.
 *
 * These are the values that were tuned on the `Small_Phone` AVD, where the row otherwise left the
 * song list a ~15px sliver. Before this bucket existed they were the *only* landscape values, so
 * a much larger landscape phone was getting a shelf row sized for a 640dp-wide window too.
 */
val SmallMobileLandscapeDimens = MobileLandscapeDimens.copy(
    common = MobileLandscapeDimens.common.copy(
        shelfCardWidth = 110.dp,
        shelfCardAspectRatio = 1.25f,
    ),
)

/**
 * Maps each [DeviceConfiguration] onto a token set.
 *
 * Every value a screen needs is now selected here, by configuration. There are no longer parallel
 * `compactHeight*` fields for a screen to choose between at runtime - the landscape sets simply
 * *are* the landscape values - so `deviceConfiguration` is read in screens only to switch layout
 * (stacked vs side-by-side), never to pick a size.
 *
 * The two `*_SMALL` buckets differ from their normal counterparts only where a measurement
 * actually justified it - the now-playing cover in portrait, the shelf row in landscape. Nothing
 * else is guessed: a small phone in portrait keeps [CompactDimens]' shelf row, confirmed fine on
 * the `Small_Phone` AVD.
 */
fun dimensFor(config: DeviceConfiguration): AppDimens = when (config) {
    DeviceConfiguration.MOBILE_PORTRAIT_SMALL -> SmallCompactDimens
    DeviceConfiguration.MOBILE_PORTRAIT -> CompactDimens
    DeviceConfiguration.MOBILE_LANDSCAPE_SMALL -> SmallMobileLandscapeDimens
    DeviceConfiguration.MOBILE_LANDSCAPE -> MobileLandscapeDimens
    DeviceConfiguration.TABLET_PORTRAIT -> MediumDimens
    DeviceConfiguration.TABLET_LANDSCAPE -> ExpandedDimens
    DeviceConfiguration.DESKTOP -> ExpandedDimens
}
