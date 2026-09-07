package com.k.sekiro.musico.ui.theme

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview: renders a composable at four representative window sizes so a layout can be
 * eyeballed for phone / landscape / foldable / tablet without a device.
 *
 * Wrap the previewed content in [MusiCoTheme] so the responsive [AppDimens] bucket is picked
 * from each preview's width, exactly as it is at runtime.
 */
@Preview(name = "Phone", group = "form factor", widthDp = 360, heightDp = 740, showBackground = true)
@Preview(name = "Landscape", group = "form factor", widthDp = 740, heightDp = 360, showBackground = true)
@Preview(name = "Foldable", group = "form factor", widthDp = 673, heightDp = 841, showBackground = true)
@Preview(name = "Tablet", group = "form factor", widthDp = 1280, heightDp = 800, showBackground = true)
annotation class FormFactorPreviews
