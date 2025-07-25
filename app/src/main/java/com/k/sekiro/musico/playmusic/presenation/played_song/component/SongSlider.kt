package com.k.sekiro.musico.playmusic.presenation.played_song.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.k.sekiro.musico.playmusic.presenation.UiAction

@Composable
fun SongSlider(
    sliderProgress:() -> Float,
    onAction:(UiAction) -> Unit,
    outlineColor: Color,
) {

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }
    Slider(
        value = if (isSliderDragging) sliderValue else sliderProgress(),
        onValueChange = {
            isSliderDragging = true
            sliderValue = it
            onAction(UiAction.UpdateProgress(it))
        },
        onValueChangeFinished = {
            isSliderDragging = false
            onAction(UiAction.SeekTo(sliderValue))

        },
        valueRange = 0f..100f,
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(
            activeTrackColor = outlineColor,
            thumbColor = outlineColor
        )
    )
}