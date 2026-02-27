package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Tokens.colors.surface,
    height: Dp = 56.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    statusBarInsets: WindowInsets? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor),
    ) {
        Column(
            modifier = Modifier.then(
                if (statusBarInsets != null) Modifier.windowInsetsPadding(statusBarInsets)
                else Modifier
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                    } else {
                        Spacer(Modifier.width(40.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        title()
                    }

                    if (actions != null) {
                        actions()
                    }
                }
            }
        }
    }
}
