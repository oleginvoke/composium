# Scene Content Padding and Floating Tools Design

## Status

Approved in conversation on 2026-09-05 and amended with draggable floating tools on 2026-09-05.

## Goals

- Replace the conditional `SceneScope.innerPadding` contract with an explicit `contentPadding` argument.
- Remove `enableEdgeToEdge`; edge-to-edge rendering should be determined by where scene content applies the supplied padding.
- Preserve the existing top-bar experience as the default.
- Add a per-scene floating-tools presentation for full-screen scenes.
- Report an error when a scene ignores `contentPadding` without requiring users to install a separate lint dependency.

## Non-goals

- Persisting the minimized state across scene visits or process restarts.
- Including the floating tools bounds in `contentPadding`.
- A mode with no discoverable way to restore Composium tools.

## Public API

Add a tools presentation enum:

```kotlin
enum class SceneTools {
    TopBar,
    Floating,
}
```

`TopBar` remains the default. `Floating` replaces the regular scene top bar with the compact floating tools surface described below.

Change scene content to receive padding as a lambda argument:

```kotlin
fun scene(
    group: String? = null,
    name: String? = null,
    tools: SceneTools = SceneTools.TopBar,
    thumbnail: (@Composable SceneScope.() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
): SceneDelegate
```

`Scene`, `SceneDelegate`, wrappers around `scene`, and internal content invocation use the same content type. `enableEdgeToEdge` is removed from all public and internal APIs. `SceneScope.innerPadding` and its backing state are removed.

### Regular content

```kotlin
val ButtonScene by scene { contentPadding ->
    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Button(...)
    }
}
```

### Edge-to-edge content

The scene always receives the full preview bounds. A scene creates edge-to-edge areas by applying padding only to the children that must remain unobscured:

```kotlin
val MapScene by scene(
    tools = SceneTools.Floating,
) { contentPadding ->
    Box(Modifier.fillMaxSize()) {
        Map()
        MapActions(Modifier.padding(contentPadding))
    }
}
```

There is no separate edge-to-edge mode or boolean.

## `contentPadding` Semantics

Composium always measures and places scene content in the entire current preview pane. It never offsets the scene content itself to avoid Composium UI.

The supplied padding describes rectangular safe content insets:

| Tools presentation | Top padding | Bottom padding |
| --- | --- | --- |
| `SceneTools.TopBar` | Status-bar inset plus the regular Composium top-bar area | System navigation-bar inset while the inspector is closed |
| `SceneTools.Floating` | Status-bar inset only | System navigation-bar inset while the inspector is closed |

When the inspector occupies the bottom of the screen, its layout already bounds and clips the preview pane, so the scene does not receive an additional inspector-sized bottom padding. The existing closed/split/expanded sizing behavior remains otherwise unchanged.

The floating surface is deliberately excluded from `contentPadding`. It is an overlay and may cover scene content. This also avoids an impossible contract once draggable positioning is considered in the future.

If no host window insets are supplied, the corresponding system inset is zero. Padding is recomputed when host insets or tools presentation change.

## Preview and Thumbnail Behavior

`Scene.RenderPreview()` invokes scene content with `PaddingValues(0.dp)`. Android Studio Preview renders the scene itself and has no Composium top bar or floating tools.

Thumbnail capture invokes fallback scene content with zero padding. The custom `thumbnail` lambda does not gain a padding parameter because it renders without host chrome.

The fallback from `thumbnail` to regular scene content is implemented as an invocation wrapper rather than by treating both lambdas as the same function type.

## Floating Tools UI

### Placement

- Starts at the physical top-right of the scene screen, below the status-bar inset.
- Dragging the central eye moves the complete surface freely across the scene screen, including over the split inspector.
- The complete 104 by 104 dp surface stays within all system insets and a 12 dp screen margin. Bounds remain based on the expanded surface while it is minimized, so restoring it cannot reveal clipped controls.
- Positioning stays physical and does not mirror in RTL layouts.
- Rendered outside the color-eyedropper capture host, like the current top bar, so Composium UI is not sampled as scene content.

### Expanded state

The tools appear as one compact rounded surface containing an evenly divided 2 by 2 grid:

```text
Back       Properties
Eyedropper Theme
```

Each action has at least a 48 by 48 dp touch target, with a compact 18-20 dp icon. The actions have no independent circular containers: an outer contour and a thin internal cross divide the surface into four rectangular sections. An active Properties or Eyedropper action fills its complete section. Theme uses a simplified light/dark icon toggle instead of the current wide switch.

A circular visibility control is centered over the grid intersection. Its `VisibilityOff` state hides the surrounding grid, while `Visibility` restores it. The grid fades and scales toward or away from this fixed center; the eye icon uses a matching compact fade-and-scale transition.

The Back action remains available in addition to Android system Back. Both use the existing scene back behavior.

### Minimized state

Activating the central eye hides the grid around it. Only the circular eye remains in exactly the same position, without an additional backing surface.

