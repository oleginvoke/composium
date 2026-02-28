package com.example.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.theme.ComposiumTheme
import oleginvoke.com.composium.ComposiumScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = android.graphics.Color.TRANSPARENT,
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = android.graphics.Color.TRANSPARENT,
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT,
                        )
                    },
                )

                onDispose {}
            }

            @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                ComposiumTheme(
                    darkTheme = isDarkTheme,
                ) {
                    ComposiumScreen(
                        isDarkTheme = isDarkTheme,
                        contentWindowInsets = WindowInsets.systemBars,
                        onThemeChange = {
                            isDarkTheme = it
                        },
                        scenePreviewDecorator = { scenePreview ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                scenePreview()
                            }
                        },
                    )
                }
            }
        }
    }
}
