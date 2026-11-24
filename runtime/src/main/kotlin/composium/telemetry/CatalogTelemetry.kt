package oleginvoke.com.composium.telemetry

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

internal val LocalCatalogTelemetry = staticCompositionLocalOf<CatalogTelemetry?> { null }

private const val TelemetryEnabledKey = "composium.telemetry.enabled"

@Suppress("DEPRECATION") // The int-flags overload also supports Android 12 and below.
internal fun isCatalogTelemetryEnabled(context: Context): Boolean = try {
    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    val metadata = info.metaData
    metadata == null || !metadata.containsKey(TelemetryEnabledKey) || metadata.get(TelemetryEnabledKey) == true
} catch (_: Exception) {
    // Skip collection when the host's configuration cannot be read.
    false
}

@Composable
internal fun rememberCatalogTelemetry(): CatalogTelemetry? {
    if (LocalInspectionMode.current) return null
    val app = LocalContext.current.applicationContext
    if (!remember(app) { isCatalogTelemetryEnabled(app) }) return null
    val reported = rememberSaveable { arrayListOf<String>() }
    val config = LocalConfiguration.current
    val isSample = app.packageName == "com.example.ComposiumSample"
    val telemetry = remember(config.screenWidthDp, config.orientation, isSample, reported) {
        val width = when {
            config.screenWidthDp >= 840 -> "expanded"
            config.screenWidthDp >= 600 -> "medium"
            else -> "compact"
        }
        val orientation = when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "unknown"
        }
        CatalogTelemetry(
            reported,
            mapOf(
                "source" to if (isSample) "sample" else "consumer",
                "width_class" to width,
                "orientation" to orientation,
            ),
        )
    }
    LaunchedEffect(telemetry) { telemetry.openCatalog() }
    return telemetry
}

/** Owned by one catalog visit. Only fixed feature names go into the saved state. */
internal class CatalogTelemetry(
    private val reported: ArrayList<String>,
    private val contextFields: Map<String, String>,
    private val enqueue: (String, Map<String, String>) -> Unit = ScarfTransport::enqueue,
) {
    fun openCatalog() {
        if ("catalog_opened" !in reported) {
            reported.add("catalog_opened")
            record("catalog_opened")
        }
    }

    fun feature(name: String) {
        if (name !in reported) {
            reported.add(name)
            record("feature_used", mapOf("feature" to name))
        }
    }

    fun record(event: String, fields: Map<String, String> = emptyMap()) {
        enqueue(event, contextFields + fields)
    }
}

internal class ThumbnailRun(
    private val telemetry: CatalogTelemetry,
    private val catalogSize: Int,
    private val cacheHits: Int,
) {
    private val start = SystemClock.elapsedRealtime()
    private var firstVisibleMs: Long? = null
    var captured = 0
    var failures = 0

    fun visibleReady() {
        if (firstVisibleMs == null) firstVisibleMs = SystemClock.elapsedRealtime() - start
    }

    fun finish(completed: Boolean) {
        val fields = mutableMapOf(
            "catalog_size" to catalogSize.toString(),
            "cache_hits" to cacheHits.toString(),
            "captured" to captured.toString(),
            "failed_attempts" to failures.toString(),
            "outcome" to if (completed) "completed" else "interrupted",
            "duration_ms" to (SystemClock.elapsedRealtime() - start).toString(),
        )
        firstVisibleMs?.let { milliseconds ->
            fields["first_visible"] = when {
                milliseconds < 500 -> "under_500ms"
                milliseconds < 1000 -> "500ms_to_1s"
                milliseconds < 3000 -> "1s_to_3s"
                else -> "3s_or_more"
            }
        }
        telemetry.record("thumbnail_run", fields)
    }
}
