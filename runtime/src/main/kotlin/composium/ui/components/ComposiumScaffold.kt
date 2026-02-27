
package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.background),
    ) {
        topBar()
        content(PaddingValues())
    }
}
