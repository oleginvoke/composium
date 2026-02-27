package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun ComposiumText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    // BasicText не имеет отдельного параметра color, поэтому просто мерджим его в style.
    val resolved = if (color != null) style.merge(TextStyle(color = color)) else style
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
    )
}
