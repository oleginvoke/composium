package oleginvoke.com.composium.scene_thumbnail

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.os.trace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CompletableDeferred
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.SceneToolStateBinding
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import oleginvoke.com.composium.ui.theme.ComposiumThemeController
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController

/** One attached composition with its own display and input window, owned by the capture coroutine. */
internal class SceneThumbnailWindow private constructor(
    private val reader: ImageReader,
    private val display: VirtualDisplay,
    private val presentation: Presentation,
    private val owners: ThumbnailWindowOwner,
    private val view: ComposeView,
) : AutoCloseable {
    val width = reader.width
    val height = reader.height

    suspend fun draw(request: SceneThumbnailCaptureRequest, bitmap: Bitmap) {
        val ready = CompletableDeferred<Unit>()
        val captureKey = request.key
        val sceneScope = SceneScope().apply {
            host.theme.binding = SceneToolStateBinding(read = { captureKey.isDarkTheme }, change = {})
        }
        check(presentation.isShowing) { "Thumbnail window was dismissed" }
        presentation.setOnDismissListener {
            ready.completeExceptionally(IllegalStateException("Thumbnail window was dismissed"))
        }
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                ready.completeExceptionally(IllegalStateException("Thumbnail window was detached"))
            }
        }
        view.addOnAttachStateChangeListener(listener)
        try {
            view.setContent {
                key(ready) {
                    ComposiumTheme(darkTheme = captureKey.isDarkTheme) {
                        CompositionLocalProvider(
                            LocalComposiumThemeController provides ComposiumThemeController(captureKey.isDarkTheme),
                        ) {
                            SceneThumbnailRenderSurface(
                                sceneEntry = request.sceneEntry,
                                sceneScope = sceneScope,
                                captureScale = captureKey.captureScale,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    if (coordinates.size.width == width && coordinates.size.height == height) {
                                        ready.complete(Unit)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            ready.await()
            // Let placement-driven state and the first window draw settle before copying pixels.
            repeat(2) { withFrameNanos { } }
            check(view.isAttachedToWindow) { "Thumbnail window was detached" }
            bitmap.eraseColor(Color.TRANSPARENT)
            trace("Composium.thumbnail.draw") { view.draw(Canvas(bitmap)) }
        } finally {
            view.removeOnAttachStateChangeListener(listener)
            presentation.setOnDismissListener(null)
        }
    }

    fun clearContent() {
        view.setContent {}
    }

    override fun close() {
        try {
            view.disposeComposition()
        } finally {
            try {
                presentation.dismiss()
            } finally {
                owners.close()
                display.release()
                reader.setOnImageAvailableListener(null, null)
                reader.close()
            }
        }
    }

    companion object {
        fun open(
            host: View,
            parent: CompositionContext,
            viewportWidth: Int,
            viewportHeight: Int,
            hostInsets: () -> WindowInsetsCompat,
        ): SceneThumbnailWindow {
            val context = host.context
            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val reader = ImageReader.newInstance(viewportWidth, viewportHeight, PixelFormat.RGBA_8888, 2)
            var display: VirtualDisplay? = null
            var presentation: Presentation? = null
            var owners: ThumbnailWindowOwner? = null
            try {
                reader.setOnImageAvailableListener({ it.acquireLatestImage()?.close() }, Handler(Looper.getMainLooper()))
                display = checkNotNull(manager.createVirtualDisplay(
                    "Composium thumbnails", viewportWidth, viewportHeight, context.resources.displayMetrics.densityDpi,
                    reader.surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                )) { "Unable to create a thumbnail display" }
                presentation = Presentation(context, display.display)
                val window = checkNotNull(presentation.window)
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                WindowCompat.setDecorFitsSystemWindows(window, false)

                val root = object : FrameLayout(presentation.context) {
                    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                        return super.dispatchApplyWindowInsets(hostInsets().toWindowInsets() ?: insets)
                    }
                }
                owners = ThumbnailWindowOwner()
                root.setViewTreeLifecycleOwner(owners)
                root.setViewTreeSavedStateRegistryOwner(owners)
                root.setViewTreeViewModelStoreOwner(host.findViewTreeViewModelStoreOwner())
                val view = ComposeView(presentation.context).apply {
                    setParentCompositionContext(parent)
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
                root.addView(view, FrameLayout.LayoutParams(viewportWidth, viewportHeight))
                presentation.setContentView(root)
                presentation.show()
                return SceneThumbnailWindow(reader, display, presentation, owners, view)
            } catch (failure: Throwable) {
                try {
                    presentation?.dismiss()
                } finally {
                    owners?.close()
                    display?.release()
                    reader.close()
                }
                throw failure
            }
        }
    }
}

private class ThumbnailWindowOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun close() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
