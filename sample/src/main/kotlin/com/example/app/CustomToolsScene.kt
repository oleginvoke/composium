package com.example.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.scene

@ComposiumScene
internal val CustomToolsScene by scene(
    group = "Custom tools",
    name = "Custom toolbar",
    tools = SceneTools.None,
) { contentPadding ->
    val title: String by param("Profile")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledTonalButton(onClick = host.onBack) { Text("Back") }
                        Text("Custom toolbar", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = host.controls::toggle,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = if (host.controls.isVisible) ButtonDefaults.buttonColors()
                                else ButtonDefaults.filledTonalButtonColors(),
                        ) { Text("Properties") }
                        Button(
                            onClick = host.eyedropper::toggle,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = if (host.eyedropper.isVisible) ButtonDefaults.buttonColors()
                                else ButtonDefaults.filledTonalButtonColors(),
                        ) { Text("Eyedropper") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = host.theme.isDark,
                                role = Role.Switch,
                                onValueChange = host.theme::setDark,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Dark theme", Modifier.weight(1f))
                        Switch(checked = host.theme.isDark, onCheckedChange = null)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Open Properties to edit this title, sample a color with the eyedropper, or switch the theme.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Back dismisses open tools first, then returns to the catalog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
