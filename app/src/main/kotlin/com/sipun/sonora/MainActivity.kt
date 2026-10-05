package com.sipun.sonora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sipun.sonora.navigation.SonoraApp
import com.sipun.sonora.ui.theme.SonoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SonoraTheme {
                SonoraApp()
            }
        }
    }
}
