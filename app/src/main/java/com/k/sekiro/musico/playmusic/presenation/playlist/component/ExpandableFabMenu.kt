package com.k.sekiro.musico.playmusic.presenation.playlist.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * 1. Composable for the individual menu items.
 */
@Composable
fun FabMenuItem(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp) // Smaller size for sub-buttons
    ) {
        icon()
    }
}

/**
 * 2. The Custom Layout that positions children in a half-circle.
 * We'll use this layout to place the menu items around the main FAB.
 */
@Composable
fun CircularFabLayout(
    radius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // The Layout composable allows us to manually measure and place its children.
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        // Measure all children
        val placeables: List<Placeable> = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // We assume the first placeable is the main FAB and the rest are the sub-menu items.
        val mainFabPlaceable = placeables.firstOrNull()
        val menuItemsPlaceables = placeables.drop(1)

        val mainFabSize = mainFabPlaceable?.width ?: 0
        val maxItemSize = menuItemsPlaceables.maxOfOrNull { max(it.width, it.height) } ?: 0

        // Use a fixed size for the entire layout based on the main FAB size and radius
        val size = mainFabSize + radius.roundToPx() + maxItemSize

        layout(size, size) {
            // Calculate the position of the main FAB (bottom-right of the layout bounds)
            val mainFabX = size - mainFabSize
            val mainFabY = size - mainFabSize
            mainFabPlaceable?.placeRelative(mainFabX, mainFabY)

            // --- Circular Positioning Logic ---
            val numItems = menuItemsPlaceables.size
            if (numItems > 0) {
                val r = radius.toPx()
                // Define the angle range for the half-circle (180 degrees)
                // We'll go from 180 degrees (left) to 90 degrees (up) for a bottom-right placement
                val startAngle = 180f
                val endAngle = 90f
                val angleSpan = abs(startAngle - endAngle)

                // Calculate the gap between items
                val angleIncrement = angleSpan / (numItems - 1).coerceAtLeast(1)

                menuItemsPlaceables.forEachIndexed { index, placeable ->
                    // Calculate the angle for the current item
                    val angleDegrees = startAngle - (index * angleIncrement)
                    val angleRadians = Math.toRadians(angleDegrees.toDouble())

                    // Polar to Cartesian conversion
                    val xOffset = (r * cos(angleRadians)).toFloat()
                    val yOffset = (r * sin(angleRadians)).toFloat()

                    // The center of the main FAB is the origin for the circle
                    val fabCenterX = mainFabX + mainFabSize / 2f
                    val fabCenterY = mainFabY + mainFabSize / 2f

                    // Item's position (center to center)
                    val itemCenterX = fabCenterX + xOffset
                    val itemCenterY = fabCenterY - yOffset // y-axis is inverted in Compose

                    // Final placement (top-left corner of the item)
                    val itemX = (itemCenterX - placeable.width / 2f).toInt()
                    val itemY = (itemCenterY - placeable.height / 2f).toInt()

                    placeable.placeRelative(itemX, itemY)
                }
            }
        }
    }
}

/**
 * 3. The main expandable FAB composable with animation logic.
 */
@Composable
fun ExpandableFabMenu(
    modifier: Modifier = Modifier,
    radius: Dp = 100.dp,
    menuItems: List<@Composable () -> Unit>,
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Animate the rotation of the main FAB icon (Add <-> Close)
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) -90f else 0f, // Rotate to -45 deg for 'Close' icon
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val rotationFabs by animateFloatAsState(
        targetValue = if (isExpanded) 360f else 0f, // Rotate to -45 deg for 'Close' icon
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    /*    // The Transition is used to stagger the animations of the sub-items
        val transition = updateTransition(targetState = isExpanded, label = "fabMenuTransition")*/

    // Define the animated state for the main FAB's icon
    val mainFabIcon = if (isExpanded) Icons.Filled.Close else Icons.Filled.Add

    // Calculate the total duration for the staggered animation
    val animationDuration = 300 // ms
    val staggerDelay = animationDuration / (menuItems.size + 1) // ms

    // --- Content for the CircularFabLayout ---
    CircularFabLayout(
        radius = radius,
        modifier = modifier
    ) {
        // 1. The Main FAB (always visible)
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
        ) {
            Icon(
                imageVector = mainFabIcon,
                contentDescription = if (isExpanded) "Close menu" else "Expand menu",
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                }
            )
        }

        // 2. The Sub-Menu Items
        menuItems.forEachIndexed { index, itemContent ->
            // Define the animation for each item
            val enterTransition = slideIn(
                initialOffset = { fullSize ->
                    // Start position is from the main FAB's center
                    val mainFabCenter = Offset(fullSize.width.toFloat(), fullSize.height.toFloat())
                    Offset(mainFabCenter.x, mainFabCenter.y).toIntOffset()
                },
                animationSpec = tween(
                    durationMillis = animationDuration,
                    // Stagger the animation using a delay based on the index
                    delayMillis = if (isExpanded) index * staggerDelay else (menuItems.size - 1 - index) * staggerDelay
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = animationDuration,
                    delayMillis = if (isExpanded) index * staggerDelay else (menuItems.size - 1 - index) * staggerDelay
                )
            )

            val exitTransition = slideOut(
                targetOffset = { fullSize ->
                    // End position is back to the main FAB's center
                    val mainFabCenter = Offset(fullSize.width.toFloat(), fullSize.height.toFloat())
                    Offset(mainFabCenter.x, mainFabCenter.y).toIntOffset()
                },
                animationSpec = tween(
                    durationMillis = animationDuration,
                    // Stagger the closing animation in reverse order
                    delayMillis = if (!isExpanded) index * staggerDelay else (menuItems.size - 1 - index) * staggerDelay
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = animationDuration,
                    delayMillis = if (!isExpanded) index * staggerDelay else (menuItems.size - 1 - index) * staggerDelay
                )
            )

            // AnimatedVisibility controls the appearance/disappearance of the item
            AnimatedVisibility(
                visible = isExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                // The item content is provided here
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotationFabs
                    }
                ) {
                    itemContent()
                }
            }
        }
    }
}

private fun Offset.toIntOffset(): IntOffset {
    return IntOffset(x.toInt(), y.toInt())
}

/**
 * Usage Example:
 */
@Preview
@Composable
fun FabMenuScreen() {
    Scaffold(
        floatingActionButton = {
            ExpandableFabMenu(
                menuItems = listOf(
                    { FabMenuItem(icon = { Icon(Icons.Filled.Edit, "Edit") }) { /* Action 1 */ } },
                    { FabMenuItem(icon = { Icon(Icons.Filled.Send, "Send") }) { /* Action 2 */ } },
                    { FabMenuItem(icon = { Icon(Icons.Filled.Add, "New") }) { /* Action 3 */ } }
                )
            )
        },
        content = { padding ->
            // Your screen content goes here
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("Your App Content")
            }
        }
    )
}
