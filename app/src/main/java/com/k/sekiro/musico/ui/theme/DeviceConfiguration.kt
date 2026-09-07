package com.k.sekiro.musico.ui.theme

import androidx.window.core.layout.WindowSizeClass

enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP;

    companion object {
        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass): DeviceConfiguration {
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