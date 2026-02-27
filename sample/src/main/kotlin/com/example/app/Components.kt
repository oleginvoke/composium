package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class ButtonVariant {
    Filled,
    Tonal,
    Outlined,
    Text,
}

@Immutable
internal sealed interface ButtonSize {
    object Small : ButtonSize
    object Medium : ButtonSize
    object Large : ButtonSize
}

internal enum class UserRole {
    Admin,
    Member,
    Guest,
}

internal enum class BannerTone {
    Neutral,
    Success,
    Warning,
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.Admin -> "Administrator"
    UserRole.Member -> "Member"
    UserRole.Guest -> "Guest"
}

@Composable
internal fun InfoLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ButtonPreviewCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.widthIn(max = 420.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
            content()
        }
    }
}

@Composable
internal fun SampleActionButton(
    title: String,
    enabled: Boolean,
    variant: ButtonVariant,
    size: ButtonSize,
    showLeadingBadge: Boolean,
) {
    val padding = when (size) {
        ButtonSize.Small -> PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ButtonSize.Medium -> PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        ButtonSize.Large -> PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    }

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = {},
            enabled = enabled,
            contentPadding = padding,
        ) {
            ButtonLabel(
                title = title,
                showLeadingBadge = showLeadingBadge,
            )
        }

        ButtonVariant.Tonal -> FilledTonalButton(
            onClick = {},
            enabled = enabled,
            contentPadding = padding,
        ) {
            ButtonLabel(
                title = title,
                showLeadingBadge = showLeadingBadge,
            )
        }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = {},
            enabled = enabled,
            contentPadding = padding,
        ) {
            ButtonLabel(
                title = title,
                showLeadingBadge = showLeadingBadge,
            )
        }

        ButtonVariant.Text -> TextButton(
            onClick = {},
            enabled = enabled,
            contentPadding = padding,
        ) {
            ButtonLabel(
                title = title,
                showLeadingBadge = showLeadingBadge,
            )
        }
    }
}

@Composable
private fun ButtonLabel(
    title: String,
    showLeadingBadge: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showLeadingBadge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(title)
    }
}

@Composable
internal fun ProfilePreviewCard(
    role: UserRole,
    avatarSize: Int?,
    subtitle: String?,
) {
    ElevatedCard(
        modifier = Modifier.widthIn(max = 420.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Nullable values and custom labels",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size((avatarSize ?: 56).dp)
                        .clip(CircleShape)
                        .background(
                            if (avatarSize == null) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = roleLabel(role).take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (avatarSize == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = roleLabel(role),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = subtitle ?: "Subtitle is null. Toggle it back on from the controls panel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "Avatar size uses explicit numeric options. Subtitle is a nullable String, so Composium adds a null-state checkbox automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun BannerPreviewCard(
    tone: BannerTone,
    spacing: Int,
    actionLabel: String,
) {
    val containerColor = when (tone) {
        BannerTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
        BannerTone.Success -> MaterialTheme.colorScheme.primaryContainer
        BannerTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (tone) {
        BannerTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        BannerTone.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        BannerTone.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = Modifier.widthIn(max = 420.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Automatic enum + explicit numeric options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Banner tone is inferred automatically from an enum. Spacing uses explicit numeric options with custom labels.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = {}) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun GettingStartedSceneContent() {
    ElevatedCard(
        modifier = Modifier.widthIn(max = 460.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Composium sample",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "This ungrouped scene acts as a landing point for the sample app. " +
                    "Open grouped scenes on the left to explore buttons, parameters, nested catalogs, and a bottom sheet demo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            InfoLine(text = "Top-level scene without a group")
            InfoLine(text = "Catalog-based grouped scenes with @ComposiumSceneCatalog")
            InfoLine(text = "Automatic enum and sealed options")
            InfoLine(text = "Explicit numeric options, custom labels, and nullable values")
            InfoLine(text = "Bottom sheet scene inside a deep nested group")
        }
    }
}

@Composable
internal fun PrimaryButtonSceneContent(
    title: String,
    enabled: Boolean,
    variant: ButtonVariant,
    size: ButtonSize,
    showBadge: Boolean,
) {
    ButtonPreviewCard(
        title = "Button playground",
        description = "This scene shows automatic controls for String, Boolean, enum, and sealed object parameters.",
    ) {
        SampleActionButton(
            title = title,
            enabled = enabled,
            variant = variant,
            size = size,
            showLeadingBadge = showBadge,
        )
    }
}

@Composable
internal fun NestedButtonSceneContent(
    title: String,
    enabled: Boolean,
    size: ButtonSize,
) {
    ButtonPreviewCard(
        title = "Nested group example",
        description = "This scene lives in a deeper group path and also reverses the auto-inferred order of sealed options.",
    ) {
        SampleActionButton(
            title = title,
            enabled = enabled,
            variant = ButtonVariant.Tonal,
            size = size,
            showLeadingBadge = true,
        )
    }
}

@Composable
internal fun NullableLabelsSceneContent(
    role: UserRole,
    avatarSize: Int?,
    subtitle: String?,
) {
    ProfilePreviewCard(
        role = role,
        avatarSize = avatarSize,
        subtitle = subtitle,
    )
}

@Composable
internal fun EnumAndNumericOptionsSceneContent(
    tone: BannerTone,
    spacing: Int,
    actionLabel: String,
) {
    BannerPreviewCard(
        tone = tone,
        spacing = spacing,
        actionLabel = actionLabel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalBottomSheetSceneContent(
    opened: Boolean,
    title: String,
    supportingText: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            onClick = onOpen,
        ) {
            Text("Open bottom sheet")
        }
    }

    if (opened) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = supportingText ?: "Supporting text is currently null.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Close")
                }
            }
        }
    }
}
