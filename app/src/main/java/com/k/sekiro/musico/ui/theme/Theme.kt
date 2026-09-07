package com.k.sekiro.musico.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
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
 * The window's height bucket - read by screens that need to switch layout (not just scale
 * dimens) on a short window, e.g. a phone in landscape. See [WindowHeightSize].
 */
val LocalWindowHeightSize = staticCompositionLocalOf { WindowHeightSize.Medium }

/** Shorthand for `LocalWindowHeightSize.current`. */
val windowHeightSize: WindowHeightSize
    @Composable
    @ReadOnlyComposable
    get() = LocalWindowHeightSize.current

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

    // screenWidthDp/screenHeightDp already reflect orientation and foldable posture, and this
    // recomposes on any configuration change, so both buckets stay current without a
    // window-size library.
    val widthDp = LocalConfiguration.current.screenWidthDp
    val heightDp = LocalConfiguration.current.screenHeightDp
    val dimens = remember(widthDp) { dimensFor(windowWidthSizeOf(widthDp)) }
    val heightSize = remember(heightDp) { windowHeightSizeOf(heightDp) }

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalWindowHeightSize provides heightSize,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
