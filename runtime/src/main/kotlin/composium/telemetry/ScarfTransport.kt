package oleginvoke.com.composium.telemetry

import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import oleginvoke.com.composium.BuildConfig
import org.json.JSONObject

internal object ScarfTransport {
    private data class Event(val name: String, val fields: Map<String, String>, val time: Long)

    private val worker = ThreadPoolExecutor(
        0, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(16),
        { task ->
            Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                } catch (_: SecurityException) {
                    // The worker can still deliver events at its inherited priority.
                }
                task.run()
            }, "Composium telemetry")
        },
        ThreadPoolExecutor.DiscardPolicy(),
    )
    // Only the worker reads and writes this deadline.
    private var nextAttempt = 0L
    private val timestamp by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun enqueue(name: String, fields: Map<String, String>) {
        val event = Event(name, fields, System.currentTimeMillis())
        worker.execute {
            if (SystemClock.elapsedRealtime() < nextAttempt) return@execute
            val delivered = try {
                send(event)
            } catch (_: Exception) {
                // Includes offline devices and hosts that remove the INTERNET permission.
                false
            }
            if (!delivered) nextAttempt = SystemClock.elapsedRealtime() + 60_000
        }
    }

    private fun send(event: Event): Boolean {
        val payload = JSONObject(event.fields)
            .put("\$time", timestamp.format(Date(event.time)))
            .put("\$version", BuildConfig.COMPOSIUM_VERSION)
            .put("android_api", Build.VERSION.SDK_INT)
            .put("device_model", Build.MODEL?.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, true) })
            .put(
                "emulator",
                Build.HARDWARE == "goldfish" || Build.HARDWARE == "ranchu" ||
                    Build.FINGERPRINT.orEmpty().startsWith("generic"),
            )
            .toString().toByteArray(Charsets.UTF_8)
        val connection = URL("https://oleginvoke.gateway.scarf.sh/composium-runtime/${event.name}")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 2000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Composium/${BuildConfig.COMPOSIUM_VERSION}")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            return connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }
}
