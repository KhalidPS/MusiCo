package com.k.sekiro.musico.playmusic.presenation.playlist

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.model.DeletionType
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.playlist.component.CustomTopBar
import com.k.sekiro.musico.playmusic.presenation.playlist.component.ExpandableFabMenu
import com.k.sekiro.musico.playmusic.presenation.playlist.component.FabMenuItem
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.mockPlaylists
import com.k.sekiro.musico.playmusic.presenation.util.component.DeleteDialog
import com.k.sekiro.musico.playmusic.presenation.util.component.PlaylistSelectionBottomSheet
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.Song
import com.k.sekiro.musico.playmusic.presenation.util.component.AddPlaylistDialog
import com.k.sekiro.musico.playmusic.presenation.util.shareAudioFile
import com.k.sekiro.musico.ui.theme.Red
import com.k.sekiro.musico.ui.theme.Red2
import com.k.sekiro.musico.ui.theme.Red3
import com.k.sekiro.musico.ui.theme.RedVarient

// Assuming you have a drawable resource named 'sample_image'
// For this example, let's use a placeholder.
// You would replace `R.drawable.sample_image` with your actual image resource.
// Create a file called `res/drawable/sample_image.jpg` or use a different image resource.
//import com.your.package.name.R // Replace with your actual package name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCollapsingScreen(
    playlistWithSongsUi: PlaylistWithSongsUi,
    onAction: (UiAction) -> Unit = {},
    onAddToNewPlaylist: (String) -> Unit,
    onAddToExistPlaylist: (Playlist) -> Unit,
    onAddSingleSong: (Long, Playlist?, String?) -> Unit,
    onCancelClicked: () -> Unit,
    playlists: List<Playlist>,
    onBackButtonClicked: () -> Unit = {},
    onSongClicked: (Int, SongUi) -> Unit = { _, _ -> },
    onSelectSong: (SongUi) -> Unit = {},
    selectedSongs: List<SongUi>,
    selectModeEnabled: Boolean,
    onConfirmDeletePlaylist:(Playlist) -> Unit = {}
) {

    BackHandler(selectModeEnabled) {
        onCancelClicked()
    }


    var songToAddFromMenu: SongUi? by remember { mutableStateOf(null) }

    val context = LocalContext.current

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var isShowSheet by remember { mutableStateOf(false) }
    var isShowDeleteDialog by remember { mutableStateOf(false) }
    var isShowAddPlaylistDialog by remember { mutableStateOf(false) }
    var isShowDeletePlaylistDialog by remember { mutableStateOf(false) }


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

    val isBackEnabled by remember { derivedStateOf { scrollProgress >= 1f } }


    // --- Title Animation Properties ---
    val titleFontSizeExpanded = 30.sp
    val titleFontSizeCollapsed = 22.sp
    val titleExpandedHorizontalPadding = 16.dp
    val titleCollapsedHorizontalPadding = 16.dp

    // Calculate the start and end positions for the title's vertical movement
    val titleExpandedY =
        with(density) { (expandedImageHeight / 2).toPx() - (titleFontSizeExpanded.toPx() / 2) }
    val titleCollapsedY =
        with(density) { (collapsedToolbarHeight / 2).toPx() - (titleFontSizeCollapsed.toPx() / 2) }

    // Calculate the start and end positions for the title's horizontal movement
    val titleExpandedX =
        with(density) { (expandedImageHeight / 2).toPx() - (200.dp.toPx() / 2) } // Approximate center
    val iconButtonWidth = 48.dp
    val startPadding = 16.dp
    val titleIconGap = 8.dp // A small gap between the icon and the title
    val titleCollapsedX = with(density) { (startPadding + iconButtonWidth + titleIconGap).toPx() }
    // --- Toolbar Color Animation ---
    /*    val toolbarBackgroundColor by animateColorAsState(
            targetValue =   if (scrollProgress < 1f) Color.Transparent else MaterialTheme.colorScheme.surface
            *//* lerp(
            Color.Transparent,
            MaterialTheme.colorScheme.surface,
            scrollProgress
        )*//*
    )*/

    Box(modifier = Modifier.fillMaxSize()) {

        // --- Collapsing Image ---
        AsyncImage(
            model = if (playlistWithSongsUi.songs.isNotEmpty()) playlistWithSongsUi.songs[0].cover else "",
            error = painterResource(id = R.drawable.logo_2),
            contentDescription = "playlist Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(expandedImageHeight)
                .graphicsLayer {
                    // Parallax effect: image moves up slower than the list
                    translationY = -scrollProgress * (expandedHeightPx - collapsedHeightPx)
                    alpha = if (scrollProgress < 1f) 1f else 0f
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

            itemsIndexed(
                playlistWithSongsUi.songs,
                key = { index, item -> item.id }
            ) { index, song ->
                Song(
                    song = song,
                    onClick = {
                        if (selectModeEnabled) {
                            onSelectSong(song)
                        } else {
                            onSongClicked(index, it)

                        }
                    },
                    onLongClicked = onSelectSong,
                    onShareClick = { context.shareAudioFile(listOf(song.dataUri.toUri())) },
                    onDeleteClicked = {
                        songToAddFromMenu = song
                        isShowDeleteDialog = true
                    },
                    onAddToPlaylistClicked = {
                        songToAddFromMenu = song
                        isShowSheet = true
                    },
                    selectedSongs = selectedSongs,
                    selectModeEnabled = selectModeEnabled
                )
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
                    translationY = lerp(
                        titleExpandedY,
                        titleCollapsedY,
                        scrollProgress
                    )

                    // Animate the horizontal position from center to start
                    translationX = lerp(
                        if (isRtl) -titleExpandedX else titleExpandedX,
                        if (isRtl) -titleCollapsedX else titleCollapsedX,
                        scrollProgress
                    )

                    // Optional: Fade out the title as it gets smaller
                    // alpha = 1f - scrollProgress
                }
                .zIndex(3f) // Ensure title is on top
                .background(if (scrollProgress < 1) Color.LightGray.copy(.3f) else Color.Unspecified)
        )

        // --- TopAppBar (The final, fixed toolbar) ---

        CustomTopBar(
            collapsedToolbarHeight = collapsedToolbarHeight,
            scrollProgress = scrollProgress,
            isBackEnabled = isBackEnabled
        )

        
        IconButton(
            onClick = { isShowDeletePlaylistDialog = true },
            modifier = Modifier.zIndex(if (scrollProgress < 1f) 5f else 0f)
        ) { 
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Red
            )
        }


        if (selectModeEnabled) {
            ExpandableFabMenu(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                menuItems = listOf(
                    {
                        FabMenuItem(icon = {
                            Icon(
                                Icons.AutoMirrored.Default.PlaylistAdd,
                                "Add to Playlist"
                            )
                        }) { isShowSheet = true }
                    },
                    {
                        FabMenuItem(icon = {
                            Icon(
                                Icons.Filled.Delete,
                                "Delete"
                            )
                        }) { isShowDeleteDialog = true }
                    },
                    {
                        FabMenuItem(icon = {
                            Icon(
                                Icons.Filled.Cancel,
                                "Cancel"
                            )
                        }, onClick = onCancelClicked)
                    }
                )
            )
        }


    }


    if (isShowSheet) {
        PlaylistSelectionBottomSheet(
            onConfirm = {
                Log.e("ks", "songToAddFromMenu : $songToAddFromMenu")
                if (selectModeEnabled) {
                    onAddToExistPlaylist(it)
                } else if (songToAddFromMenu != null) {
                    onAddSingleSong(songToAddFromMenu!!.id, it, null)
                }
            },
            onDismiss = { isShowSheet = false },
            onAddPlaylist = { isShowAddPlaylistDialog = true },
            playlists = playlists
        )
    }


    AddPlaylistDialog(
        isShowDialog = isShowAddPlaylistDialog,
        playlists = playlists,
        onAddPlaylistClicked = {
            isShowAddPlaylistDialog = false
            isShowSheet = false
            if (selectModeEnabled) {
                onAddToNewPlaylist(it)
            } else if (songToAddFromMenu != null) {
                onAddSingleSong(songToAddFromMenu!!.id, null, it)
            }
        },
        onCancelClicked = { isShowAddPlaylistDialog = false }
    )


    if (isShowDeleteDialog) {

        var radioValue by remember { mutableIntStateOf(1) }

        DeleteDialog(
            onConfirm = {
                if (radioValue == 1) {
                    if (selectModeEnabled) {
                        onAction(
                            UiAction.DeletionConfirmClicked(
                                DeletionType.PlaylistDeletion(
                                    playlistWithSongsUi.playlist.id
                                )
                            )
                        )
                    } else if (songToAddFromMenu != null) {
                        Log.e("ks","songToAddFromMenu : $songToAddFromMenu")
                        onAction(
                            UiAction.DeletionConfirmClicked(
                                DeletionType.PlaylistDeletion(
                                    playlistWithSongsUi.playlist.id,
                                    songToAddFromMenu
                                )
                            )
                        )
                    }
                } else {
                    if (selectModeEnabled) {
                        onAction(UiAction.DeletionConfirmClicked(DeletionType.StorageDeletion()))
                    } else if (songToAddFromMenu != null) {
                        onAction(
                            UiAction.DeletionConfirmClicked(
                                DeletionType.StorageDeletion(
                                    songToAddFromMenu
                                )
                            )
                        )

                    }
                }

                isShowDeleteDialog = false
            },
            onIgnore = { isShowDeleteDialog = false },
            onDismissRequest = { isShowDeleteDialog = false },
            additionalContent = {

                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = radioValue == 1,
                            onClick = { radioValue = 1 }
                        )

                        Text("From Playlist")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = radioValue == 2,
                            onClick = { radioValue = 2 }
                        )

                        Text("Permanently")
                    }


                }
            }
        )

    }


    if (isShowDeletePlaylistDialog){
        DeleteDialog(
            title = "Delete Playlist",
            description = "Are you sure you that you want delete this Playlist?",
            onConfirm = {
                onConfirmDeletePlaylist(playlistWithSongsUi.playlist)
                isShowDeletePlaylistDialog = false
            },
            onIgnore = { isShowDeletePlaylistDialog = false },
            onDismissRequest = { isShowDeletePlaylistDialog = false }
        )
    }


}

@Preview
@Composable
private fun CollapsingTitleToolbarPrev() {
    PlaylistCollapsingScreen(
        playlistWithSongsUi = mockPlaylists[0],
        selectedSongs = mockSongs.map { it.toSongUi() },
        selectModeEnabled = false,
        playlists = emptyList(),
        onCancelClicked = {},
        onAddToExistPlaylist = {},
        onAddToNewPlaylist = {},
        onAddSingleSong = { _, _, _ -> }
    )
}