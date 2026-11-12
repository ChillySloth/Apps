package com.chillysloth.neomusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.chillysloth.neomusic.data.ThemeMode
import com.chillysloth.neomusic.ui.AppRoot
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoButton
import com.chillysloth.neomusic.ui.components.NeoTag
import com.chillysloth.neomusic.ui.theme.Neo
import com.chillysloth.neomusic.ui.theme.NeoTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as NeoMusicApp

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by app.prefs.theme.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            NeoTheme(dark) {
                var granted by remember { mutableStateOf(hasAudioPermission()) }
                // Re-check when coming back from system settings.
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { granted = hasAudioPermission() }
                }
                LaunchedEffect(granted) {
                    if (granted) {
                        app.repository.start()
                        app.player.connect()
                    }
                }
                if (granted) AppRoot(app)
                else PermissionScreen(onGranted = { granted = true })
            }
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    @Composable
    private fun PermissionScreen(onGranted: () -> Unit) {
        var askedOnce by remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            askedOnce = true
            if (result[audioPermission()] == true) onGranted()
        }
        val permanentlyDenied = askedOnce && !shouldShowRequestPermissionRationale(audioPermission())

        Box(
            Modifier
                .fillMaxSize()
                .background(Neo.colors.paper)
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    NeoBox(
                        Modifier.size(132.dp), color = Neo.colors.yellow,
                        shape = RoundedCornerShape(32.dp), shadow = 8.dp, borderWidth = 3.dp,
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Headphones, null, tint = Color(0xFF111111), modifier = Modifier.size(72.dp))
                    }
                    NeoTag(
                        "HI!", Neo.colors.pink,
                        Modifier.align(Alignment.TopEnd).graphicsLayer {
                            rotationZ = 12f; translationX = 18.dp.toPx(); translationY = (-10).dp.toPx()
                        },
                    )
                }
                Spacer(Modifier.height(36.dp))
                NeoBox(Modifier.fillMaxWidth(), shadow = 6.dp) {
                    Column(Modifier.padding(22.dp)) {
                        Text("LET'S GET\nLOUD.", style = Neo.type.hero, color = Neo.colors.text)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Neo Music needs access to the audio files on this device to build your library. " +
                                "Nothing leaves your phone.",
                            style = Neo.type.body, color = Neo.colors.textMuted,
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.Start) {
                            if (permanentlyDenied) {
                                NeoButton(
                                    "Open settings",
                                    {
                                        startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                                        )
                                    },
                                    icon = Icons.Rounded.LockOpen, color = Neo.colors.pink,
                                )
                            } else {
                                NeoButton(
                                    "Grant access",
                                    {
                                        val perms = buildList {
                                            add(audioPermission())
                                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        launcher.launch(perms.toTypedArray())
                                    },
                                    icon = Icons.Rounded.LockOpen, color = Neo.colors.green,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
