package com.sipun.sonora.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun WelcomeScreen(
    onRequestMusicAccess: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "welcome_page",
                modifier = Modifier.fillMaxSize(),
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage(
                        brand = true,
                        title = "Your music.\nYour way.",
                        description = "A beautiful music player built for your local library.",
                        visual = { VinylVisual() },
                        button = "Get Started",
                        onClick = { page = 1 },
                        page = page,
                    )

                    1 -> WelcomePage(
                        title = "Your library,\nbeautifully organized",
                        description = "Browse albums, artists, playlists and every song on your device.",
                        visual = { LibraryVisual() },
                        button = "Continue",
                        onClick = { page = 2 },
                        page = page,
                    )

                    else -> WelcomePage(
                        title = "Let’s find your music",
                        description = "Sonora needs access to your music library, photos, and videos to organize your media.",
                        visual = { PermissionVisual() },
                        button = "Allow Music Access",
                        onClick = onRequestMusicAccess,
                        privacy = true,
                        page = page,
                    )
                }
            }

        }
    }
}

@Composable
private fun WelcomePage(
    brand: Boolean = false,
    title: String,
    description: String,
    visual: @Composable () -> Unit,
    button: String,
    onClick: () -> Unit,
    privacy: Boolean = false,
    page: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            visual()
        }

        if (brand) {
            Text(
                text = "Sonora",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Text(
            text = description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (privacy) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SonoraRed.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = SonoraRed,
                        )
                    }
                    Column {
                        Text(
                            "Your music stays on your device.",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Sonora only accesses media stored on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == page) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == page) SonoraRed
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SonoraRed,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = button,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VinylVisual() {
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
private fun LibraryVisual() {
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
private fun LibraryCard(
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
private fun ArtworkTile(color: Color) {
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
private fun PermissionVisual() {
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
