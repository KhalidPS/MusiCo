package com.k.sekiro.musico.ui.theme

enum class WindowSize{
    SmallCompact,
    Compact,
    Medium,
    Expanded
}



fun calculateWindowSizeWidth(width: Int): WindowSize{
    return when{
        width<=360 -> WindowSize.SmallCompact
        width < 600 -> WindowSize.Compact
        width < 840 -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}