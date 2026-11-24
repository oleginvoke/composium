package oleginvoke.com.composium.telemetry

import android.app.Application
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35])
class TelemetryManifestTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val flag = "composium.telemetry.enabled"

    @Test
    fun readsTheHostManifestAndDefaultsToEnabledWhenTheKeyIsAbsent() {
        assertFalse(isCatalogTelemetryEnabled(app)) // src/test/AndroidManifest.xml opts out.
        assertTrue(isCatalogTelemetryEnabled(hostWithMetadata(null)))
        assertTrue(isCatalogTelemetryEnabled(hostWithMetadata(Bundle().apply { putBoolean("other", false) })))
        assertTrue(isCatalogTelemetryEnabled(hostWithMetadata(Bundle().apply { putBoolean(flag, true) })))
    }

    @Test
    fun unreadableOrMalformedSettingsDisableCollection() {
        assertFalse(isCatalogTelemetryEnabled(hostWithMetadata(Bundle().apply { putString(flag, "invalid") })))
        val missingPackage = object : ContextWrapper(app) {
            override fun getPackageName() = "missing.composium.host"
        }
        assertFalse(isCatalogTelemetryEnabled(missingPackage))
    }

    private fun hostWithMetadata(metadata: Bundle?): ContextWrapper {
        val host = object : ContextWrapper(app) {
            override fun getPackageName() = "test.composium.host"
        }
        val pkg = PackageInfo().apply {
            packageName = host.packageName
            applicationInfo = ApplicationInfo().apply {
                packageName = host.packageName
                metaData = metadata
            }
        }
        shadowOf(app.packageManager).installPackage(pkg)
        return host
    }
}
