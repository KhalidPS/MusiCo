package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the window sizes each [DeviceConfiguration] bucket is meant to separate.
 *
 * [DeviceConfiguration.from] is pure and takes plain values, so the whole device table runs on
 * the JVM without an emulator - which is the point: the small-phone breakpoint was chosen against
 * these specific measurements, and this is where that reasoning stays checkable.
 *
 * Sizes are the *raw window* (insets included), matching what
 * `LocalWindowInfo.current.containerDpSize` reports - roughly 48dp taller than the
 * `Configuration.screenHeightDp` figures quoted in `reports/`.
 */
class DeviceConfigurationTest {

    private fun classify(widthDp: Int, heightDp: Int): DeviceConfiguration =
        DeviceConfiguration.from(
            // Same breakpoint set currentWindowAdaptiveInfoV2() uses.
            windowSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                widthDp = widthDp,
                heightDp = heightDp,
            ),
            windowDpSize = DpSize(widthDp.dp, heightDp.dp),
        )

    // --- small phones -------------------------------------------------------------------

    @Test
    fun `Small_Phone AVD portrait is a small phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT_SMALL, classify(360, 640))
    }

    @Test
    fun `Small_Phone AVD landscape is a small phone`() {
        assertEquals(DeviceConfiguration.MOBILE_LANDSCAPE_SMALL, classify(640, 360))
    }

    @Test
    fun `an old 480x800 hdpi phone is a small phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT_SMALL, classify(320, 533))
    }

    /**
     * The whole point of the bucket, and why the raw [DpSize] has to be passed alongside the
     * size class: the Small_Phone AVD and a Redmi 9 are both width-Compact and height-Medium, so
     * they produce byte-for-byte identical [WindowSizeClass]es despite a 160dp height difference.
     *
     * (A Pixel 7 would *not* work as the counter-example here - at 914dp it crosses into
     * height-Expanded, so the size class alone does separate it. Redmi 9 is the case that
     * actually forced this change.)
     */
    @Test
    fun `size class alone cannot separate a small phone from a normal one`() {
        val small = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(360, 640)
        val normal = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(360, 800)
        assertEquals(small.minWidthDp, normal.minWidthDp)
        assertEquals(small.minHeightDp, normal.minHeightDp)
        // ...yet they must classify differently.
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT_SMALL, classify(360, 640))
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(360, 800))
    }

    // --- normal phones ------------------------------------------------------------------

    @Test
    fun `Redmi 9 portrait is a normal phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(360, 800))
    }

    @Test
    fun `Galaxy S23 portrait is a normal phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(360, 780))
    }

    @Test
    fun `Pixel_7_API_33 portrait is a normal phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(412, 914))
    }

    @Test
    fun `Pixel_7_API_33 landscape is a normal phone`() {
        assertEquals(DeviceConfiguration.MOBILE_LANDSCAPE, classify(914, 412))
    }

    /**
     * A landscape phone at Medium width (600-839dp) rather than Expanded - the case the
     * MOBILE_LANDSCAPE branch was widened for. Above the small threshold, so it stays normal.
     */
    @Test
    fun `a medium-width landscape phone is a normal landscape phone`() {
        assertEquals(DeviceConfiguration.MOBILE_LANDSCAPE, classify(800, 400))
    }

    // --- the breakpoint itself ----------------------------------------------------------

    @Test
    fun `the small threshold is exclusive at 700dp on the long edge`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT_SMALL, classify(360, 699))
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(360, 700))
    }

    /**
     * The 5" 1080x2160 "compact flagship" class sits just above the line and is deliberately
     * treated as a normal phone. Change this test consciously, not incidentally.
     */
    @Test
    fun `a 720dp compact flagship is deliberately left as a normal phone`() {
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT, classify(360, 720))
    }

    @Test
    fun `smallness is a property of the device, not the orientation`() {
        // The same physical window, rotated, must agree on whether it is small.
        assertEquals(classify(360, 640).isSmallPhone, classify(640, 360).isSmallPhone)
        assertEquals(classify(412, 914).isSmallPhone, classify(914, 412).isSmallPhone)
    }

    // --- larger form factors are unaffected ---------------------------------------------

    @Test
    fun `a 7-inch tablet in portrait is a tablet`() {
        assertEquals(DeviceConfiguration.TABLET_PORTRAIT, classify(600, 960))
    }

    @Test
    fun `Medium_Tablet landscape is a tablet`() {
        assertEquals(DeviceConfiguration.TABLET_LANDSCAPE, classify(1280, 800))
    }

    @Test
    fun `a desktop-sized window is a desktop`() {
        assertEquals(DeviceConfiguration.DESKTOP, classify(1600, 1200))
    }

    // --- the fall-through this bucket closes --------------------------------------------

    /**
     * Width-Compact *and* height-Compact matched none of the five original buckets and fell
     * through to DESKTOP - documented as a known limitation in
     * `reports/device-configuration-migration.md`. Its long edge is under 700, so the small-phone
     * branch now catches it first.
     */
    @Test
    fun `a split-screen window is a small phone, not a desktop`() {
        val splitScreen = classify(412, 457)
        assertEquals(DeviceConfiguration.MOBILE_PORTRAIT_SMALL, splitScreen)
    }

    @Test
    fun `a short wide split-screen window is a small landscape phone`() {
        assertEquals(DeviceConfiguration.MOBILE_LANDSCAPE_SMALL, classify(560, 400))
    }

    // --- the derived properties call sites rely on --------------------------------------

    @Test
    fun `isMobileLandscape covers both landscape phone buckets`() {
        assertTrue(DeviceConfiguration.MOBILE_LANDSCAPE.isMobileLandscape)
        assertTrue(DeviceConfiguration.MOBILE_LANDSCAPE_SMALL.isMobileLandscape)
        assertFalse(DeviceConfiguration.MOBILE_PORTRAIT.isMobileLandscape)
        assertFalse(DeviceConfiguration.MOBILE_PORTRAIT_SMALL.isMobileLandscape)
        assertFalse(DeviceConfiguration.TABLET_LANDSCAPE.isMobileLandscape)
    }

    @Test
    fun `isMobilePortrait covers both portrait phone buckets`() {
        assertTrue(DeviceConfiguration.MOBILE_PORTRAIT.isMobilePortrait)
        assertTrue(DeviceConfiguration.MOBILE_PORTRAIT_SMALL.isMobilePortrait)
        assertFalse(DeviceConfiguration.MOBILE_LANDSCAPE.isMobilePortrait)
        assertFalse(DeviceConfiguration.TABLET_PORTRAIT.isMobilePortrait)
    }
}
