package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.DpSize
import androidx.window.core.layout.WindowSizeClass

enum class DeviceConfiguration {
    MOBILE_PORTRAIT_SMALL,
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE_SMALL,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP;

    /**
     * True for a phone in landscape at *any* size. Prefer this over `== MOBILE_LANDSCAPE`:
     * an equality check silently excludes [MOBILE_LANDSCAPE_SMALL], which would send a small
     * phone in landscape down the portrait branch.
     */
    val isMobileLandscape: Boolean
        get() = this == MOBILE_LANDSCAPE || this == MOBILE_LANDSCAPE_SMALL

    /** True for a phone in portrait at *any* size. Same caveat as [isMobileLandscape]. */
    val isMobilePortrait: Boolean
        get() = this == MOBILE_PORTRAIT || this == MOBILE_PORTRAIT_SMALL

    /** True for either small-phone bucket, regardless of orientation. */
    val isSmallPhone: Boolean
        get() = this == MOBILE_PORTRAIT_SMALL || this == MOBILE_LANDSCAPE_SMALL

    companion object {
        /**
         * A window whose *longer* edge is under this many dp is a "small phone".
         *
         * The official size classes can't express this: their height boundaries are 480 and 900,
         * so the `Small_Phone` AVD (360x640) and a Redmi 9 (360x800) produce *identical* size
         * classes - both width-Compact, both height-Medium - despite a 160dp height difference
         * that decides whether a fixed-height cover fits above the controls.
         *
         * 700 is the middle of the real gap between the two clusters, measured on the same
         * [DpSize] this classifier receives (a full window *including* inset areas - about 48dp
         * more than the `Configuration.screenHeightDp` figures quoted in `reports/`):
         *
         *     533  old 480x800 hdpi phone        small
         *     640  Small_Phone AVD / 5" 720p     small
         *     ---- 700 ----------------------------------
         *     780  Galaxy S23                    normal
         *     800  Redmi 9                       normal
         *     914  Pixel_7_API_33                normal
         *
         * It is also exactly where the runtime cover cap this bucket replaces used to start
         * engaging (a 350dp cover token against a 0.5 height fraction), so every device tested
         * before this bucket existed keeps the sizing it already had.
         *
         * The ~720dp "compact flagship" class (5" 1080x2160) lands just above the line and stays
         * normal - the conservative direction, since those devices get today's behavior. Raising
         * this to 750 would pull them in, at the cost of leaving only 30dp of margin above it.
         */
        private const val SMALL_PHONE_MAX_EDGE_DP = 700f

        /**
         * @param windowSizeClass the official size class, used for the tablet/desktop buckets.
         * @param windowDpSize the raw window size. Needed because [WindowSizeClass]'s own
         *   `minWidthDp`/`minHeightDp` are the *breakpoint lower bounds* it was matched to
         *   (0/600/840 and 0/480/900), not the measured size - so the size class alone cannot
         *   tell a 640dp-tall phone from a 914dp one. Consulted *only* for the small-phone
         *   check below; the other buckets still come from the library's own breakpoints.
         */
        fun from(windowSizeClass: WindowSizeClass, windowDpSize: DpSize): DeviceConfiguration {
            // Checked before the size-class buckets: a small phone's landscape window is
            // width-Medium/height-Compact and its portrait window width-Compact/height-Medium,
            // so the buckets below would classify it as an ordinary phone. Comparing the two
            // edges (rather than reading orientation off the coarse buckets) also means a window
            // that is both width-Compact and height-Compact - a split-screen view, which matched
            // none of the buckets and fell through to DESKTOP - now lands here instead.
            val longEdgeDp = maxOf(windowDpSize.width.value, windowDpSize.height.value)
            if (longEdgeDp < SMALL_PHONE_MAX_EDGE_DP) {
                return if (windowDpSize.height >= windowDpSize.width) {
                    MOBILE_PORTRAIT_SMALL
                } else {
                    MOBILE_LANDSCAPE_SMALL
                }
            }

            // WindowWidthSizeClass/WindowHeightSizeClass (and the windowWidthSizeClass/
            // windowHeightSizeClass properties that returned them) are deprecated in favor of
            // matching breakpoints directly on WindowSizeClass - isWidthAtLeastBreakpoint(600)
            // is the same "is this at least Medium width" check the old WindowWidthSizeClass.MEDIUM
            // comparison used to make, just without going through the deprecated enum.
            val isWidthCompact =
                !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
            val isWidthMedium =
                windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
                    !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
            val isWidthExpanded =
                windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

            val isHeightCompact =
                !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
            val isHeightMedium =
                windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) &&
                    !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
            val isHeightExpanded =
                windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)

            return when {
                isWidthCompact && isHeightMedium -> MOBILE_PORTRAIT
                isWidthCompact && isHeightExpanded -> MOBILE_PORTRAIT
                // Widened from width-Expanded-only: a phone rotated to landscape can easily be
                // Medium width (600-839dp - e.g. a ~360dp-wide phone lands around 640-700dp in
                // landscape) without being Expanded (>=840dp, more of a small-tablet width) -
                // both are "phone in landscape", not desktop, as long as the height is Compact.
                (isWidthMedium || isWidthExpanded) && isHeightCompact -> MOBILE_LANDSCAPE
                isWidthMedium && isHeightExpanded -> TABLET_PORTRAIT
                isWidthExpanded && isHeightMedium -> TABLET_LANDSCAPE
                else -> DESKTOP
            }
        }
    }
}
