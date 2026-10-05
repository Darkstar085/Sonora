package com.sipun.sonora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sipun.sonora.core.update.AppUpdate
import com.sipun.sonora.core.update.DownloadProgress
import com.sipun.sonora.core.update.UpdateInstaller
import com.sipun.sonora.core.update.UpdateManager
import com.sipun.sonora.core.update.UpdateNotificationHelper
import com.sipun.sonora.navigation.SonoraApp
import com.sipun.sonora.ui.components.DownloadProgressDialog
import com.sipun.sonora.ui.components.UpdateDialog
import com.sipun.sonora.ui.theme.SonoraRed
import com.sipun.sonora.ui.theme.SonoraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val updateCheckRequested = mutableStateOf(false)
    private val updateNotificationRequested = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0xFFF6F7FA.toInt(), 0xFFF6F7FA.toInt()),
            navigationBarStyle = SystemBarStyle.light(0xFFF6F7FA.toInt(), 0xFFF6F7FA.toInt()),
        )
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)
        setContent { SonoraTheme { SonoraPermissionGate() } }
    }

    @Composable
    private fun SonoraPermissionGate() {
        val permission = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        }
        var permissionGranted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
        }
        var showExplanation by remember { mutableStateOf(!permissionGranted) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            showExplanation = !granted
        }

        if (permissionGranted) {
            SonoraContent()
        } else {
            if (!showExplanation) PermissionRequiredScreen { launcher.launch(permission) }
            if (showExplanation) {
                AlertDialog(
                    onDismissRequest = { showExplanation = false },
                    title = { Text("Access your music") },
                    text = { Text("Sonora needs access to your audio files to find and play the music stored on your device.") },
                    confirmButton = {
                        Button(onClick = {
                            showExplanation = false
                            launcher.launch(permission)
                        }) { Text("Allow") }
                    },
                    dismissButton = { TextButton(onClick = { showExplanation = false }) { Text("Not now") } },
                )
            }
        }
    }

    @Composable
    private fun SonoraContent() {
        val installer = remember { UpdateInstaller(applicationContext) }
        var pendingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
        var downloadedUpdate by remember { mutableStateOf<AppUpdate?>(null) }
        var downloading by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf<DownloadProgress?>(null) }
        var showNoUpdate by remember { mutableStateOf(false) }
        var checkingForUpdate by remember { mutableStateOf(false) }
        var updateCheckFailed by remember { mutableStateOf(false) }
        val checkRequested by updateCheckRequested
        val notificationRequested by updateNotificationRequested
        val appIcon = remember { packageManager.getApplicationIcon(applicationInfo).toBitmap().asImageBitmap() }

        LaunchedEffect(Unit) {
            UpdateManager.enqueuePeriodicCheck(this@MainActivity)
            val stored = withContext(Dispatchers.IO) { UpdateManager.getValidatedPendingUpdate(this@MainActivity) }
            if (stored != null) {
                pendingUpdate = stored
                downloadedUpdate = withContext(Dispatchers.IO) { UpdateManager.getDownloadedUpdate(this@MainActivity) }
                withContext(Dispatchers.IO) { UpdateManager.getDownloadProgress(this@MainActivity, stored.tag) }?.let {
                    if (!it.isFinished && !it.isFailed) {
                        progress = it
                        downloading = true
                    }
                }
            }
        }

        LaunchedEffect(checkRequested) {
            if (!checkRequested) return@LaunchedEffect
            updateCheckRequested.value = false
            checkingForUpdate = true
            updateCheckFailed = false
            showNoUpdate = false
            try {
                val latest = withContext(Dispatchers.IO) {
                    UpdateManager.findLatestUpdate(this@MainActivity)
                }
                if (latest != null) {
                    UpdateManager.savePendingUpdate(this@MainActivity, latest)
                    pendingUpdate = latest
                    downloadedUpdate = withContext(Dispatchers.IO) {
                        UpdateManager.getDownloadedUpdate(this@MainActivity)
                    }
                } else {
                    showNoUpdate = true
                }
            } catch (_: Exception) {
                updateCheckFailed = true
            } finally {
                checkingForUpdate = false
            }
        }

        LaunchedEffect(notificationRequested) {
            if (!notificationRequested) return@LaunchedEffect
            updateNotificationRequested.value = false
            val latest = pendingUpdate ?: withContext(Dispatchers.IO) { UpdateManager.getValidatedPendingUpdate(this@MainActivity) }
            if (latest != null) {
                pendingUpdate = latest
                downloadedUpdate = withContext(Dispatchers.IO) { UpdateManager.getDownloadedUpdate(this@MainActivity) }
            }
        }

        LaunchedEffect(pendingUpdate?.tag, downloading) {
            val update = pendingUpdate ?: return@LaunchedEffect
            if (!downloading) return@LaunchedEffect
            while (downloading) {
                val status = withContext(Dispatchers.IO) { UpdateManager.getDownloadProgress(this@MainActivity, update.tag) }
                if (status != null) {
                    progress = status
                    if (status.isFinished) {
                        downloadedUpdate = withContext(Dispatchers.IO) { UpdateManager.getDownloadedUpdate(this@MainActivity) }
                        downloading = false
                        progress = null
                        break
                    }
                    if (status.isFailed) {
                        downloading = false
                        progress = null
                        Toast.makeText(this@MainActivity, "Update download failed. Please try again.", Toast.LENGTH_LONG).show()
                        break
                    }
                }
                delay(250)
            }
        }

        SonoraApp(onCheckForUpdates = { updateCheckRequested.value = true })

        pendingUpdate?.let { update ->
            if (downloading && progress != null) {
                DownloadProgressDialog(
                    update = update,
                    appIcon = appIcon,
                    progress = progress!!,
                    onCancel = {
                        UpdateManager.cancelDownload(this@MainActivity, update.tag)
                        downloading = false
                        progress = null
                    },
                )
            } else {
                UpdateDialog(
                    update = update,
                    appIcon = appIcon,
                    isDownloaded = downloadedUpdate?.tag == update.tag,
                    isDownloading = downloading,
                    onDownload = {
                        downloading = true
                        UpdateManager.enqueueDownload(this@MainActivity, update)
                    },
                    onInstall = {
                        when (installer.installDownloadedUpdate()) {
                            UpdateInstaller.Result.Success -> Unit
                            UpdateInstaller.Result.FileMissing ->
                                Toast.makeText(this@MainActivity, "Downloaded update is no longer available.", Toast.LENGTH_LONG).show()
                            UpdateInstaller.Result.PermissionRequired -> {
                                Toast.makeText(this@MainActivity, "Allow Sonora to install updates, then try again.", Toast.LENGTH_LONG).show()
                                installer.openInstallPermissionSettings()
                            }
                            UpdateInstaller.Result.Failed ->
                                Toast.makeText(this@MainActivity, "Could not open the update installer.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onDismiss = {
                        pendingUpdate = null
                        downloadedUpdate = null
                        downloading = false
                        progress = null
                    },
                )
            }
        }

        if (checkingForUpdate) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Checking for updates") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SonoraRed,
                            strokeWidth = 2.5.dp,
                        )
                        Text("Checking GitHub for the latest Sonora release.")
                    }
                },
                confirmButton = {},
            )
        }

        if (updateCheckFailed) {
            AlertDialog(
                onDismissRequest = { updateCheckFailed = false },
                title = { Text("Couldn't check for updates") },
                text = { Text("Sonora couldn't reach GitHub right now. Check your internet connection and try again.") },
                confirmButton = {
                    Button(
                        onClick = {
                            updateCheckFailed = false
                            updateCheckRequested.value = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraRed),
                    ) {
                        Text("Try again")
                    }
                },
            )
        }

        if (showNoUpdate) {
            AlertDialog(
                onDismissRequest = { showNoUpdate = false },
                title = { Text("You're up to date") },
                text = { Text("You're already running the latest version, " + BuildConfig.VERSION_NAME + ".") },
                confirmButton = {
                    Button(onClick = { showNoUpdate = false }, colors = ButtonDefaults.buttonColors(containerColor = SonoraRed)) {
                        Text("OK")
                    }
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        when (intent?.action) {
            UpdateManager.ACTION_SHOW_UPDATE -> updateNotificationRequested.value = true
            UpdateManager.ACTION_INSTALL_UPDATE -> {
                val installer = UpdateInstaller(applicationContext)
                if (installer.installDownloadedUpdate() == UpdateInstaller.Result.PermissionRequired) {
                    installer.openInstallPermissionSettings()
                }
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
            Text("Allow Sonora to access your music library to load your songs.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRequest, modifier = Modifier.padding(top = 20.dp)) { Text("Allow access") }
        }
    }
}
