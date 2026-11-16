# Dependency compatibility

## Supported baseline

The published runtime supports Android API 21+, compileSdk 34+, Kotlin 2.0.20+,
and JVM bytecode target 11. The older consumer fixture uses AGP 8.5.2 and Gradle
8.7; this is the oldest build-tool combination covered by the automated checks.
The consumer owns its targetSdk. Composium does not require a particular targetSdk.

The published dependency baseline is deliberately independent of the compiler
and libraries used to build this checkout and its sample:

| Published dependency | Minimum version |
| --- | --- |
| Compose runtime, UI, foundation and animation | 1.7.8 |
| Material Icons | 1.7.8 |
| Activity | 1.9.3 |
| Lifecycle runtime Compose | 2.8.7 |
| Core KTX | 1.13.1 |
| Kotlin stdlib and reflect | 2.0.20 |
| Processor's compile-only KSP API | 2.0.20-1.0.25 |

Versions are ordinary minimum dependencies, with no `force`, `strictly`, or
exported BOM. New applications can resolve newer versions through their own BOM
and dependencies. Public Compose types and Kotlin stdlib remain `api`; internal
libraries remain `implementation`. Implementation dependencies are still
transitive runtime dependencies. The processor does not publish its KSP API.
Preview tooling is debug-only; neither tooling nor Material3 nor the Compose BOM
is published in the release runtime graph.

Parameter chips use an internal layout built on stable Compose APIs. The
experimental FlowRow overload from Compose 1.7 was removed in Compose 1.12;
calling it from an older compiled AAR causes NoSuchMethodError even when both
projects compile successfully. Tests compare chip wrapping/spacing in LTR and
RTL and exercise the controls against both runtime versions.

Explicit stdlib/reflect dependencies prevent the repository's Kotlin compiler
from silently raising the published Kotlin floor. Language/API level 2.0 alone
would not achieve this. The processor also compiles against the older KSP API so
it does not require methods added by a newer plugin. Consumers select their KSP
plugin, independently of the processor's compile-only API version.

Android display zoom is available from API 24. On API 21-23, the current display
density is the default reference. This avoids accessing `DENSITY_DEVICE_STABLE`
on an Android version which does not provide it.

## Build tools and current applications

Building this checkout uses AGP 9.1.1, Gradle 9.5.1, Kotlin 2.3.21, and KSP 2.3.9.
These are repository build tools, not requirements imposed on a consuming app.
The runtime is compiled with Kotlin language/API level 2.0 and checked by a
separately built Kotlin 2.0.20 consumer.
Preserving this output level takes priority over upgrading the repository's
compiler merely to match the newest consumer compiler.

The compatibility profiles pin the following consumer configurations:

| Profile | AGP / Gradle | Kotlin / KSP2 | compileSdk / targetSdk | Compose |
| --- | --- | --- | --- | --- |
| legacy | 8.5.2 / 8.7 | 2.0.20 / 2.0.20-1.0.25 | 34 / 34 | 1.7.8 |
| legacy-manual | 8.5.2 / 8.7 | 2.0.20 / no KSP | 34 / 34 | 1.7.8 |
| modern-baseline | 9.5.1 / 9.9.0 | 2.4.21 / 2.3.12 | 37 / 37 | 1.7.8 |
| modern | 9.5.1 / 9.9.0 | 2.4.21 / 2.3.12 | 37 / 37 | 1.12.1 |

The modern profile supplies BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0,
and Core 1.19.1. The modern-baseline profile checks that upgrading SDK/build tools
does not require upgrading Compose. The modern profile uses minSdk 23 to meet
its own newer AndroidX requirements; Composium's baseline remains API 21.

KSP 2.3.13 was intentionally not selected: its authors document a concurrent
execution regression in the [release notes](https://github.com/google/ksp/releases/tag/2.3.13).
Use a compatible KSP release rather than assuming the newest plugin supports
every older AGP/Kotlin pair.

These profiles establish tested combinations, not a guarantee for every
historical or future toolchain. Kotlin 1.x and compileSdk below 34 are outside
the supported baseline.

## Reproducing the checks

Run from the repository root with Python 3, JDK 17, and an installed Android SDK:

```shell
python tests/compatibility-consumer/run.py
python tests/compatibility-consumer/run.py --connected --lint-probe
```

The runner stages the locally built release AAR, processor JAR, POM, and Gradle
module metadata into `.gradle/compatibility/repository`. Each profile then builds
with its own Gradle wrapper and Maven dependencies, without `project(":runtime")`
or `mavenLocal()`. Nothing is uploaded to Maven Central. Logs, resolved graphs,
APKs, test reports, and results are kept under `.gradle/compatibility`.

Checks include debug and R8-minified release builds, strict consumer lint,
KSP-generated scene registration, and exact dependency graph assertions. Older
profiles also run scene/reflection/Environment UI tests on Android API 21 and 23
with Robolectric 4.14.1. `--connected` runs UI smoke tests on the connected
emulator/device; it requires a device compatible with the selected profiles.
The instrumentation driver uses current AndroidX Test/Espresso independently
of the app's dependency baseline, so it can inject input on current Android.
`--lint-probe` requires the embedded lint check to reject a deliberately unused
scene padding parameter and then verifies a clean lint run after removing it.
Only dependency-update and catalog-style lint suggestions are disabled in this
fixture because pinning separate old/new versions is intentional; API and
custom-check loading errors remain enabled.

Repository runtime tests use the sample's Compose BOM. They include density and
padding regressions on API 23, 24, and 35. The independent consumers test the
published older baseline and the newer dependency stack. Also run:

```shell
./gradlew :runtime:testDebugUnitTest :runtime:lintRelease :lint:test :sample:assembleDebug
./gradlew --init-script tests/api-consumer/include.gradle :api-consumer:compileDebugKotlin :api-consumer:compileReleaseKotlin :api-consumer:verifyRuntimeDependencyIsolation
```

## Exact Environment scales

Exact input continues to accept every finite positive Float. Scene density and
padding are computed using Double intermediates; only derived values outside
Float's representable range are saturated. The exact selected value is retained.
Zero padding edges remain zero, including at the smallest accepted scale. This
prevents infinity multiplication from producing NaN and crashing the preview.
