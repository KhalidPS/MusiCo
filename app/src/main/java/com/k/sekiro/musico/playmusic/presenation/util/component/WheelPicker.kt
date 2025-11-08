package com.k.sekiro.musico.playmusic.presenation.util.component

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomWheelPicker(
    modifier: Modifier = Modifier,
    items: List<Playlist>,
    onItemSelected: (Playlist) -> Unit,
    itemHeight: Dp = 48.dp,
    visibleItemsCount: Int = 3,
) {
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    val density = LocalDensity.current

    val contentPadding =
        with(density) { (itemHeight * visibleItemsCount / 2 - itemHeight / 2).toPx() }
    var selectedItemIndex by remember { mutableIntStateOf(0) }


    LaunchedEffect(lazyListState) {
        // This flow now only collects once a scroll has definitively stopped.
        snapshotFlow { lazyListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (!isScrolling) {

                    val index = (lazyListState.firstVisibleItemIndex).coerceAtLeast(0)

                    Log.e("ks", "Calculated index: $index, Item: ${items[index]},")

                    if (index in items.indices) {
                        selectedItemIndex = index
                        onItemSelected(items[index])
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemsCount)
    ) {
        LazyColumn(
            state = lazyListState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = with(density) { contentPadding.toDp() })
        ) {
            items(items.size) { index ->

                val scale by animateFloatAsState(
                    targetValue = if (selectedItemIndex == index) 1f else 0.7f,
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = LinearEasing

                    )/*spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    )*/
                )

                val alpha by animateFloatAsState(
                    targetValue = if (selectedItemIndex == index) 1f else 0.5f,
                    animationSpec = tween()
                )


                /*  val distanceFromCenter by remember(selectedItemIndex) {
                      derivedStateOf { (index - selectedItemIndex).absoluteValue }
                  }
                  val receiver = remember(distanceFromCenter){ 1f - distanceFromCenter * 0.3f }

                  val scale by remember (receiver){
                      derivedStateOf {
                          receiver.coerceIn(0.7f, 1f)
                      }
                  }

                  val alpha by remember (receiver){
                      derivedStateOf {
                          receiver.coerceIn(0.5f, 1f)
                      }
                  }*/

                Text(
                    text = items[index].name,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .scale(scale)
                        .alpha(alpha)
                        .padding(top = (itemHeight - Dp(24.sp.value)) / 2)
                )
            }
        }

        // Highlight box for the selected item
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(Color.Gray.copy(alpha = 0.1f))
        )
    }
}