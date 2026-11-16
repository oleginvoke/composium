# Public API dependency regression fixture

This Android library deliberately depends only on `:runtime`, without its own
Compose dependencies or BOM. Compiling it checks that Composium exposes the types
needed to declare scenes, render previews, and host the catalog. The sample cannot
catch missing API dependencies because it already depends on Compose and Material3.

Run from the repository root:

```shell
./gradlew --init-script tests/api-consumer/include.gradle :api-consumer:compileDebugKotlin :api-consumer:compileReleaseKotlin :api-consumer:verifyRuntimeDependencyIsolation
```

The fixture is included only for this invocation and is not published.

The isolation task checks that the release runtime graph does not leak the Compose BOM,
Material3, preview tooling, or the KSP API. This fixture uses the repository toolchain;
it does not establish compatibility with older Kotlin/AGP versions.
