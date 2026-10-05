package com.sipun.sonora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sipun.sonora.core.update.UpdateInstaller
import com.sipun.sonora.core.update.UpdateManager
import com.sipun.sonora.core.update.UpdateNotificationHelper
import com.sipun.sonora.navigation.SonoraApp
import com.sipun.sonora.ui.theme.SonoraTheme
import com.sipun.sonora.ui.update.UpdateContent

class MainActivity : ComponentActivity() {
    private val updateCheckRequested = mutableStateOf(false)
    private val updateNotificationRequested = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)
        setContent { SonoraTheme { SonoraPermissionGate() } }
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
                ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }
        var showExplanation by remember { mutableStateOf(!permissionGranted) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionGranted = granted
            showExplanation = !granted
        }

        if (permissionGranted) {
            SonoraContent()
        } else {
            if (!showExplanation) {
                PermissionRequiredScreen { launcher.launch(permission) }
            }
            if (showExplanation) {
                AlertDialog(
                    onDismissRequest = { showExplanation = false },
                    title = { Text("Access your music") },
                    text = {
                        Text(
                            "Sonora needs access to your audio files to find and play the music stored on your device."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExplanation = false
                                launcher.launch(permission)
                            }
                        ) {
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
    private fun SonoraContent() {
        val appIcon = remember {
            packageManager
                .getApplicationIcon(applicationInfo)
                .toBitmap()
                .asImageBitmap()
        }
        val checkRequested by updateCheckRequested
        val notificationRequested by updateNotificationRequested

        SonoraApp(
            onCheckForUpdates = { updateCheckRequested.value = true }
        )
        UpdateContent(
            checkRequested = checkRequested,
            onCheckRequestConsumed = { updateCheckRequested.value = false },
            onRequestCheck = { updateCheckRequested.value = true },
            notificationRequested = notificationRequested,
            onNotificationRequestConsumed = {
                updateNotificationRequested.value = false
            },
            appIcon = appIcon,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun configureSystemBars() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                0xFFF6F7FA.toInt(),
                0xFFF6F7FA.toInt(),
            ),
            navigationBarStyle = SystemBarStyle.light(
                0xFFF6F7FA.toInt(),
                0xFFF6F7FA.toInt(),
            ),
        )
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightNavigationBars = true
    }

    private fun handleUpdateIntent(intent: Intent?) {
        when (intent?.action) {
            UpdateManager.ACTION_SHOW_UPDATE -> {
                updateNotificationRequested.value = true
            }

            UpdateManager.ACTION_INSTALL_UPDATE -> {
                val installer = UpdateInstaller(applicationContext)
                if (
                    installer.installDownloadedUpdate() ==
                    UpdateInstaller.Result.PermissionRequired
                ) {
                    installer.openInstallPermissionSettings()
                }
            }
        }
    }

    @Composable
    private fun PermissionRequiredScreen(onRequest: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Music access is required",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Allow Sonora to access your music library to load your songs.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequest,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Allow access")
            }
        }
    }
}
