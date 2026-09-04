# Scene Content Padding and Floating Tools Design

## Status

Approved in conversation on 2026-09-05. This document defines the intended public API and runtime behavior before implementation.

## Goals

- Replace the conditional `SceneScope.innerPadding` contract with an explicit `contentPadding` argument.
- Remove `enableEdgeToEdge`; edge-to-edge rendering should be determined by where scene content applies the supplied padding.
- Preserve the existing top-bar experience as the default.
- Add a per-scene floating-tools presentation for full-screen scenes.
- Warn when a scene accidentally ignores `contentPadding` without requiring users to install a separate lint dependency.

## Non-goals

- Draggable floating tools.
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

- Fixed at the physical top-right of the scene screen.
- Positioned below the status-bar inset.
- Uses approximately 12 dp top and right margins.
- Rendered outside the color-eyedropper capture host, like the current top bar, so Composium UI is not sampled as scene content.

### Expanded state

The tools appear as one compact surface containing a 2 by 2 grid:

```text
Back       Properties
Eyedropper Theme
```

Each action has at least a 48 by 48 dp touch target, with a compact 18-20 dp icon. The surface should read as one component rather than four unrelated floating buttons. Active properties and eyedropper states reuse the current top-bar visual feedback. Theme uses a simplified light/dark icon toggle instead of the current wide switch.

The Back action remains available in addition to Android system Back. Both use the existing scene back behavior.

### Minimized state

A small chevron tab is visually attached above the expanded surface. Activating it minimizes the grid. Only the small tab remains, at the same top-right anchor, with its chevron reversed to indicate restoration.

- The surface starts expanded each time a scene screen is entered.
- The minimized state is local to that scene-screen instance.
- It is not persisted when leaving and reopening the scene.
- No drag gesture is implemented in this version.

The tab has an accessible touch target even if its visible capsule is smaller, plus `Minimize tools` and `Show tools` content descriptions for its two states.

### Inspector and eyedropper interaction

- With the inspector closed, Properties opens the current split inspector.
- In split mode, the Properties action changes to the existing expand action and the floating surface remains over the preview pane.
- In expanded inspector mode, the floating surface is not shown; the existing expanded-inspector top bar handles navigation and closing.
- Returning from expanded inspector restores the floating surface in its previous expanded/minimized state.
- Eyedropper activation and system Back retain the current state-machine behavior.
- The floating surface is never included in eyedropper sampling or scene padding.

## Lint Rule

Add a custom lint issue named `UnusedComposiumContentPadding`, modeled after the Material `Scaffold` padding check.

The check reports a warning when a `scene` content lambda receives an implicit or named padding argument but does not reference it. Forwarding the argument to another function counts as usage.

Intentional edge-to-edge code opts out explicitly with an underscore:

```kotlin
val BackgroundScene by scene { _ ->
    FullScreenBackground()
}
```

Using `_` documents intent and does not produce the warning. The rule does not attempt to prove that padding was applied correctly; it only catches accidental omission.

The detector and its `IssueRegistry` live in a dedicated lint module with detector tests. The runtime module uses `lintPublish` so the lint JAR is packaged into the published AAR and automatically runs for consumers without another dependency. This is the supported AAR delivery mechanism for library-provided checks.

## State and Internal Boundaries

- `SceneTools` is immutable scene metadata.
- Floating minimized state belongs to `SceneScreenState` and changes through explicit intents in the existing reducer.
- Padding calculation is a pure function of tools presentation, window insets, and inspector layout mode so it can be unit tested independently.
- Floating tools rendering is a separate composable from the existing top bar; both consume the same callbacks and state-derived button models.
- Existing scene parameter state, inspector state, theme controller, and eyedropper state remain the sources of truth.

## Migration

This is a source-breaking public API change.

- Remove `enableEdgeToEdge = false` and apply `contentPadding` to the scene root or relevant child.
- Remove `enableEdgeToEdge = true` and apply `contentPadding` only where controls must avoid system or Composium UI.
- Replace `innerPadding` reads with the new lambda argument.
- Update project-local scene wrappers to accept and forward `PaddingValues`.
- Existing scene lambdas that do not name their implicit argument may still compile, but lint warns until they apply the padding or declare `_ ->` explicitly.
- Direct `Scene` and `SceneDelegate` construction must migrate to the new `tools` metadata and content function type.

README examples and the sample application are migrated as part of the same change.

## Verification

- Unit tests for padding calculation in `TopBar` and `Floating` modes, with closed and split inspectors.
- Reducer tests for minimizing/restoring floating tools and preserving that state across inspector transitions.
- UI tests for the four actions, minimized handle, accessibility descriptions, and expanded-inspector visibility.
- Lint detector tests covering named use, forwarded use, accidental omission, implicit omission, and `_` opt-out.
- Compilation coverage for `scene`, direct `Scene`, wrappers, `RenderPreview`, thumbnails, and KSP-generated registration.
- Runtime and processor test suites, sample compilation, and Android lint.
