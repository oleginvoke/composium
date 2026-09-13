package oleginvoke.com.composium.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class ComposiumIssueRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val vendor = Vendor(
        vendorName = "Composium",
        identifier = "oleginvoke.com.composium",
    )
    override val issues = listOf(
        ComposiumContentPaddingDetector.UnusedComposiumContentPaddingParameter,
    )
}
