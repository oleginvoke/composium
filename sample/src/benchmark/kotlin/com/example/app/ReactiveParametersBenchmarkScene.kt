package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.scene

private val ReactiveParameterNames = List(100) { index -> "reactiveColor$index" }
private val ProductThemeA = lightColorScheme(
    surface = Color(0xFFFFE0E0),
    background = Color(0xFFFFE0E0),
    primary = Color(0xFF1A4BA3),
)
private val ProductThemeB = lightColorScheme(
    surface = Color(0xFFFFF4BA),
    background = Color(0xFFFFF4BA),
    primary = Color(0xFF6A1B9A),
)

@ComposiumScene
internal val reactiveParametersBenchmark by scene(
    group = "Benchmark/Parameters",
    name = "Reactive parameters",
    tools = SceneTools.None,
    thumbnail = null,
) { contentPadding ->
    var isProductB by remember { mutableStateOf(false) }
    var themeSwitches by remember { mutableIntStateOf(0) }
    var unrelatedRecompositions by remember { mutableIntStateOf(0) }
    MaterialTheme(
        colorScheme = if (isProductB) ProductThemeB else ProductThemeA,
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val options = listOf(
            colorScheme.surface named "surface",
            colorScheme.primary named "primary",
        )
        val colors = mutableListOf<Color>()
        // Every declaration has a fixed, unique explicit name. The first option selects "surface".
        ReactiveParameterNames.forEach { name ->
            val color: Color by param(options = options, name = name)
            colors.add(color)
        }

        // These reads invalidate the content, including all param calls, on an unrelated click.
        val status = "Theme: product ${if (isProductB) "B" else "A"}; " +
            "theme switches: $themeSwitches; " +
            "unrelated recompositions: $unrelatedRecompositions; params: ${colors.size}"

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Reactive parameters", style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(
                    onClick = host.onBack,
                    modifier = Modifier.semantics { contentDescription = "Return to catalog" },
                ) {
                    Text("Back")
                }
                Button(
                    onClick = {
                        themeSwitches++
                        isProductB = !isProductB
                    },
                    modifier = Modifier.semantics { contentDescription = "Switch product theme" },
                ) {
                    Text("Switch product theme")
                }
                Button(
                    onClick = { unrelatedRecompositions++ },
                    modifier = Modifier.semantics { contentDescription = "Recompose scene" },
                ) {
                    Text("Recompose scene")
                }
                Text(
                    text = status,
                    modifier = Modifier.semantics { contentDescription = "Reactive parameter status" },
                    style = MaterialTheme.typography.bodySmall,
                )
                // Informational only: the baseline runtime intentionally retains the old color.
                Text(
                    text = "Observed surface: ${colors.first().argbHex()}; " +
                        "current surface: ${colorScheme.surface.argbHex()}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(10) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            repeat(10) { column ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(16.dp)
                                        .background(colors[row * 10 + column]),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.argbHex(): String =
    "#${toArgb().toUInt().toString(16).padStart(8, '0').uppercase()}"
