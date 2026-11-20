package composium.compatibility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import oleginvoke.com.composium.Composium
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.scene

sealed interface Choice {
    data object First : Choice
    data object Second : Choice
}

@Volatile internal var sceneDisplayId: Int? = null

@ComposiumScene
val CompatibilityScene by scene(name = "Compatibility scene", tools = SceneTools.TopBar) { padding ->
    val view = LocalView.current
    SideEffect { sceneDisplayId = view.display?.displayId }
    val choice: Choice by param(Choice.First)
    val label: String by param("Editable text")
    Column(Modifier.padding(padding)) {
        BasicText("Scene rendered")
        BasicText(choice.toString())
        BasicText(label)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sceneDisplayId = null
        if (!BuildConfig.USE_KSP) Composium.register(CompatibilityScene)
        setContentView(ComposeView(this).apply { setContent { ComposiumScreen() } })
    }
}
