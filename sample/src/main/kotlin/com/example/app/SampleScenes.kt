package com.example.app

import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.optionListLabeled
import oleginvoke.com.composium.scene

@ComposiumScene
internal val gettingStarted by scene(name = "Getting started") {
    GettingStartedSceneContent()
}

@ComposiumScene
val primaryPlayground by scene(
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
val tonalNestedPlayground by scene(
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

    val labelsAndNullable by scene(
        group = "Parameters/Custom options",
        name = "Labels + nullable",
    ) {
        val role: UserRole by param(
            default = UserRole.Member,
        )
        val avatarSize: Int? by param(
            default = null,
            options = optionListLabeled(
                32 to "32 dp",
                48 to "48 dp",
                72 to "72 dp",
            ),
        )
        val subtitle: String? by param(
            default = null,
            name = "Subtitle",
        )

        NullableLabelsSceneContent(
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
            options = optionListLabeled(
                8 to "Compact",
                16 to "Comfortable",
                24 to "Spacious",
            ),
        )
        val actionLabel: String by param("Retry")

        EnumAndNumericOptionsSceneContent(
            tone = tone,
            spacing = spacing,
            actionLabel = actionLabel,
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
