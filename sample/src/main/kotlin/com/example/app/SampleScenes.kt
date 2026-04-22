package com.example.app

import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.scene

@ComposiumScene
internal val primaryPlayground by scene(
    group = "Buttons/Primary", // optional group
    name = "Playground", // optional name
) {
    val title: String by param("Continue")
    val enabled: Boolean by param(true)
    val variant: ButtonVariant by param(ButtonVariant.Filled)
    val size: ButtonSize by param(ButtonSize.Medium)
    val showBadge: Boolean by param(
        default = false,
        name = "Leading badge", // optional name
    )

    PrimaryButtonSceneContent(
        title = title,
        enabled = enabled,
        variant = variant,
        size = size,
        showBadge = showBadge,
    )
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
