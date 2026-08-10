package com.k.sekiro.musico.playmusic.presenation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.playlist.component.CustomTopBar
import com.k.sekiro.musico.playmusic.presenation.playlist.component.ExpandableFabMenu
import com.k.sekiro.musico.playmusic.presenation.playlist.component.FabMenuItem
import com.k.sekiro.musico.playmusic.presenation.util.component.AddPlaylistDialog
import com.k.sekiro.musico.playmusic.presenation.util.component.DeleteDialog
import com.k.sekiro.musico.playmusic.presenation.util.component.PlaylistSelectionBottomSheet
import com.k.sekiro.musico.playmusic.presenation.util.component.Song
import com.k.sekiro.musico.playmusic.presenation.util.shareAudioFile

/** Read-only counterpart to PlaylistCollapsingScreen for a derived group (artist/album) -
same collapsing-header + song-list shell, but with no whole-entity delete concept, since a
browse group isn't a persisted Playlist and can't be "deleted" itself. Per-song actions
(add to playlist, delete from storage, share, select-mode bulk add/delete) are unchanged.**/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.BrowseDetailScreen(
    title: String,
    coverUrl: String,
    songs: List<SongUi>,
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
    animatedVisibilityScope: AnimatedVisibilityScope,
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

    val expandedImageHeight = 250.dp
    val collapsedToolbarHeight = 60.dp

    val density = LocalDensity.current
    val expandedHeightPx = density.run { expandedImageHeight.toPx() }
    val collapsedHeightPx = density.run { collapsedToolbarHeight.toPx() }

    val scrollState = rememberLazyListState()

    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
                val fullScrollRange = expandedHeightPx - collapsedHeightPx
                (scrollOffset / fullScrollRange).coerceIn(0f, 1f)
            }
        }
    }

    val isBackEnabled by remember { derivedStateOf { scrollProgress >= 1f } }

    val titleFontSizeExpanded = 30.sp
    val titleFontSizeCollapsed = 22.sp

    val titleExpandedY =
        with(density) { (expandedImageHeight / 2).toPx() - (titleFontSizeExpanded.toPx() / 2) }
    val titleCollapsedY =
        with(density) { (collapsedToolbarHeight / 2).toPx() - (titleFontSizeCollapsed.toPx() / 2) }

    val titleExpandedX =
        with(density) { (expandedImageHeight / 2).toPx() - (200.dp.toPx() / 2) }
    val iconButtonWidth = 48.dp
    val startPadding = 16.dp
    val titleIconGap = 8.dp
    val titleCollapsedX = with(density) { (startPadding + iconButtonWidth + titleIconGap).toPx() }

    Box(modifier = Modifier.fillMaxSize()) {

        AsyncImage(
            model = coverUrl,
            error = painterResource(id = R.drawable.logo_2),
            contentDescription = "$title cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(expandedImageHeight)
                .graphicsLayer {
                    translationY = -scrollProgress * (expandedHeightPx - collapsedHeightPx)
                    alpha = if (scrollProgress < 1f) 1f else 0f
                }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
        ) {
            item {
                Spacer(modifier = Modifier.height(expandedImageHeight))
            }

            itemsIndexed(
                songs,
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
                    onShareClick = { context.shareAudioFile(song.dataUri.toUri()) },
                    onDeleteClicked = {
                        songToAddFromMenu = song
                        isShowDeleteDialog = true
                    },
                    onAddToPlaylistClicked = {
                        songToAddFromMenu = song
                        isShowSheet = true
                    },
                    selectedSongs = selectedSongs,
                    selectModeEnabled = selectModeEnabled,
                    sharedTransitionScope = this@BrowseDetailScreen,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        Text(
            text = title,
            fontSize = lerp(titleFontSizeExpanded, titleFontSizeCollapsed, scrollProgress),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .graphicsLayer {
                    translationY = lerp(titleExpandedY, titleCollapsedY, scrollProgress)
                    translationX = lerp(
                        if (isRtl) -titleExpandedX else titleExpandedX,
                        if (isRtl) -titleCollapsedX else titleCollapsedX,
                        scrollProgress
                    )
                }
                .zIndex(3f)
                .background(
                    if (scrollProgress < 1) Color.LightGray.copy(.3f)
                    else Color.Unspecified
                )
        )

        CustomTopBar(
            collapsedToolbarHeight = collapsedToolbarHeight,
            scrollProgress = scrollProgress,
            isBackEnabled = isBackEnabled,
            onBackButtonClicked = onBackButtonClicked
        )

        if (selectModeEnabled) {
            ExpandableFabMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
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
        DeleteDialog(
            onConfirm = {
                if (selectModeEnabled) {
                    onAction(UiAction.DeletionConfirmClicked(DeletionType.StorageDeletion()))
                } else if (songToAddFromMenu != null) {
                    onAction(
                        UiAction.DeletionConfirmClicked(
                            DeletionType.StorageDeletion(songToAddFromMenu)
                        )
                    )
                }
                isShowDeleteDialog = false
            },
            onIgnore = { isShowDeleteDialog = false },
            onDismissRequest = { isShowDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun BrowseDetailScreenPrev() {
    SharedTransitionLayout {
        AnimatedVisibility(true) {
            BrowseDetailScreen(
                title = "Ajnad Nasheed",
                coverUrl = "",
                songs = mockSongs.map { it.toSongUi() },
                selectedSongs = emptyList(),
                selectModeEnabled = false,
                playlists = emptyList(),
                onCancelClicked = {},
                onAddToExistPlaylist = {},
                onAddToNewPlaylist = {},
                onAddSingleSong = { _, _, _ -> },
                animatedVisibilityScope = this
            )
        }
    }
}
