package com.sipun.sonora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sipun.sonora.navigation.SonoraApp
import com.sipun.sonora.ui.theme.SonoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0xFFF6F7FA.toInt(), 0xFFF6F7FA.toInt()),
            navigationBarStyle = SystemBarStyle.light(0xFFF6F7FA.toInt(), 0xFFF6F7FA.toInt()),
        )

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContent {
            SonoraTheme {
                SonoraPermissionGate()
            }
        }
    }

    @Composable
    private fun SonoraPermissionGate() {
        val permission = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
        var permissionGranted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED,
            )
        }
        var showExplanation by remember { mutableStateOf(!permissionGranted) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            permissionGranted = granted
            showExplanation = !granted
        }

        if (permissionGranted) {
            SonoraApp()
        } else {
            if (!showExplanation) {
                PermissionRequiredScreen(onRequest = { launcher.launch(permission) })
            }

            if (showExplanation) {
                AlertDialog(
                    onDismissRequest = { showExplanation = false },
                    title = { Text("Access your music") },
                    text = {
                        Text("Sonora needs access to your audio files to find and play the music stored on your device.")
                    },
                    confirmButton = {
                        Button(onClick = {
                            showExplanation = false
                            launcher.launch(permission)
                        }) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExplanation = false }) {
                            Text("Not now")
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun PermissionRequiredScreen(onRequest: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Music access is required", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Allow Sonora to access your music library to load your songs.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRequest, modifier = Modifier.padding(top = 20.dp)) {
                Text("Allow access")
            }
        }
    }
}
