package com.k.sekiro.musico.core.presentaion.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp


fun Modifier.applyIf(
    condition: Boolean,
    modifier: Modifier.()-> Modifier
): Modifier{
    return then(
        if (condition){
            modifier()
        }else{
            Modifier
        }
    )
}


fun Dp.toPx(density: Float) = this.value * density

