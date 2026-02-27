import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.host_screen.ComposiumHostScreen
import oleginvoke.com.composium.ui.components.ComposiumBadge
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumTopBar
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import oleginvoke.com.composium.ui.theme.Tokens

@Preview(showBackground = true, widthDp = 420, heightDp = 840)
@Composable
private fun ComposiumHostScreenPreview() {
    LaunchedEffect(Unit) {
        if (ComposiumRuntime.scenes.isEmpty()) {
            ComposiumRuntime.register(
                Scene(group = "Preview", name = "Demo") {
                    ComposiumText("Hello from ComposiumHostScreen preview")
                }
            )
            ComposiumRuntime.register(
                Scene(group = "Preview", name = "With params") {
                    var enabled: Boolean by param(true, "Enabled")
                    ComposiumText("Enabled = $enabled")
                }
            )
        }
    }

    ComposiumTheme {
        ComposiumHostScreen()
    }
}

@Preview
@Composable
private fun TopBarPreview() {
    ComposiumTheme {
        ComposiumTopBar(
            title = {
                ComposiumText(
                    text = "Hello",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Tokens.typography.titleLarge,
                    color = Tokens.colors.onSurface,
                )
            },
            navigationIcon = {
                ComposiumIconButton(onClick = {}) {
                    ComposiumIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Tokens.colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            actions = {
                ComposiumIconButton(
                    enabled = true,
                    onClick = { },
                ) {
                    ComposiumIcon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = "Properties",
                        tint = Tokens.colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            containerColor = Tokens.colors.surface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreviewDark() {
    ComposiumTheme {
        ComposiumHostScreen()
    }
}

@Preview
@Composable
private fun BadgePreview() {
    ComposiumTheme {
        ComposiumBadge(
            text = "1",
            containerColor = Tokens.colors.primaryContainer,
            contentColor = Tokens.colors.onPrimaryContainer,
        )
    }
}
