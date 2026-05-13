package oleginvoke.com.composium

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only

internal fun WindowInsets.onlyTopAndHorizontal(): WindowInsets =
    only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

internal fun WindowInsets?.onlyTopAndHorizontalOrNull(): WindowInsets? =
    this?.onlyTopAndHorizontal()
