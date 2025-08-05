package com.k.sekiro.musico.playmusic.presenation.playlist

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.mockPlaylists
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.Song

// Assuming you have a drawable resource named 'sample_image'
// For this example, let's use a placeholder.
// You would replace `R.drawable.sample_image` with your actual image resource.
// Create a file called `res/drawable/sample_image.jpg` or use a different image resource.
//import com.your.package.name.R // Replace with your actual package name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCollapsingScreen(
    playlistWithSongsUi: PlaylistWithSongsUi,
    onAction:(UiAction) -> Unit = {}
) {

    // --- Define Dimensions ---
    val expandedImageHeight = 250.dp
    val collapsedToolbarHeight = 60.dp

    // Density provides pixel values for calculations
    val density = LocalDensity.current
    val expandedHeightPx = density.run { expandedImageHeight.toPx() }
    val collapsedHeightPx = density.run { collapsedToolbarHeight.toPx() }

    // --- Scroll State & Progress Calculation ---
    val scrollState = rememberLazyListState()

    val scrollProgress by remember {
        derivedStateOf {
            // Check if the first item (our dummy Spacer) is still fully visible
            if (scrollState.firstVisibleItemIndex > 0) {
                // If it's scrolled out of view, the progress is 1f and stays there.
                1f
            } else {
                // Otherwise, calculate the progress based on the scroll offset of the first item
                val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
                val fullScrollRange = expandedHeightPx - collapsedHeightPx
                val progress = (scrollOffset / fullScrollRange).coerceIn(0f, 1f)
                Log.d("ks", "Scroll Progress: $progress") // <-- DEBUG LOG
                progress
            }
        }
    }


    // --- Title Animation Properties ---
    val titleFontSizeExpanded = 30.sp
    val titleFontSizeCollapsed = 22.sp
    val titleExpandedHorizontalPadding = 16.dp
    val titleCollapsedHorizontalPadding = 16.dp

    // Calculate the start and end positions for the title's vertical movement
    val titleExpandedY = with(density) { (expandedImageHeight / 2).toPx() - (titleFontSizeExpanded.toPx() / 2) }
    val titleCollapsedY = with(density) { (collapsedToolbarHeight / 2).toPx() - (titleFontSizeCollapsed.toPx() / 2) }

    // Calculate the start and end positions for the title's horizontal movement
    val titleExpandedX = with(density) { (expandedImageHeight / 2).toPx() - (200.dp.toPx() / 2) } // Approximate center
    val iconButtonWidth = 48.dp
    val startPadding = 16.dp
    val titleIconGap = 8.dp // A small gap between the icon and the title
    val titleCollapsedX = with(density) { (startPadding + iconButtonWidth + titleIconGap).toPx() }
    // --- Toolbar Color Animation ---
    val toolbarBackgroundColor by animateColorAsState(
        targetValue =   if (scrollProgress < 1f) Color.Transparent else MaterialTheme.colorScheme.surface
        /* lerp(
            Color.Transparent,
            MaterialTheme.colorScheme.surface,
            scrollProgress
        )*/
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // --- Collapsing Image ---
        AsyncImage(
            model = if (playlistWithSongsUi.songs.isNotEmpty())playlistWithSongsUi.songs[0].cover else "",
            error = painterResource(id = R.drawable.logo_2),
            contentDescription = "playlist Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(expandedImageHeight)
                .graphicsLayer {
                    // Parallax effect: image moves up slower than the list
                    translationY = -scrollProgress * (expandedHeightPx - collapsedHeightPx)
                }
        )

        // --- Scrollable Content (LazyColumn) ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
        ) {
            item {
                Spacer(modifier = Modifier.height(expandedImageHeight))
            }

            items(playlistWithSongsUi.songs) { song ->
                Song(song = song)
            }
        }

        // --- Custom Animated Title ---
        Text(
            text = playlistWithSongsUi.playlist.name,
            fontSize = lerp(titleFontSizeExpanded, titleFontSizeCollapsed, scrollProgress),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .graphicsLayer {
                    // Animate the vertical position of the title
                    translationY = androidx.compose.ui.util.lerp(
                        titleExpandedY,
                        titleCollapsedY,
                        scrollProgress
                    )

                    // Animate the horizontal position from center to start
                    translationX = androidx.compose.ui.util.lerp(
                        titleExpandedX,
                        titleCollapsedX,
                        scrollProgress
                    )

                    // Optional: Fade out the title as it gets smaller
                    // alpha = 1f - scrollProgress
                }
                .zIndex(3f) // Ensure title is on top
                .background( if(scrollProgress < 1) Color.LightGray.copy(.3f) else Color.Unspecified)
        )

        // --- TopAppBar (The final, fixed toolbar) ---
        TopAppBar(
            title = { }, // Title is handled by the animated Text composable above
            navigationIcon = {
                IconButton(
                    onClick = { /* Handle back click */ },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = toolbarBackgroundColor,
                scrolledContainerColor = toolbarBackgroundColor
            ),
            modifier = Modifier
                .height(collapsedToolbarHeight)
                .zIndex(2f) // Make sure it's above the scrollable content
        )
    }
}

@Preview
@Composable
private fun CollapsingTitleToolbarPrev() {
    PlaylistCollapsingScreen(playlistWithSongsUi = mockPlaylists[0])
}