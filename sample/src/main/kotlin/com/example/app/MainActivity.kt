package com.example.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = Color.TRANSPARENT,
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = Color.TRANSPARENT,
                            darkScrim = Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = DarkNavigationBarColor,
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = LightNavigationBarColor,
                            darkScrim = LightNavigationBarColor,
                        )
                    },
                )

                onDispose {}
            }

            @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                ComposiumPreviewScreen(
                    isDarkTheme = isDarkTheme,
                    contentWindowInsets = WindowInsets.systemBars,
                    onThemeChange = {
                        isDarkTheme = it
                    },
                )
            }
        }
    }
}

private val LightNavigationBarColor = Color.WHITE
private val DarkNavigationBarColor = Color.rgb(17, 25, 32)
