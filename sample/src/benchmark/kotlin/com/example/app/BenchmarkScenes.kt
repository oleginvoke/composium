package com.example.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.scene

@ComposiumSceneCatalog
internal object BenchmarkScenes {

    val buttonsCard01 by benchmarkScene(group = "Benchmark/Buttons", index = 1)
    val buttonsCard02 by benchmarkScene(group = "Benchmark/Buttons", index = 2)
    val buttonsCard03 by benchmarkScene(group = "Benchmark/Buttons", index = 3)
    val buttonsCard04 by benchmarkScene(group = "Benchmark/Buttons", index = 4)

    val cards01 by benchmarkScene(group = "Benchmark/Cards", index = 5)
    val cards02 by benchmarkScene(group = "Benchmark/Cards", index = 6)
    val cards03 by benchmarkScene(group = "Benchmark/Cards", index = 7)
    val cards04 by benchmarkScene(group = "Benchmark/Cards", index = 8)

    val forms01 by benchmarkScene(group = "Benchmark/Forms", index = 9)
    val forms02 by benchmarkScene(group = "Benchmark/Forms", index = 10)
    val forms03 by benchmarkScene(group = "Benchmark/Forms", index = 11)
    val forms04 by benchmarkScene(group = "Benchmark/Forms", index = 12)

    val banners01 by benchmarkScene(group = "Benchmark/Banners", index = 13)
    val banners02 by benchmarkScene(group = "Benchmark/Banners", index = 14)
    val banners03 by benchmarkScene(group = "Benchmark/Banners", index = 15)
    val banners04 by benchmarkScene(group = "Benchmark/Banners", index = 16)

    val sheets01 by benchmarkScene(group = "Benchmark/Sheets", index = 17)
    val sheets02 by benchmarkScene(group = "Benchmark/Sheets", index = 18)
    val sheets03 by benchmarkScene(group = "Benchmark/Sheets", index = 19)
    val sheets04 by benchmarkScene(group = "Benchmark/Sheets", index = 20)
}

private fun benchmarkScene(
    group: String,
    index: Int,
) = scene(
    group = group,
    name = "Benchmark $index",
) {
    BenchmarkSceneCard(
        title = "Benchmark scene $index",
        description = "Benchmark-only synthetic scene used to stress the catalog, search, and navigation benchmarks.",
    )
}

@Composable
private fun BenchmarkSceneCard(
    title: String,
    description: String,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            repeat(4) { line ->
                Text(
                    text = "Synthetic benchmark content line ${line + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
