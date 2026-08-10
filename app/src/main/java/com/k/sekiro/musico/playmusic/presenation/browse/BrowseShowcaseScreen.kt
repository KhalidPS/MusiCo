package com.k.sekiro.musico.playmusic.presenation.browse

import androidx.collection.LruCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.presenation.browse.components.BrowseGroupItem
import com.k.sekiro.musico.playmusic.presenation.browse.model.BrowseGroupUi

@Composable
fun BrowseShowcaseScreen(
    modifier: Modifier = Modifier,
    title: String,
    groups: List<BrowseGroupUi>,
    lruCache: LruCache<String, Palette>,
    onBackButtonClicked: () -> Unit = {},
    onGroupClicked: (key: String) -> Unit = {}
) {

    Column(
        modifier = modifier.fillMaxSize(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBackButtonClicked,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBackIos,
                    contentDescription = "back button",
                )
            }

            Text(
                title,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )

            // Balances the back button so the title stays centered - unlike playlists,
            // browse groups are derived, not user-creatable, so there's no "add" action.
            IconButton(onClick = {}, enabled = false) {}
        }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                groups,
                key = { it.key }
            ) { group ->
                BrowseGroupItem(
                    title = group.title,
                    songCount = group.songs.size,
                    coverUrl = group.songs.firstOrNull()?.cover.orEmpty(),
                    cacheKey = group.songs.firstOrNull()?.path.orEmpty(),
                    lruCache = lruCache,
                    onClick = { onGroupClicked(group.key) }
                )
            }
        }
    }
}
