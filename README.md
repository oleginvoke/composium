# Composium

[![Maven Central](https://img.shields.io/maven-central/v/io.github.oleginvoke/composium?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.oleginvoke/composium)
[![KSP processor](https://img.shields.io/maven-central/v/io.github.oleginvoke/composium-processor?label=KSP%20processor)](https://central.sonatype.com/artifact/io.github.oleginvoke/composium-processor)

Storybook-like scene browser for native Android Compose projects.

Composium lets you describe UI states as regular Compose scenes, browse them inside the app, tweak parameters at runtime, and inspect components in isolation without building ad-hoc debug screens for every case.

Current release: `0.3.0-alpha01`

Artifacts:
- `io.github.oleginvoke:composium:0.3.0-alpha01`
- `io.github.oleginvoke:composium-processor:0.3.0-alpha01`

It is useful for:
- design systems and component libraries;
- interactive state catalogs for Compose components;
- QA and visual review of edge cases;
- quick local experiments when regular `@Preview` is not enough;
- keeping a living UI playground inside the app.

## Media
<img width="200" height="444" alt="image" src="https://github.com/user-attachments/assets/7ebded29-65be-4ff4-9156-a04a3bb0ef13" />
<img width="200" height="444" alt="image" src="https://github.com/user-attachments/assets/8d842f20-84f7-4550-a72b-95a22cc7a0b1" />
<img width="200" height="444" alt="image" src="https://github.com/user-attachments/assets/2a1af022-2b29-429a-a4b6-b7af79c2f7fb" />
<img width="200" height="444" alt="image" src="https://github.com/user-attachments/assets/71e419e8-1f95-44f4-8507-d3ef3e88a310" />
<img width="200" height="444" alt="demo" src="![demo](https://github.com/user-attachments/assets/3c019253-e681-4151-829d-24e444f09373)
" />



<!-- TODO: add overview screenshot -->
<!-- TODO: add grouped scenes screenshot -->
<!-- TODO: add scene controls screenshot -->
<!-- TODO: add theme + system params video / gif -->

## Why Composium

Composium is intentionally flexible:
- scenes are just Compose code;
- KSP is recommended, but not required;
- scenes can be flat or deeply grouped;
- grouping depth is unlimited;
- you can use the built-in theme toggle or fully own theme state yourself;
- you can wrap every scene preview with your own decorator;
- you can describe parameters with automatic inference where possible and explicit options where needed.

The library is meant to help you explore UI, not constrain how you structure it.

## Feature Overview

- Storybook-style scene browser for Android Compose
- Searchable scene list
- Flat scenes and unlimited nested groups via `group = "A/B/C"`
- Automatic scene discovery with KSP
- Optional manual registration without KSP
- Runtime controls for scene parameters
- Automatic controls for `Boolean`, `String`, `enum`, and sealed object hierarchies
- Nullable parameter support with explicit null-state toggle
- Custom labels for selectable options
- Custom ordering for auto-inferred sealed options
- Scene preview decorator for wrapping content
- Built-in dark theme toggle
- External theme ownership when you need full control
- Preview system controls for dark theme, display size, font size, and RTL

## Requirements

- Android `minSdk 24`
- JVM target `11`
- Kotlin `2.0+`
- `mavenCentral()` and `google()` in your consumer project

If you use KSP, use a KSP plugin version that matches your Kotlin version.

## Installation

Add repositories:

```kotlin
repositories {
    google()
    mavenCentral()
}
```

### Recommended: with KSP

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("io.github.oleginvoke:composium:0.3.0-alpha01")
    ksp("io.github.oleginvoke:composium-processor:0.3.0-alpha01")
}
```

Use this mode when you want automatic scene collection.

### Optional: without KSP

```kotlin
dependencies {
    implementation("io.github.oleginvoke:composium:0.3.0-alpha01")
}
```

Use this mode when you do not want to add KSP to the consumer project. In this case:
- do not add the processor;
- do not use `@ComposiumScene` or `@ComposiumSceneCatalog`;
- register scenes manually through `Composium.registerAll(...)`.

## Quick Start With KSP

KSP is the primary integration path. You annotate either individual scene properties, or scene catalogs.

```kotlin
import androidx.compose.runtime.Composable
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.scene

enum class ButtonVariant {
    Primary,
    Secondary,
    Danger,
}

sealed interface ButtonSize {
    object Small : ButtonSize
    object Medium : ButtonSize
    object Large : ButtonSize
}

@ComposiumScene
val primaryButton by scene(
    group = "Buttons/Primary",
    name = "Filled",
) {
    val enabled: Boolean by param(true)
    val text: String by param("Continue")
    val variant: ButtonVariant by param(ButtonVariant.Primary)
    val size: ButtonSize by param(ButtonSize.Medium)

    AppButton(
        text = text,
        enabled = enabled,
        variant = variant,
        size = size,
    )
}

@ComposiumSceneCatalog
object FormScenes {
    val loginDefault by scene(group = "Forms/Auth", name = "Login / default") {
        LoginForm()
    }

    val loginLoading by scene(group = "Forms/Auth", name = "Login / loading") {
        LoginForm(isLoading = true)
    }
}

@Composable
fun DebugCatalog() {
    ComposiumScreen()
}
```

What KSP collects:
- every property annotated with `@ComposiumScene`;
- every non-private `Scene` property declared inside an object annotated with `@ComposiumSceneCatalog`.

## Quick Start Without KSP

Manual mode uses the same `scene {}` API, but skips both KSP and annotations.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import oleginvoke.com.composium.Composium
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.scene

val primaryButton by scene(group = "Buttons/Primary", name = "Filled") {
    AppButton(text = "Continue")
}

object FormScenes {
    val loginDefault by scene(group = "Forms/Auth", name = "Login / default") {
        LoginForm()
    }
}

@Composable
fun DebugCatalog() {
    LaunchedEffect(Unit) {
        Composium.registerAll(
            primaryButton,
            FormScenes.loginDefault,
        )
    }

    ComposiumScreen()
}
```

Notes:
- manual registration is deduplicated by scene id (`group + name`);
- scenes still use the same runtime API as KSP mode;
- annotations are not needed in manual mode.

## Organizing Scenes

Composium does not force a single scene structure.

### Flat scenes

Scenes without a `group` stay at the root level:

```kotlin
val typographyTokens by scene {
    TypographyShowcase()
}
```

### Grouped scenes

Use slash-separated paths to build nested groups:

```kotlin
val primaryFilled by scene(group = "Buttons/Primary/Filled") {
    PrimaryFilledButton()
}

val primaryOutlined by scene(group = "Buttons/Primary/Outlined") {
    PrimaryOutlinedButton()
}

val dangerFilled by scene(group = "Buttons/Danger/Filled") {
    DangerButton()
}
```

Group nesting is unlimited. The UI tree is derived from the `group` path, not from where the property is declared in code.

That means you can:
- keep scenes as a flat list in source code;
- group them with `group = "..."`;
- or combine that with `@ComposiumSceneCatalog` objects for code organization.

## Parameters And Controls

Scene parameters are declared inside the scene body with delegated properties:

```kotlin
val buttonPlayground by scene(group = "Buttons") {
    val enabled: Boolean by param(true)
    val title: String by param("Continue")

    AppButton(
        text = title,
        enabled = enabled,
    )
}
```

### How Composium renders parameter controls

| Parameter kind | UI control | Notes |
| --- | --- | --- |
| `Boolean` | Switch | Automatic |
| `String` | Text field | Automatic |
| `enum` | Option chips | Values inferred automatically |
| Sealed object hierarchy | Option chips | Values inferred automatically from object instances |
| Nullable supported parameter | Checkbox + underlying control | Checkbox toggles null-state |
| Any other type with explicit options | Option chips | Use `optionList(...)` or `optionListLabeled(...)` |

Important detail: you can use any type as a parameter value, but interactive selection for custom and numeric types requires explicit options unless Composium can infer them automatically.

### Automatic options for `Boolean`, `enum`, and sealed object hierarchies

```kotlin
enum class ButtonVariant {
    Primary,
    Secondary,
    Danger,
}

sealed interface ButtonSize {
    object Small : ButtonSize
    object Medium : ButtonSize
    object Large : ButtonSize
}

val buttonPlayground by scene(group = "Buttons") {
    val enabled: Boolean by param(true)
    val variant: ButtonVariant by param(ButtonVariant.Primary)
    val size: ButtonSize by param(ButtonSize.Medium)

    AppButton(
        enabled = enabled,
        variant = variant,
        size = size,
    )
}
```

What happens in the UI:
- `enabled` is shown as a switch;
- `variant` is shown as chips with enum entries;
- `size` is shown as chips with inferred sealed object options.

### Explicit options for numbers and custom values

For types like `Int`, `Long`, `Float`, `Double`, or custom objects, define the allowed values explicitly:

```kotlin
import oleginvoke.com.composium.optionList
import oleginvoke.com.composium.optionListLabeled

val spacingPlayground by scene(group = "Spacing") {
    val elevation: Int by param(
        default = 0,
        options = optionListLabeled(
            0 to "None",
            2 to "2dp",
            8 to "8dp",
            16 to "16dp",
        ),
    )

    val alpha: Float by param(
        default = 1f,
        options = optionList(0.25f, 0.5f, 0.75f, 1f),
    )

    ExampleCard(
        elevation = elevation,
        alpha = alpha,
    )
}
```

### Custom labels

You can override how options are shown in the controls UI.

Using a label mapper:

```kotlin
val role by param(
    default = UserRole.Member,
    options = optionList(
        values = listOf(UserRole.Admin, UserRole.Member, UserRole.Guest),
    ) { role ->
        when (role) {
            UserRole.Admin -> "Administrator"
            UserRole.Member -> "Member"
            UserRole.Guest -> "Guest"
        }
    },
)
```

Using explicit value-label pairs:

```kotlin
val alignment by param(
    default = AlignmentMode.Center,
    options = optionListLabeled(
        AlignmentMode.Start to "Start",
        AlignmentMode.Center to "Center",
        AlignmentMode.End to "End",
    ),
)
```

### Nullable parameters

Nullable parameters get an extra checkbox that controls whether the value is currently `null`.

```kotlin
val cardPlayground by scene(group = "Cards") {
    val maxLines: Int? by param(
        default = null,
        options = optionListLabeled(
            1 to "1 line",
            2 to "2 lines",
            3 to "3 lines",
        ),
    )

    val subtitle: String? by param(null)

    ExampleCard(
        maxLines = maxLines,
        subtitle = subtitle,
    )
}
```

What happens in the UI:
- nullable parameters get a checkbox in the control header;
- unchecked means the parameter is currently `null`;
- checked restores the value and shows its regular control.

### Reordering auto-inferred options

For automatically inferred sealed options, you can still override their display order:

```kotlin
val size: ButtonSize by param(ButtonSize.Medium) { inferred ->
    inferred.reversed()
}
```

## Decorating The Scene Preview

Every scene can be rendered inside your own wrapper through `scenePreviewDecorator`.

Use this when you want:
- a custom background;
- padding around the component;
- a centered preview surface;
- device-frame-like wrappers;
- app-specific chrome around every scene.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumScreen

@Composable
fun DebugCatalog() {
    ComposiumScreen(
        scenePreviewDecorator = { scenePreview ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                scenePreview()
            }
        },
    )
}
```

The decorator receives the scene content as a composable lambda. Call it exactly once.

## Theme Control

Composium supports two theme ownership models.

### 1. Built-in internal theme state

If you just call `ComposiumScreen()`, Composium manages its own dark theme state and exposes a built-in toggle in the UI.

```kotlin
@Composable
fun DebugCatalog() {
    ComposiumScreen()
}
```

### 2. External theme ownership

If your app already owns theme state, pass it in and keep Composium synchronized with your theme system:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import oleginvoke.com.composium.ComposiumScreen

@Composable
fun DebugCatalog() {
    var isDarkTheme by remember { mutableStateOf(false) }

    AppTheme(darkTheme = isDarkTheme) {
        ComposiumScreen(
            isDarkTheme = isDarkTheme,
            onThemeChange = { isDarkTheme = it },
        )
    }
}
```

This mode is useful when:
- your app already has a single source of truth for theme;
- edge-to-edge setup depends on dark mode;
- you want Composium to follow the same theme logic as the rest of the app.

## Preview System Controls

Inside a scene, Composium also provides runtime preview controls for environment simulation:
- dark theme;
- display size;
- font size;
- RTL layout.

These controls are useful for quickly checking how a component behaves under different system conditions without leaving the scene browser.

Examples of what you can validate:
- typography overflow with larger font size;
- density-sensitive layouts with different display size multipliers;
- dark theme colors;
- mirrored layout issues in RTL.

## Typical Usage Patterns

### Design system catalog

Keep a single app-internal catalog with button, text field, card, sheet, and token scenes.

### Product component playground

Put scene definitions close to feature components and expose real states like loading, error, empty, success, and disabled.

### QA handoff

Give QA a stable in-app surface where they can switch component states without hidden debug menus.

### Local experimentation

Use scenes as a fast sandbox for composing UI states that would be awkward to wire into production navigation.

## Summary

Composium is a runtime scene browser for Compose that aims to stay out of your way:
- use KSP when you want automatic discovery;
- skip KSP when you want manual registration;
- keep scenes flat or organize them into deep nested groups;
- use automatic controls where possible and explicit options where needed;
- let Composium own theme state or plug it into your own;
- wrap scene rendering with your own decorator;
- inspect components under different preview system settings.

<!-- TODO: add final showcase screenshot -->
<!-- TODO: add end-to-end demo video / gif -->
