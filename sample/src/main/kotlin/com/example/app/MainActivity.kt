package com.example.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }

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

            ComposiumPreviewScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = { isDarkTheme = it },
            )
        }
    }
}

private val LightNavigationBarColor = Color.WHITE
private val DarkNavigationBarColor = Color.rgb(17, 25, 32)
