package com.example.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.scene

@ComposiumScene
internal val SampleButton by sceneWithDecorator(
    group = "Buttons", // optional group
) {
    val text: String by param("Continue")
    val size: ButtonSize by param(ButtonSize.Medium)
    val enabled: Boolean by param(true)
    val loading: Boolean by param(false)
    val style: ButtonStyle by param(ButtonStyle.default)
    val leadingIcon: Painter? by param(
        options = listOf(
            painterResource(R.drawable.solid_help_circle) named "solid_help_circle",
            painterResource(R.drawable.solid_information) named "solid_information",
        )
    )
    val trailingIcon: Painter? by param(
        options = listOf(
            painterResource(R.drawable.solid_help_circle) named "solid_help_circle",
            painterResource(R.drawable.solid_information) named "solid_information",
        )
    )

    SampleButton(
        onClick = {},
        text = text,
        size = size,
        enabled = enabled,
        loading = loading,
        style = style,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    )
}

@ComposiumScene
internal val testList by sceneWithDecorator(
    enableEdgeToEdge = false,
    scrollable = false,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
    ) {
        items(10) {
            Text("$it")
        }
    }
}

@ComposiumScene
internal val tonalNestedPlayground by scene(
    group = "Buttons/Secondary/Tonal",
    name = "Nested playground",
) {
    val title: String by param("Invite teammate")
    val enabled: Boolean by param(true)
    val size: ButtonSize by param(ButtonSize.Medium) { inferred ->
        inferred.reversed()
    }

    NestedButtonSceneContent(
        title = title,
        enabled = enabled,
        size = size,
    )
}

@ComposiumSceneCatalog
internal object ParameterScenes {

    val namesAndNullable by scene(
        group = "Parameters/Custom options",
        name = "Names + nullable",
    ) {
        val role: UserRole by param(
            default = UserRole.Member,
        )
        val avatarSize: Int? by param(
            default = null,
            options = listOf(
                32 named "32 dp",
                48 named "48 dp",
                72 named "72 dp",
            ),
        )
        val subtitle: String? by param(
            default = null,
            name = "Subtitle",
        )

        NullableNamesSceneContent(
            role = role,
            avatarSize = avatarSize,
            subtitle = subtitle,
        )
    }

    val enumAndNumericOptions by scene(
        group = "Parameters/Automatic",
        name = "Enum + numeric options",
    ) {
        val tone: BannerTone by param(BannerTone.Success)
        val spacing: Int by param(
            default = 16,
            options = listOf(
                8,
                16,
                24,
            ).toParamOptions(),
        )
        val actionText: String by param("Retry")

        EnumAndNumericOptionsSceneContent(
            tone = tone,
            spacing = spacing,
            actionText = actionText,
        )
    }
}

@ComposiumScene
internal val modalBottomSheetDemo by scene(
    group = "Layouts/Overlays/Bottom sheets",
    name = "Modal bottom sheet",
) {
    var opened: Boolean by param(
        default = false,
        name = "Opened",
    )
    val title: String by param("Share options")
    val supportingText: String? by param(
        default = "Use this scene to validate overlays and theme behavior.",
        name = "Supporting text",
    )

    ModalBottomSheetSceneContent(
        opened = opened,
        title = title,
        supportingText = supportingText,
        onOpen = { opened = true },
        onDismiss = { opened = false },
    )
}
