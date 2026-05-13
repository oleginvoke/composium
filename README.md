# Composium

[![Maven Central](https://img.shields.io/maven-central/v/io.github.oleginvoke/composium?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.oleginvoke/composium)
[![KSP processor](https://img.shields.io/maven-central/v/io.github.oleginvoke/composium-processor?label=KSP%20processor)](https://central.sonatype.com/artifact/io.github.oleginvoke/composium-processor)

Storybook-like scene browser for native Android Compose projects.

Composium lets you describe UI states as regular Compose scenes, browse them inside the app, tweak parameters at runtime, and inspect components in isolation without building ad-hoc debug screens for every case.

One of the core ideas of the library is that it gives you a ready-to-embed `ComposiumScreen()` composable. You can place this screen anywhere in your app: in a debug-only route, a separate activity, an internal tools section, or any custom navigation graph. `ComposiumScreen()` will render the scenes that you described in your project and turn them into a browsable interactive catalog.

Current release: `1.0.0`

Artifacts:
- `io.github.oleginvoke:composium:1.0.0`
- `io.github.oleginvoke:composium-processor:1.0.0`

It is useful for:
- design systems and component libraries;
- interactive state catalogs for Compose components;
- QA and visual review of edge cases;
- quick local experiments when regular `@Preview` is not enough;
- keeping a living UI playground inside the app.

<p align="center">
    <img src="https://github.com/user-attachments/assets/b91587da-5b54-4462-b1f1-6f24607f833c">
</p>
<p align="center">
    <img src="https://github.com/user-attachments/assets/c7c8295f-a8d8-4bcc-ba7e-188c85e2b9b5">
</p>
<p align="center">
    <img src="https://github.com/user-attachments/assets/4c23b4c6-3681-4cdc-bc6d-ba558c202b20">
</p>
<p align="center">
    <img src="https://github.com/user-attachments/assets/f22d482a-9893-4883-aed8-4dc401d02b35">
</p>

## Why Composium

Composium is intentionally flexible:
- you can place `ComposiumScreen()` anywhere in your app;
- supports any parameter type, including nullable types;
- scenes are just Compose code;
- KSP is recommended, but not required;
- scenes can be flat or deeply grouped;
- grouping depth is unlimited;
- you can use the built-in theme toggle or fully own theme state yourself;
- you can create project-local scene helpers for shared preview chrome;
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
- Custom names for selectable options
- Custom ordering for auto-inferred sealed options
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
    implementation("io.github.oleginvoke:composium:1.0.0")
    ksp("io.github.oleginvoke:composium-processor:1.0.0")
}
```

Use this mode when you want automatic scene collection.

### Optional: without KSP

```kotlin
dependencies {
    implementation("io.github.oleginvoke:composium:1.0.0")
}
```

Use this mode when you do not want to add KSP to the consumer project. In this case:
- do not add the processor;
- do not use `@ComposiumScene` or `@ComposiumSceneCatalog`;
- register scenes manually through `Composium.registerAll(...)`.

## Quick Start With KSP

KSP is the primary integration path.

There are two discovery styles:
- annotate individual scene properties with `@ComposiumScene`;
- or annotate an object with `@ComposiumSceneCatalog`.

You do not need to use both at the same time. Pick the style that matches how you want to organize scenes in your project.

### Option 1: annotate individual scene properties

Use this style when you want flat, explicit scene declarations and prefer to mark each scene directly.

```kotlin
import androidx.compose.runtime.Composable
import oleginvoke.com.composium.ComposiumScene
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
internal val primaryButton by scene(
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
@Composable
fun DebugCatalog() {
    ComposiumScreen()
}
```

### Option 2: annotate a scene catalog object

Use this style when you want to keep several related scenes together inside one object and let KSP collect all non-private `Scene` properties from it.

```kotlin
import androidx.compose.runtime.Composable
import oleginvoke.com.composium.ComposiumSceneCatalog
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.scene

@ComposiumSceneCatalog
internal object FormScenes {
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

These are two independent discovery styles, not a required pair of annotations on the same scene setup.

## Quick Start Without KSP

Manual mode uses the same `scene {}` API, but skips both KSP and annotations.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import oleginvoke.com.composium.Composium
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.scene

internal val primaryButton by scene(group = "Buttons/Primary", name = "Filled") {
    AppButton(text = "Continue")
}

internal object FormScenes {
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
internal val typographyTokens by scene {
    TypographyShowcase()
}
```

### Grouped scenes

Use slash-separated paths to build nested groups:

```kotlin
internal val primaryFilled by scene(group = "Buttons/Primary/Filled") {
    PrimaryFilledButton()
}

internal val primaryOutlined by scene(group = "Buttons/Primary/Outlined") {
    PrimaryOutlinedButton()
}

internal val dangerFilled by scene(group = "Buttons/Danger/Filled") {
    DangerButton()
}
```

Group nesting is unlimited. The UI tree is derived from the `group` path, not from where the property is declared in code.

That means you can:
- keep scenes as a flat list in source code;
- group them with `group = "..."`;
- or combine that with `@ComposiumSceneCatalog` objects for code organization.

### Edge-to-edge scenes

By default, Composium keeps scene content below its own top bar and above the system navigation bar:

```kotlin
internal val regularScene by scene(group = "Layout") {
    Content()
}
```

Use `enableEdgeToEdge = true` when a scene should own this spacing itself, for example for full-screen layouts, lists, maps, or custom surfaces that need to render behind Composium chrome.

```kotlin
internal val edgeToEdgeFeed by scene(
    group = "Layout",
    enableEdgeToEdge = true,
) {
    Feed(
        contentPadding = innerPadding,
    )
}
```

When `enableEdgeToEdge` is `true`, Composium lets the scene fill the preview area and exposes the top and bottom insets through `innerPadding`. Apply that padding where it matches the component layout. When it is `false`, Composium applies the spacing for you and `innerPadding` is zero.

## Parameters And Controls

Scene parameters are declared inside the scene body with delegated properties:

```kotlin
internal val buttonPlayground by scene(group = "Buttons") {
    val enabled: Boolean by param(true)
    var title: String by param("Continue")

    AppButton(
        text = title,
        enabled = enabled,
        onClick = {
            title = if (title == "Continue") "Saved" else "Continue"
        },
    )
}
```

`param(...)` delegates can be declared as `var`. This lets the scene update a parameter from its own code while Composium still exposes the same value in the settings panel.

### How Composium renders parameter controls

| Parameter kind | UI control | Notes |
| --- | --- | --- |
| `Boolean` | Switch | Automatic |
| `String` | Text field | Automatic |
| `enum` | Option chips | Values inferred automatically |
| Sealed object hierarchy | Option chips | Values inferred automatically from object instances |
| Nullable supported parameter | Checkbox + underlying control | Checkbox toggles null-state; do not add `null` to `options` manually |
| Any other type with explicit options | Option chips | Use `listOf(...).toParamOptions()` or explicit `named` values |

Important detail: you can use any type as a parameter value, but interactive selection for custom and numeric types requires explicit options unless Composium can infer them automatically.

For referential or non-static types such as `Painter`, prefer explicit named options.

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

internal val buttonPlayground by scene(group = "Buttons") {
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

For types like `Int`, `Long`, `Float`, `Double`, or custom objects, define the allowed values explicitly.

Inside `scene {}` you can convert raw values to `List<ParamOption<T>>` with `toParamOptions()`. If you want full control over labels, pass explicit `named` values instead.

If the parameter type is nullable, pass only the non-null choices. Composium handles the `null` state through the checkbox automatically.

```kotlin
internal val spacingPlayground by scene(group = "Spacing") {
    val elevation: Int by param(
        default = 0 named "None",
        options = listOf(
            0 named "None",
            2 named "2dp",
            8 named "8dp",
            16 named "16dp",
        ),
    )

    val alpha: Float by param(
        default = 1f,
        options = listOf(0.25f, 0.5f, 0.75f, 1f).toParamOptions(),
    )

    ExampleCard(
        elevation = elevation,
        alpha = alpha,
    )
}
```

### Custom names

You can override how options are shown in the controls UI.

`default` can also be passed as a named option with infix syntax:

```kotlin
val mode by param(
    default = DisplayMode.Grid named "Grid",
    options = listOf(
        DisplayMode.Grid named "Grid",
        DisplayMode.List named "List",
    ),
)
```

Using a name mapper:

```kotlin
val role by param(
    default = UserRole.Member,
    options = listOf(
        UserRole.Admin,
        UserRole.Member,
        UserRole.Guest,
    ).toParamOptions { role ->
        when (role) {
            UserRole.Admin -> "Administrator"
            UserRole.Member -> "Member"
            UserRole.Guest -> "Guest"
        }
    },
)
```

Using explicit named values:

```kotlin
val alignment by param(
    default = AlignmentMode.Center,
    options = listOf(
        AlignmentMode.Start named "Start",
        AlignmentMode.Center named "Center",
        AlignmentMode.End named "End",
    ),
)
```

Using a named default without explicit options:

```kotlin
val leadingIcon: Painter? by param(
    painterResource(R.drawable.solid_attention) named "Attention",
)
```

For referential values such as `Painter`, names act as the stable identity for explicit option chips. In these cases, name every option and name the default too when it is declared independently from the option list.

If explicit option names collide inside the same parameter, Composium will append numeric suffixes automatically until every name becomes unique.

### Nullable parameters

Nullable parameters get an extra checkbox that controls whether the value is currently `null`.

```kotlin
internal val cardPlayground by scene(group = "Cards") {
    val maxLines: Int? by param(
        default = null,
        options = listOf(
            1 named "1 line",
            2 named "2 lines",
            3 named "3 lines",
        ),
    )

    val leadingIcon: Painter? by param(
        painterResource(R.drawable.solid_attention) named "Attention",
    )

    val subtitle: String? by param(null)

    ExampleCard(
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        subtitle = subtitle,
    )
}
```

What happens in the UI:
- nullable parameters get a checkbox in the control header;
- unchecked means the parameter is currently `null`;
- checked restores the value and shows its regular control.

For nullable parameters with explicit options, do not include `null` in the list yourself. Pass only non-null values and let Composium manage the null-state toggle.

If you declare a nullable parameter with `param(options = ...)` and do not provide an explicit `default`, Composium uses the first option from the list as the initial value (including `null` if it is first).

### Reordering auto-inferred options

For automatically inferred sealed options, you can still override their display order:

By default, auto-inferred sealed options are sorted by their generated names using natural ordering (for example, `r2`, `r10`, `r100`).

```kotlin
val size: ButtonSize by param(ButtonSize.Medium) { inferred ->
    inferred.reversed()
}
```

## Custom Scene Wrappers

If several scenes need the same preview chrome, create a small project-local helper around `scene(...)`.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneDelegate
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.scene

fun sceneWithFrame(
    group: String? = null,
    name: String? = null,
    content: @Composable SceneScope.() -> Unit,
): SceneDelegate = scene(
    group = group,
    name = name,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

Use that helper for scenes that should share the same frame:

```kotlin
val PrimaryButtonScene by sceneWithFrame(group = "Buttons") {
    PrimaryButton(text = "Save", onClick = {})
}
```

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

## Contributing

If you want to fix a bug, improve the API, or extend the library with new functionality, contributions are welcome.

## Roadmap

- [ ] dedicated tokens section for things like typography and colors;
- [ ] color picker.

## Summary

Composium is a runtime scene browser for Compose that aims to stay out of your way:
- use KSP when you want automatic discovery;
- skip KSP when you want manual registration;
- keep scenes flat or organize them into deep nested groups;
- use automatic controls where possible and explicit options where needed;
- let Composium own theme state or plug it into your own;
- create scene helper wrappers for shared preview chrome;
- inspect components under different preview system settings.
