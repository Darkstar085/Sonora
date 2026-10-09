package com.sipun.sonora.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
internal fun VinylVisual() {
    Box(
        modifier = Modifier
            .size(280.dp)
            .shadow(28.dp, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF3A1114), Color(0xFF170F10)),
                ),
                radius = radius * 1.25f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF252326), Color(0xFF080808)),
                ),
                radius = radius,
            )
            repeat(8) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.055f),
                    radius = radius * (0.22f + index * 0.09f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
            drawCircle(color = SonoraRed, radius = radius * 0.27f)
            drawCircle(color = Color(0xFF171214), radius = radius * 0.07f)
        }
        Box(
            modifier = Modifier
                .size(280.dp)
                .border(1.dp, SonoraRed.copy(alpha = 0.35f), CircleShape),
        )
    }
}

@Composable
internal fun LibraryVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        LibraryCard(
            modifier = Modifier
                .width(230.dp)
                .height(190.dp)
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            title = "Artists",
            icon = Icons.Rounded.MusicNote,
        )
        LibraryCard(
            modifier = Modifier
                .width(250.dp)
                .height(210.dp)
                .align(Alignment.Center),
            title = "Albums",
            icon = Icons.Rounded.Album,
            front = true,
        )
        LibraryCard(
            modifier = Modifier
                .width(210.dp)
                .height(180.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            title = "Playlists",
            icon = Icons.Rounded.LibraryMusic,
        )
    }
}

@Composable
internal fun LibraryCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    front: Boolean = false,
) {
    Surface(
        modifier = modifier
            .shadow(if (front) 22.dp else 8.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp)),
        color = if (front) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (front) 5.dp else 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = SonoraRed)
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtworkTile(Color(0xFF7E1F28))
                ArtworkTile(Color(0xFF283E61))
                ArtworkTile(Color(0xFF4E4730))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtworkTile(Color(0xFF3E5B54))
                ArtworkTile(Color(0xFF6C384D))
                ArtworkTile(Color(0xFF2D2D35))
            }
        }
    }
}

@Composable
internal fun ArtworkTile(color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(color, color.copy(alpha = 0.45f), Color(0xFF171717)),
                ),
            ),
    )
}

@Composable
internal fun PermissionVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(38.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SonoraRed.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SonoraRed,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(SonoraRed.copy(alpha = 0.18f))
                .border(1.dp, SonoraRed.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
