package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
    val shape = Tokens.shapes.medium
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tokens.colors.surfaceVariant)
            .border(
                width = 1.dp,
                color = Tokens.colors.outlineVariant.copy(alpha = 0.9f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ParamHeader(
                    modifier = Modifier,
                    paramName = paramDescriptor.name,
                    paramType = paramDescriptor.typeName,
                )
                paramDescriptor.activation?.let { activation ->
                    ComposiumCheckbox(
                        modifier = Modifier,
                        checked = activation.isActive,
                        size = 20.dp,
                        onCheckedChange = { isActive ->
                            callbacks.onParamActivationChange(
                                paramName = paramDescriptor.name,
                                isActive = isActive,
                            )
                        },
                    )
                }
            }
            if (paramDescriptor.activation?.isActive != false) {
                Spacer(Modifier.height(4.dp))
                when (paramDescriptor) {
                    is BooleanParamDescriptor -> {
                        ComposiumSwitch(
                            modifier = Modifier,
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                maxLines = if (!expanded) 2 else Int.MAX_VALUE,
                                overflow = FlowRowOverflow.expandIndicator {
                                    ComposiumChip(
                                        text = "Show all",
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ComposiumChipDefaults.colors(
                                            unselectedBackgroundColor = Tokens.colors.tertiaryContainer,
                                            unselectedBorderColor = Tokens.colors.outline.copy(alpha = 0.5f),
                                        ),
                                        modifier = Modifier
                                            .height(22.dp)
                                            .clickable { expanded = true },
                                    )
                                },
                            ) {
                                options.forEach { option ->
                                    ComposiumChip(
                                        modifier = Modifier
                                            .height(22.dp)
                                            .clickable {
                                                callbacks.onObjectParamChange(
                                                    paramName = paramDescriptor.name,
                                                    option = option,
                                                )
                                            },
                                        text = option.label,
                                        selected = option == paramDescriptor.selectedOption,
                                    )
                                }
                            }
                        } else {
                            ComposiumText(
                                text = paramDescriptor.selectedOption.label,
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamTypeChip(text: String) {
    ComposiumChip(
        modifier = Modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ComposiumChipDefaults.colors(
            unselectedBackgroundColor = Color.Transparent,
            unselectedBorderColor = Tokens.colors.outline.copy(alpha = 0.12f),
        ),
        text = text,
    )
}

@Composable
private fun ParamHeader(modifier: Modifier = Modifier, paramName: String, paramType: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ComposiumText(
            text = paramName,
            style = Tokens.typography.titleMedium,
            color = Tokens.colors.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        ParamTypeChip(text = paramType)
    }
}
