package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.k.sekiro.musico.R
import com.k.sekiro.musico.ui.theme.Blue
import com.k.sekiro.musico.ui.theme.FavoritePlaylistColor
import com.k.sekiro.musico.ui.theme.RecentPlayListColor
import androidx.compose.material.icons.twotone.Refresh

/** A "shelf card" style quick-access tile for Home: cover art fills the tile, a bottom scrim
keeps the title/subtitle legible over any artwork, and a small accent-colored badge carries the
icon that used to be the tile's whole background tint - so the artwork gets to be the focus
instead of a flat color block.**/
@Composable
fun PlaylistShelfCard(
    modifier: Modifier = Modifier,
    accentColor: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    coverUrl: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shelfCardScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = coverUrl,
            error = painterResource(R.drawable.logo_musico3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Preview
@Composable
private fun PlaylistShelfCardPrev() {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        PlaylistShelfCard(
            modifier = Modifier.size(120.dp, 140.dp),
            accentColor = FavoritePlaylistColor,
            icon = Icons.Default.Favorite,
            title = "Favorite",
            subtitle = "24 songs",
            coverUrl = "",
            onClick = {}
        )
        PlaylistShelfCard(
            modifier = Modifier.size(120.dp, 140.dp),
            accentColor = Blue,
            icon = Icons.AutoMirrored.Default.List,
            title = "Playlists",
            subtitle = "6 playlists",
            coverUrl = "",
            onClick = {}
        )
        PlaylistShelfCard(
            modifier = Modifier.size(120.dp, 140.dp),
            accentColor = RecentPlayListColor,
            icon = Icons.TwoTone.Refresh,
            title = "Recent",
            subtitle = "12 songs",
            coverUrl = "",
            onClick = {}
        )
    }
}
