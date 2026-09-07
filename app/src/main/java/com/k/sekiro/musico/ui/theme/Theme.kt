package com.k.sekiro.musico.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Responsive dimension tokens for the current window width. Selected once here, at the theme
 * root, and consumed everywhere else via [appDimens] - screens must not recompute the window
 * size themselves.
 */
val LocalAppDimens = staticCompositionLocalOf { CompactDimens }

/** Shorthand for `LocalAppDimens.current`. */
val appDimens: AppDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimens.current

/**
 * The device/orientation bucket - read by screens that need to switch layout (not just scale
 * dimens), e.g. a phone in landscape switching to a side-by-side arrangement. Backed by the
 * official `androidx.window` [androidx.window.core.layout.WindowSizeClass] (via
 * [currentWindowAdaptiveInfoV2]) rather than a hand-rolled breakpoint check, so it stays correct
 * as the platform's own size-class definitions evolve. See [DeviceConfiguration].
 */
val LocalDeviceConfiguration = staticCompositionLocalOf { DeviceConfiguration.MOBILE_PORTRAIT }

/** Shorthand for `LocalDeviceConfiguration.current`. */
val deviceConfiguration: DeviceConfiguration
    @Composable
    @ReadOnlyComposable
    get() = LocalDeviceConfiguration.current

@Composable
fun MusiCoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // The window size class already reflects orientation and foldable posture, and this
    // recomposes on any configuration change, so the bucket stays current.
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val config = remember(windowSizeClass) { DeviceConfiguration.fromWindowSizeClass(windowSizeClass) }
    val dimens = remember(config) { dimensFor(config) }

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalDeviceConfiguration provides config,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
