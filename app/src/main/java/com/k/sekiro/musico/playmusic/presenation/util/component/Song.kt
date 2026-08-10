package com.k.sekiro.musico.playmusic.presenation.util.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.util.Constants
import com.k.sekiro.musico.playmusic.presenation.util.applyIfComposable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Song(
    modifier: Modifier = Modifier,
    song: SongUi,
    onClick:(SongUi) -> Unit = {},
    onLongClicked:(SongUi) -> Unit = {},
    onShareClick:() -> Unit = {},
    onDeleteClicked: () -> Unit = {},
    onAddToPlaylistClicked:() -> Unit = {},
    selectModeEnabled: Boolean = false,
    showMoreIcon: Boolean = true,
    selectedSongs: List<SongUi> = emptyList(),
    // Both must be provided to enable the shared-element transition into PlayedSongScreen.
    // Left null for call sites (e.g. search results) where the same song may already be
    // rendered elsewhere with the same key, which SharedTransitionScope doesn't allow.
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isSharedElementEnabled = sharedTransitionScope != null && animatedVisibilityScope != null

    val artist =
        if (song.artist.isBlank() || song.artist.isEmpty()) "Unknown artist" else song.artist
    val album = if (song.album.isBlank() || song.album.isEmpty()) "Unknown album" else song.album
    val isSelected by remember(selectedSongs) { derivedStateOf { selectedSongs.contains(song) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = {
                    onClick(song)
                },
                onLongClick =   {
                    Log.e("ks","long clicked")
                    Log.e("ks","in long enabled $selectModeEnabled")
                    if (!selectModeEnabled){
                        Log.e("ks","inside if")
                        onLongClicked(song)
                    }
                }
            )
            .background(if (isSelected) Color.Gray else Color.Unspecified)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
       // val song = song.toSongUi(context.contentResolver,context.resources)

        trace("song Image"){
            AsyncImage(
                model = song.cover,
                error = painterResource(R.drawable.logo_musico3),
                placeholder = painterResource(R.drawable.logo_musico3),

                // bitmap = songCover.asImageBitmap(),
                contentDescription = "song album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .applyIfComposable(isSharedElementEnabled) {
                        with(sharedTransitionScope!!) {
                            sharedBounds(
                                sharedContentState = rememberSharedContentState("${Constants.LIST_IMAGE_KEY}_${song.path}"),
                                animatedVisibilityScope = animatedVisibilityScope!!,
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                            )
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .size(70.dp)

            )
        }


        Column(
            modifier = modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.title,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .applyIfComposable(isSharedElementEnabled) {
                        with(sharedTransitionScope!!) {
                            sharedBounds(
                                sharedContentState = rememberSharedContentState("${Constants.LIST_TITLE_KEY}_${song.path}"),
                                animatedVisibilityScope = animatedVisibilityScope!!,
                            )
                        }
                    }
                    .padding(bottom = 8.dp),
                maxLines = 1,

            )
            Text(
                text = "$artist | $album",
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier
                    .applyIfComposable(isSharedElementEnabled) {
                        with(sharedTransitionScope!!) {
                            sharedBounds(
                                sharedContentState = rememberSharedContentState("${Constants.LIST_ARTIST_KEY}_${song.path}"),
                                animatedVisibilityScope = animatedVisibilityScope!!,
                            )
                        }
                    }

            )
        }



        Box {

            var isExpanded by remember { mutableStateOf(false) }


            if (showMoreIcon){
                IconButton(
                    onClick = {isExpanded = !isExpanded},
                    enabled = !selectModeEnabled
                ) {


                    Icon(
                        imageVector = Icons.TwoTone.MoreVert,
                        contentDescription = "more action to do for song icon"
                    )
                }
            }

            OptionMenu(
                isExpanded = isExpanded,
                onShareClicked = onShareClick,
                onDeleteClicked = onDeleteClicked,
                onAddToPlaylistClicked = onAddToPlaylistClicked,
                onDismissRequest = { isExpanded = false }
            )
        }

    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun SongPrev() {
    SharedTransitionLayout {
        AnimatedVisibility(true) {
            Song(
                song = mockSongs[1].toSongUi(),
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@AnimatedVisibility
            )
        }
    }
}