package com.sipun.sonora.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sipun.sonora.ui.components.SonoraSearchBar
import com.sipun.sonora.ui.theme.SonoraRed

private val tabs = listOf("Overview", "Songs", "Albums", "Artists")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNowPlaying: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Listening",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SonoraSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                divider = {},
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab) },
                        selectedContentColor = SonoraRed,
                    )
                }
            }

            Text("Your music", style = MaterialTheme.typography.titleLarge)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(listOf("Albums", "Songs", "Artists")) { label ->
                    Card(
                        onClick = onOpenNowPlaying,
                        modifier = Modifier.fillMaxWidth(0.42f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Browse",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text("Playlists", style = MaterialTheme.typography.titleLarge)
            Text(
                "Your local playlists will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
