package com.k.sekiro.musico.playmusic.presenation.playlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    collapsedToolbarHeight: Dp,
    scrollProgress: Float,
    isBackEnabled: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(collapsedToolbarHeight)
            .background(if (scrollProgress < 1f) Color.Transparent else TopAppBarDefaults.topAppBarColors().containerColor)
            .padding(4.dp)
            .zIndex(2f) // Make sure it's above the scrollable content
            .graphicsLayer {
                alpha = if (scrollProgress < 1f) 0f else 1f
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton(
                onClick = {},
                enabled = isBackEnabled
            ) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}