- The surface starts expanded each time a scene screen is entered.
- The minimized state is local to that scene-screen instance.
- It is not persisted when leaving and reopening the scene.
- The dragged position survives minimize/restore and inspector transitions, but resets on the next scene-screen entry.
- Window-size and orientation changes clamp the current position into the new safe bounds.
- There is no edge snapping, fling, or persisted position.

The eye is a 40 by 40 dp visible and semantic control with `Hide tools` and `Show tools` content descriptions for its two states. Its hit box does not overlap the surrounding action cells; platform minimum-touch-target expansion may still apply in otherwise empty space. A tap toggles visibility, while movement beyond touch slop starts dragging without also toggling the tools.

### Inspector and eyedropper interaction

- With the inspector closed, Properties opens the current split inspector.
- In split mode, the Properties action changes to the existing expand action and the floating surface remains over the preview pane.
- In expanded inspector mode, the floating surface is not shown; the existing expanded-inspector top bar handles navigation and closing.
- Returning from expanded inspector restores the floating surface in its previous expanded/minimized state.
- Eyedropper activation and system Back retain the current state-machine behavior.
- The floating surface is never included in eyedropper sampling or scene padding.

## Lint Rule

Add a custom lint issue named `UnusedComposiumContentPaddingParameter`, adapted from the existing `DesignContentPaddingDetector` in `F:\New-STG-design`.

The issue uses `Severity.ERROR`, matching the STG detector. The check reports an error when a `scene`, direct `Scene` construction, or scene-wrapper content lambda does not reference its implicit or named padding argument. Forwarding the argument to another function counts as usage.

An underscore is still considered unused and produces the error. Intentional full-bleed content must suppress the issue explicitly:

```kotlin
@Suppress("UnusedComposiumContentPaddingParameter")
val BackgroundScene by scene {
    FullScreenBackground()
}
```

The rule does not attempt to prove that padding was applied correctly; it only enforces an explicit reference or an explicit suppression.

Reuse the STG detector's UAST structure for locating the `content` lambda, resolving the actual lambda parameter, handling implicit `it`, rejecting same-named unrelated properties, and respecting nested-lambda shadowing. Composium host detection should use the `SceneScope.(PaddingValues) -> Unit` content signature so project-local scene wrappers are covered in addition to the public `scene` factory and direct `Scene` construction.

Do not carry over the STG-specific exceptions for `TopSpacer`, `BottomSpacer`, `consumeParentScaffoldPadding`, nested `Scaffold` inheritance, or `ScaffoldSheet` hierarchy boundaries. Composium has no equivalent APIs, so only a real reference to the scene lambda's padding parameter counts as usage.

The detector and its `IssueRegistry` live in a dedicated lint module with detector tests. The runtime module uses `lintPublish` so the lint JAR is packaged into the published AAR and automatically runs for consumers without another dependency. This is the supported AAR delivery mechanism for library-provided checks.

## State and Internal Boundaries

- `SceneTools` is immutable scene metadata.
- Floating minimized state belongs to `SceneScreenState` and changes through explicit intents in the existing reducer.
- Floating position is transient scene-screen UI state. Placement and clamping are isolated as pure logic from the gesture host.
- Padding calculation is a pure function of tools presentation, window insets, and inspector layout mode so it can be unit tested independently.
- Floating tools rendering is a separate composable from the existing top bar; both consume the same callbacks and state-derived button models.
- Existing scene parameter state, inspector state, theme controller, and eyedropper state remain the sources of truth.

## Migration

This is a source-breaking public API change.

- Remove `enableEdgeToEdge = false` and apply `contentPadding` to the scene root or relevant child.
- Remove `enableEdgeToEdge = true` and apply `contentPadding` only where controls must avoid system or Composium UI.
- Replace `innerPadding` reads with the new lambda argument.
- Update project-local scene wrappers either to consume `PaddingValues` as part of their own layout or to expose and forward `SceneScope.(PaddingValues) -> Unit`.
- Existing scene lambdas that do not name their implicit argument may still compile, but lint fails until they reference the padding or explicitly suppress `UnusedComposiumContentPaddingParameter`.
- Direct `Scene` and `SceneDelegate` construction must migrate to the new `tools` metadata and content function type.

README examples and the sample application are migrated as part of the same change.

## Verification

- Unit tests for padding calculation in `TopBar` and `Floating` modes, with closed and split inspectors.
- Reducer tests for minimizing/restoring floating tools and preserving that state across inspector transitions.
- UI tests for the four equal sections, central eye, non-overlapping action hit areas, drag movement, safe-bound clamping, hidden state, accessibility descriptions, and expanded-inspector visibility.
- Lint detector tests covering named use, implicit use, forwarded use, accidental named and implicit omission, `_` omission, explicit suppression, unrelated same-named properties, nested-lambda shadowing, same-named functions with the wrong signature, direct `Scene` construction, and project-local scene wrappers.
- Detector tests proving that calls named `TopSpacer`, `BottomSpacer`, or `consumeParentScaffoldPadding` do not count as usage in Composium.
- Compilation coverage for `scene`, direct `Scene`, wrappers, `RenderPreview`, thumbnails, and KSP-generated registration.
- Runtime and processor test suites, sample compilation, and Android lint.
