package oleginvoke.com.composium.main_screen

import kotlin.test.Test
import kotlin.test.assertEquals

class MainScreenLogicTest {

    @Test
    fun `buildCatalogStatus returns empty catalog when there are no scenes and no query`() {
        val status = buildCatalogStatus(
            query = "",
            visibleCount = 0,
            totalCount = 0,
        )

        assertEquals(MainScreenCatalogMode.EmptyCatalog, status.mode)
    }

    @Test
    fun `buildCatalogStatus returns full catalog when query is blank and scenes exist`() {
        val status = buildCatalogStatus(
            query = "   ",
            visibleCount = 12,
            totalCount = 12,
        )

        assertEquals(MainScreenCatalogMode.FullCatalog, status.mode)
    }

    @Test
    fun `buildCatalogStatus returns filtered results when query has matches`() {
        val status = buildCatalogStatus(
            query = "button",
            visibleCount = 3,
            totalCount = 12,
        )

        assertEquals(MainScreenCatalogMode.FilteredResults, status.mode)
    }

    @Test
    fun `buildCatalogStatus returns empty results when query has no matches`() {
        val status = buildCatalogStatus(
            query = "skeleton",
            visibleCount = 0,
            totalCount = 12,
        )

        assertEquals(MainScreenCatalogMode.EmptyResults, status.mode)
    }

    @Test
    fun `reduceSearchFieldImeState does not clear focus before ime becomes visible`() {
        val result = reduceSearchFieldImeState(
            isFocused = true,
            isImeVisible = false,
            hasSeenVisibleImeForCurrentFocus = false,
        )

        assertEquals(false, result.clearFocus)
        assertEquals(false, result.hasSeenVisibleImeForCurrentFocus)
    }

    @Test
    fun `reduceSearchFieldImeState tracks ime once visible`() {
        val result = reduceSearchFieldImeState(
            isFocused = true,
            isImeVisible = true,
            hasSeenVisibleImeForCurrentFocus = false,
        )

        assertEquals(false, result.clearFocus)
        assertEquals(true, result.hasSeenVisibleImeForCurrentFocus)
    }

    @Test
    fun `reduceSearchFieldImeState clears focus only after ime hides again`() {
        val result = reduceSearchFieldImeState(
            isFocused = true,
            isImeVisible = false,
            hasSeenVisibleImeForCurrentFocus = true,
        )

        assertEquals(true, result.clearFocus)
        assertEquals(false, result.hasSeenVisibleImeForCurrentFocus)
    }

    @Test
    fun `reduceSearchFieldImeState resets tracking when field loses focus`() {
        val result = reduceSearchFieldImeState(
            isFocused = false,
            isImeVisible = false,
            hasSeenVisibleImeForCurrentFocus = true,
        )

        assertEquals(false, result.clearFocus)
        assertEquals(false, result.hasSeenVisibleImeForCurrentFocus)
    }
}
