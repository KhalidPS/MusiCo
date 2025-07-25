package com.k.sekiro.musico.playmusic.presenation.played_song.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun PassedTimeText(
    modifier: Modifier = Modifier,
    passedTime:() -> String,
    color: Color
) {

    Text(text = passedTime() , color = color)


}