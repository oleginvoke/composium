package oleginvoke.com.composium.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogTelemetryTest {
    @Test
    fun repeatedEditsAndConfigurationRestoreReportEachFeatureOncePerVisit() {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        val reported = arrayListOf<String>()
        fun session(saved: ArrayList<String>, orientation: String) =
            CatalogTelemetry(saved, mapOf("orientation" to orientation)) { event, fields ->
                events.add(event to fields)
            }

        val first = session(reported, "portrait")
        first.openCatalog()
        repeat(10) { first.feature("edit_parameter") }
        val rotated = session(ArrayList(reported), "landscape")
        rotated.openCatalog()
        rotated.feature("edit_parameter")
        rotated.feature("rtl")
        session(arrayListOf(), "landscape").openCatalog()

        assertEquals(
            listOf(
                "catalog_opened" to mapOf("orientation" to "portrait"),
                "feature_used" to mapOf("orientation" to "portrait", "feature" to "edit_parameter"),
                "feature_used" to mapOf("orientation" to "landscape", "feature" to "rtl"),
                "catalog_opened" to mapOf("orientation" to "landscape"),
            ),
            events,
        )
    }
}
