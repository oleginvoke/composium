package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.BooleanParamDescriptor
import oleginvoke.com.composium.ObjectParamDescriptor
import oleginvoke.com.composium.ParamDescriptor
import oleginvoke.com.composium.StringParamDescriptor
import oleginvoke.com.composium.ui.components.ComposiumCheckbox
import oleginvoke.com.composium.ui.components.ComposiumChip
import oleginvoke.com.composium.ui.components.ComposiumChipDefaults
import oleginvoke.com.composium.ui.components.ComposiumSwitch
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumTextField
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ControlsPanel(
    state: SceneParamsState,
    callbacks: SceneParamsCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.params, key = { it.name }) { param ->
            ParamCard(
                paramDescriptor = param,
                callbacks = callbacks,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParamCard(
    paramDescriptor: ParamDescriptor,
    callbacks: SceneParamsCallbacks,
    modifier: Modifier = Modifier,
) {
    val isActive = paramDescriptor.activation?.isActive != false
    val shape = Tokens.shapes.large
    val shadowElevation: Dp = if (isActive) 14.dp else 8.dp
    val containerColor = if (isActive) {
        Tokens.colors.surface.copy(alpha = 0.94f)
    } else {
        Tokens.colors.surface.copy(alpha = 0.78f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Tokens.colors.scrim.copy(alpha = 0.22f),
                spotColor = Tokens.colors.scrim.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (isActive) Tokens.colors.outlineVariant else Tokens.colors.outlineVariant.copy(alpha = 0.72f),
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ParamHeader(
                    paramName = paramDescriptor.name,
                    paramType = paramDescriptor.typeName,
                )
                paramDescriptor.activation?.let { activation ->
                    ComposiumCheckbox(
                        checked = activation.isActive,
                        size = 20.dp,
                        onCheckedChange = { next ->
                            callbacks.onParamActivationChange(
                                paramName = paramDescriptor.name,
                                isActive = next,
                            )
                        },
                    )
                }
            }

            if (!isActive) return@Column

            Spacer(Modifier.height(10.dp))

            when (paramDescriptor) {
                is BooleanParamDescriptor -> {
                    ComposiumSwitch(
                        checked = paramDescriptor.value,
                        onCheckedChange = { value ->
                            callbacks.onBooleanParamChange(
                                paramName = paramDescriptor.name,
                                value = value,
                            )
                        },
                    )
                }

                is ObjectParamDescriptor -> {
                    val options = paramDescriptor.options
                    val hasChoices = options.size > 1
                    var expanded by rememberSaveable { mutableStateOf(false) }

                    if (hasChoices) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            maxLines = if (!expanded) 2 else Int.MAX_VALUE,
                            overflow = FlowRowOverflow.expandIndicator {
                                ComposiumChip(
                                    text = "Show all",
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ComposiumChipDefaults.colors(
                                        selectedBackgroundColor = Tokens.colors.secondaryContainer,
                                        selectedBorderColor = Tokens.colors.secondaryContainer,
                                        selectedContentColor = Tokens.colors.onSecondaryContainer,
                                        unselectedBackgroundColor = Tokens.colors.secondaryContainer,
                                        unselectedBorderColor = Tokens.colors.secondaryContainer,
                                        unselectedContentColor = Tokens.colors.onSecondaryContainer,
                                    ),
                                    modifier = Modifier.height(24.dp),
                                    onClick = { expanded = true },
                                )
                            },
                        ) {
                            options.forEach { option ->
                                ComposiumChip(
                                    modifier = Modifier.height(24.dp),
                                    text = option.name,
                                    selected = option.name == paramDescriptor.selectedOption.name,
                                    onClick = {
                                        callbacks.onObjectParamChange(
                                            paramName = paramDescriptor.name,
                                            option = option,
                                        )
                                    },
                                )
                            }
                        }
                    } else {
                        ComposiumText(
                            text = paramDescriptor.selectedOption.name,
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colors.onSurfaceVariant,
                        )
                    }
                }

                is StringParamDescriptor -> {
                    ComposiumTextField(
                        value = paramDescriptor.value,
                        onValueChange = { value ->
                            callbacks.onStringParamChange(
                                paramName = paramDescriptor.name,
                                value = value,
                            )
                        },
                        placeholder = "Enter text",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamTypeChip(text: String) {
    ComposiumChip(
        shape = RoundedCornerShape(12.dp),
        colors = ComposiumChipDefaults.colors(
            selectedBackgroundColor = Color.Transparent,
            selectedBorderColor = Tokens.colors.outlineVariant,
            selectedContentColor = Tokens.colors.onSurfaceVariant,
            unselectedBackgroundColor = Color.Transparent,
            unselectedBorderColor = Tokens.colors.outlineVariant,
            unselectedContentColor = Tokens.colors.onSurfaceVariant,
        ),
        text = text,
    )
}

@Composable
private fun ParamHeader(
    paramName: String,
    paramType: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ComposiumText(
            text = paramName,
            style = Tokens.typography.titleMedium,
            color = Tokens.colors.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        ParamTypeChip(text = paramType)
    }
}
