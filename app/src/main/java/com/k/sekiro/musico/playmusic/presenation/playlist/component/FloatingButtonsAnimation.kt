package com.k.sekiro.musico.playmusic.presenation.playlist.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.More
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.google.common.primitives.Ints
import kotlinx.coroutines.launch


@Composable
fun BoxScope.FloatingButtonsAnimation(
    onCancelClicked:() -> Unit,
    onDeleteClicked:() -> Unit,

) {
    BoxWithConstraints(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()

    ) {

        //this.maxWidth.value.toInt()+ size.width,0

        var isShown by remember { mutableStateOf(false) }
        var intSize by remember { mutableStateOf(IntSize(0, 0)) }

                val playlistButtonX by animateIntAsState(if (isShown)intSize.width * 2 else this.maxWidth.value.toInt() + intSize.width,)
                val playlistButtonY by animateIntAsState(if (isShown)intSize.height * -1 else 0,)
                val cancelButtonX by animateIntAsState(if (isShown)this.maxWidth.value.toInt() + intSize.width else this.maxWidth.value.toInt() + intSize.width,)
                val cancelButtonY by animateIntAsState(if (isShown)intSize.height * -2 else 0,)
                val deleteButtonX by animateIntAsState(if (isShown)intSize.width * 4 +(intSize.width/4) else this.maxWidth.value.toInt() + intSize.width,)
                val deleteButtonY by animateIntAsState(if (isShown)intSize.height * -1 else 0,)


        val playlistButtonRotate by animateFloatAsState(
            targetValue = if (isShown) 360f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        val cancelButtonRotate by animateFloatAsState(
            targetValue = if (isShown) 360f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )

        )

        val deleteButtonRotate by animateFloatAsState(
            targetValue = if (isShown) 360f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )

        )



        FloatingActionButton(
            onClick = { isShown = !isShown },
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.More,
                contentDescription = null
            )
        }

            FloatingActionButton(
                onClick = { },
                modifier = Modifier
                    .rotate(playlistButtonRotate)
                    .align { size, space, layoutDirection ->
                        intSize = size
                        //IntOffset(size.width * 2, size.height * -1)
                        IntOffset(playlistButtonX, playlistButtonY)
                    },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null
                )
            }



            FloatingActionButton(
                onClick = { },
                modifier = Modifier
                    .rotate(deleteButtonRotate)
                    .align { size, space, layoutDirection ->
                        // IntOffset(size.width * 4 +(size.width/4),size.height * -1)
                        IntOffset(deleteButtonX, deleteButtonY)
                    },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            }

            FloatingActionButton(
                onClick = { },
                modifier = Modifier
                    .rotate(cancelButtonRotate)
                    .align { size, space, layoutDirection ->
                        IntOffset(cancelButtonX, cancelButtonY)
                    },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null
                )
            }




    }
}